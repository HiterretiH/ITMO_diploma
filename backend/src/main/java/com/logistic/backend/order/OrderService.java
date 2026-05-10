package com.logistic.backend.order;

import com.logistic.backend.api.dto.DocumentDownload;
import com.logistic.backend.api.dto.OrderCreateRequest;
import com.logistic.backend.api.dto.OrderDocumentDescriptor;
import com.logistic.backend.api.dto.OrderResponse;
import com.logistic.backend.api.dto.OrderUpdateRequest;
import com.logistic.backend.audit.AuditEventRepository;
import com.logistic.backend.audit.AuditEventType;
import com.logistic.backend.audit.AuditService;
import com.logistic.backend.catalog.Customer;
import com.logistic.backend.catalog.CustomerRepository;
import com.logistic.backend.catalog.Driver;
import com.logistic.backend.catalog.DriverRepository;
import com.logistic.backend.catalog.Performer;
import com.logistic.backend.catalog.PerformerRepository;
import com.logistic.backend.catalog.Vehicle;
import com.logistic.backend.catalog.VehicleRepository;
import com.logistic.backend.document.DocumentGenerationService;
import com.logistic.backend.document.DocumentTemplateVersion;
import com.logistic.backend.document.DocumentType;
import com.logistic.backend.document.FileFormat;
import com.logistic.backend.document.GeneratedDocumentCache;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserAccess;
import com.logistic.backend.user.UserRepository;
import com.logistic.backend.user.UserTripDefaults;
import com.logistic.backend.user.UserTripDefaultsRepository;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PerformerRepository performerRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;
    private final DocumentGenerationService documentGenerationService;
    private final GeneratedDocumentCache generatedDocumentCache;
    private final UserTripDefaultsRepository userTripDefaultsRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse create(OrderCreateRequest req, User actor) {
        Customer customer = customerRepository.findById(req.customerId()).orElseThrow(this::notFound);
        Performer performer = performerRepository.findById(req.performerId()).orElseThrow(this::notFound);
        validateCustomerPerformerAccess(actor, customer, performer);
        User orderOwner = customer.getOwner();
        Order o = new Order();
        o.setOwner(orderOwner);
        o.setCustomer(customer);
        o.setPerformer(performer);
        o.setOrderNumber(orderRepository.maxOrderNumberForOwner(orderOwner.getId()) + 1);
        o.setOrderDate(LocalDate.now());
        o.setLoadingPlace("");
        o.setUnloadingPlace("");
        if (req.vehicleId() != null) {
            Vehicle v = vehicleRepository.findById(req.vehicleId()).orElseThrow(this::notFound);
            assertPerformerOwnsVehicle(performer, v);
            assertVehicleVisible(actor, v);
            o.setVehicle(v);
        }
        if (req.driverId() != null) {
            Driver d = driverRepository.findById(req.driverId()).orElseThrow(this::notFound);
            assertPerformerEmploysDriver(performer, d);
            assertDriverVisible(actor, d);
            o.setDriver(d);
        }
        orderRepository.save(o);
        upsertUserTripDefaults(actor, o);
        auditService.record(
                actor, o, AuditEventType.ORDER_CREATED, Map.of("orderId", o.getId().toString()));
        return toDto(o);
    }

    @Transactional
    public OrderResponse update(Long id, OrderUpdateRequest req, User actor) {
        Order o = loadDetailed(actor, id);
        apply(o, req, actor);
        syncOrderOwner(o);
        orderRepository.save(o);
        upsertUserTripDefaults(actor, o);
        auditService.record(
                actor, o, AuditEventType.ORDER_UPDATED, Map.of("orderId", o.getId().toString()));
        Long ownerId = o.getOwner().getId();
        Long oid = o.getId();
        generatedDocumentCache.invalidate(ownerId, oid);
        return toDto(o);
    }

    @Transactional
    public OrderResponse complete(Long id, User actor) {
        Order o = loadDetailed(actor, id);
        validateReadyForComplete(o);
        generatedDocumentCache.invalidate(o.getOwner().getId(), o.getId());
        o.setTemplateVersion(DocumentTemplateVersion.CURRENT);
        orderRepository.save(o);
        auditService.record(actor, o, AuditEventType.ORDER_COMPLETED, Map.of("orderId", id.toString()));
        return toDto(o);
    }

    @Transactional
    public void delete(Long id, User actor) {
        Order o = loadDetailed(actor, id);
        Long orderId = o.getId();
        generatedDocumentCache.invalidate(o.getOwner().getId(), orderId);
        orderRepository.delete(o);
        auditService.record(actor, AuditEventType.ORDER_DELETED, Map.of("orderId", orderId.toString()));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id, User actor) {
        return toDto(loadDetailed(actor, id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(User actor) {
        List<Order> orders =
                UserAccess.isAdmin(actor)
                        ? orderRepository.findAllDetailedOrderByOrderDateDesc()
                        : orderRepository.findAllDetailedByOwner_IdOrderByOrderDateDesc(actor.getId());
        List<Long> ids = orders.stream().map(Order::getId).toList();
        Set<Long> completedIds =
                ids.isEmpty()
                        ? Set.of()
                        : auditEventRepository.findOrderIdsByOrder_IdInAndEventType(
                                ids, AuditEventType.ORDER_COMPLETED);
        return orders.stream().map(o -> toDto(o, completedIds.contains(o.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDocumentDescriptor> listDocuments(Long orderId, User actor) {
        loadDetailed(actor, orderId);
        return Arrays.stream(DocumentType.values())
                .map(
                        dt ->
                                new OrderDocumentDescriptor(
                                        dt, List.of(FileFormat.DOCX, FileFormat.PDF), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadOrderDocument(
            Long orderId, DocumentType documentType, FileFormat format, User actor) throws IOException {
        Order o = loadDetailed(actor, orderId);
        validateReadyForComplete(o);
        long ownerId = o.getOwner().getId();
        Optional<byte[]> fromCache =
                generatedDocumentCache.get(ownerId, orderId, documentType, format);
        byte[] bytes;
        if (fromCache.isPresent()) {
            bytes = fromCache.get();
        } else {
            bytes = documentGenerationService.generateDocument(o, documentType, format);
            generatedDocumentCache.put(ownerId, orderId, documentType, format, bytes);
        }
        String filename = documentGenerationService.downloadFileName(o, documentType, format);
        String contentType = DocumentGenerationService.contentTypeFor(format);
        return new DocumentDownload(new ByteArrayResource(bytes), filename, contentType);
    }

    public Order requireAccessibleOrder(Long id, User actor) {
        return loadDetailed(actor, id);
    }

    private Order loadDetailed(User actor, Long id) {
        if (UserAccess.isAdmin(actor)) {
            return orderRepository.findDetailedById(id).orElseThrow(this::notFound);
        }
        return orderRepository.findDetailedByIdAndOwner_Id(id, actor.getId()).orElseThrow(this::notFound);
    }

    private void apply(Order o, OrderUpdateRequest req, User actor) {
        if (req.customerId() != null) {
            Customer c = customerRepository.findById(req.customerId()).orElseThrow(this::notFound);
            assertCatalogRowAccessible(actor, c.getOwner().getId());
            o.setCustomer(c);
        }
        if (req.performerId() != null) {
            Performer p = performerRepository.findById(req.performerId()).orElseThrow(this::notFound);
            assertCatalogRowAccessible(actor, p.getOwner().getId());
            o.setPerformer(p);
            if (o.getVehicle() != null && !o.getVehicle().getOwner().getId().equals(p.getId())) {
                o.setVehicle(null);
            }
            if (o.getDriver() != null && !o.getDriver().getEmployer().getId().equals(p.getId())) {
                o.setDriver(null);
            }
        }
        if (req.vehicleId() != null) {
            Vehicle v = vehicleRepository.findById(req.vehicleId()).orElseThrow(this::notFound);
            assertPerformerOwnsVehicle(o.getPerformer(), v);
            assertVehicleVisible(actor, v);
            o.setVehicle(v);
        }
        if (req.driverId() != null) {
            Driver d = driverRepository.findById(req.driverId()).orElseThrow(this::notFound);
            assertPerformerEmploysDriver(o.getPerformer(), d);
            assertDriverVisible(actor, d);
            o.setDriver(d);
        }
        if (req.orderDate() != null) {
            o.setOrderDate(req.orderDate());
        }
        if (req.orderNumber() != null) {
            o.setOrderNumber(req.orderNumber());
        }
        if (req.loadingPlace() != null) {
            o.setLoadingPlace(req.loadingPlace());
        }
        if (req.loadingContact() != null) {
            o.setLoadingContact(req.loadingContact());
        }
        if (req.unloadingPlace() != null) {
            o.setUnloadingPlace(req.unloadingPlace());
        }
        if (req.unloadingContact() != null) {
            o.setUnloadingContact(req.unloadingContact());
        }
        if (req.tripCount() != null) {
            o.setTripCount(req.tripCount());
        }
        if (req.pricePerTrip() != null) {
            o.setPricePerTrip(req.pricePerTrip());
        }
        if (req.totalPrice() != null) {
            o.setTotalPrice(req.totalPrice());
        }
    }

    private void syncOrderOwner(Order o) {
        if (!o.getCustomer().getOwner().getId().equals(o.getPerformer().getOwner().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Заказчик и исполнитель должны принадлежать одному владельцу.");
        }
        o.setOwner(o.getCustomer().getOwner());
    }

    private void validateCustomerPerformerAccess(User actor, Customer customer, Performer performer) {
        if (!customer.getOwner().getId().equals(performer.getOwner().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Заказчик и исполнитель должны принадлежать одному владельцу.");
        }
        assertCatalogRowAccessible(actor, customer.getOwner().getId());
    }

    private void assertCatalogRowAccessible(User actor, Long dataOwnerId) {
        if (!UserAccess.isAdmin(actor) && !actor.getId().equals(dataOwnerId)) {
            throw notFound();
        }
    }

    private void upsertUserTripDefaults(User actor, Order o) {
        UserTripDefaults row =
                userTripDefaultsRepository.findByUser_Id(actor.getId()).orElseGet(() -> {
                    UserTripDefaults d = new UserTripDefaults();
                    d.setUser(userRepository.getReferenceById(actor.getId()));
                    return d;
                });
        row.setLastPerformer(o.getPerformer());
        row.setLastDriver(o.getDriver());
        row.setLastVehicle(o.getVehicle());
        row.setUpdatedAt(Instant.now());
        userTripDefaultsRepository.save(row);
    }

    private void assertVehicleVisible(User actor, Vehicle v) {
        assertCatalogRowAccessible(actor, v.getOwner().getOwner().getId());
    }

    private void assertDriverVisible(User actor, Driver d) {
        assertCatalogRowAccessible(actor, d.getEmployer().getOwner().getId());
    }

    private static void assertPerformerOwnsVehicle(Performer p, Vehicle v) {
        if (!v.getOwner().getId().equals(p.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Транспорт привязан к другому исполнителю.");
        }
    }

    private static void assertPerformerEmploysDriver(Performer p, Driver d) {
        if (!d.getEmployer().getId().equals(p.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Водитель привязан к другому исполнителю.");
        }
    }

    private static void validateReadyForComplete(Order o) {
        if (!OrderTripCompleteness.readyForTripDocuments(o)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Заполните все обязательные поля рейса.");
        }
    }

    private OrderResponse toDto(Order o) {
        boolean completed =
                auditEventRepository.existsByOrder_IdAndEventType(o.getId(), AuditEventType.ORDER_COMPLETED);
        return toDto(o, completed);
    }

    private OrderResponse toDto(Order o, boolean completed) {
        return new OrderResponse(
                o.getId(),
                o.getCustomer().getId(),
                o.getPerformer().getId(),
                o.getVehicle() != null ? o.getVehicle().getId() : null,
                o.getDriver() != null ? o.getDriver().getId() : null,
                o.getOrderNumber(),
                o.getOrderDate(),
                o.getLoadingPlace(),
                o.getLoadingContact(),
                o.getUnloadingPlace(),
                o.getUnloadingContact(),
                o.getTripCount(),
                o.getPricePerTrip(),
                o.getTotalPrice(),
                o.getTemplateVersion(),
                completed,
                o.getCustomer().getShortName(),
                o.getPerformer().getShortName());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Рейс не найден.");
    }
}
