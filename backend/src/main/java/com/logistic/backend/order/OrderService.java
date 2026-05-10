package com.logistic.backend.order;

import com.logistic.backend.api.dto.DocumentDownload;
import com.logistic.backend.api.dto.GeneratedDocumentResponse;
import com.logistic.backend.api.dto.OrderCreateRequest;
import com.logistic.backend.api.dto.OrderResponse;
import com.logistic.backend.api.dto.OrderUpdateRequest;
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
import com.logistic.backend.config.StorageProperties;
import com.logistic.backend.document.DocumentGenerationService;
import com.logistic.backend.document.DocumentTemplateVersion;
import com.logistic.backend.document.FileFormat;
import com.logistic.backend.document.GeneratedDocument;
import com.logistic.backend.document.GeneratedDocumentRepository;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserAccess;
import java.io.IOException;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final DocumentGenerationService documentGenerationService;
    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final StorageProperties storageProperties;

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
        applyPerformerDefaults(o);
        orderRepository.save(o);
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
        auditService.record(
                actor, o, AuditEventType.ORDER_UPDATED, Map.of("orderId", o.getId().toString()));
        return toDto(o);
    }

    @Transactional
    public OrderResponse complete(Long id, User actor) {
        Order o = loadDetailed(actor, id);
        validateReadyForComplete(o);
        try {
            documentGenerationService.generateAndPersist(o);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Document generation failed", e);
        }
        o.setTemplateVersion(DocumentTemplateVersion.CURRENT);
        orderRepository.save(o);
        auditService.record(actor, o, AuditEventType.ORDER_COMPLETED, Map.of("orderId", id.toString()));
        auditService.record(
                actor, o, AuditEventType.DOCUMENTS_GENERATED, Map.of("orderId", id.toString()));
        return toDto(o);
    }

    @Transactional
    public void delete(Long id, User actor) {
        Order o = loadDetailed(actor, id);
        Long orderId = o.getId();
        deleteOrderStorageBestEffort(storageProperties.getRoot(), orderId);
        orderRepository.delete(o);
        auditService.record(actor, AuditEventType.ORDER_DELETED, Map.of("orderId", orderId.toString()));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id, User actor) {
        return toDto(loadDetailed(actor, id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(User actor) {
        if (UserAccess.isAdmin(actor)) {
            return orderRepository.findAllDetailedOrderByOrderDateDesc().stream()
                    .map(this::toDto)
                    .toList();
        }
        return orderRepository.findAllDetailedByOwner_IdOrderByOrderDateDesc(actor.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocumentResponse> listDocuments(Long orderId, User actor) {
        Order o = loadDetailed(actor, orderId);
        return generatedDocumentRepository.findByOrder(o).stream()
                .map(
                        g ->
                                new GeneratedDocumentResponse(
                                        g.getId(),
                                        g.getDocumentType(),
                                        g.getFileFormat(),
                                        g.getContentSha256(),
                                        g.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownload prepareDocumentDownload(Long documentId, User actor) throws IOException {
        GeneratedDocument gd =
                generatedDocumentRepository.findById(documentId).orElseThrow(this::notFound);
        loadDetailed(actor, gd.getOrder().getId());
        byte[] bytes = Files.readAllBytes(Path.of(gd.getStoragePath()));
        String ext = gd.getFileFormat() == FileFormat.PDF ? ".pdf" : ".docx";
        String filename = gd.getDocumentType().name().toLowerCase() + ext;
        String contentType =
                gd.getFileFormat() == FileFormat.PDF
                        ? MediaType.APPLICATION_PDF_VALUE
                        : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
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
            applyPerformerDefaults(o);
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
                    HttpStatus.BAD_REQUEST, "Customer and performer must belong to the same owner");
        }
        o.setOwner(o.getCustomer().getOwner());
    }

    private void validateCustomerPerformerAccess(User actor, Customer customer, Performer performer) {
        if (!customer.getOwner().getId().equals(performer.getOwner().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Customer and performer must belong to the same owner");
        }
        assertCatalogRowAccessible(actor, customer.getOwner().getId());
    }

    private void assertCatalogRowAccessible(User actor, Long dataOwnerId) {
        if (!UserAccess.isAdmin(actor) && !actor.getId().equals(dataOwnerId)) {
            throw notFound();
        }
    }

    private void assertVehicleVisible(User actor, Vehicle v) {
        assertCatalogRowAccessible(actor, v.getOwner().getOwner().getId());
    }

    private void assertDriverVisible(User actor, Driver d) {
        assertCatalogRowAccessible(actor, d.getEmployer().getOwner().getId());
    }

    private void applyPerformerDefaults(Order o) {
        Performer p = o.getPerformer();
        if (p == null) {
            return;
        }
        Long pid = p.getId();
        if (o.getVehicle() == null) {
            vehicleRepository.findByOwner_IdAndDefaultForPerformerIsTrue(pid).ifPresent(o::setVehicle);
        }
        if (o.getDriver() == null) {
            driverRepository.findByEmployer_IdAndDefaultForEmployerIsTrue(pid).ifPresent(o::setDriver);
        }
    }

    private static void assertPerformerOwnsVehicle(Performer p, Vehicle v) {
        if (!v.getOwner().getId().equals(p.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle belongs to another performer");
        }
    }

    private static void assertPerformerEmploysDriver(Performer p, Driver d) {
        if (!d.getEmployer().getId().equals(p.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver belongs to another performer");
        }
    }

    private static void validateReadyForComplete(Order o) {
        if (o.getVehicle() == null
                || o.getDriver() == null
                || o.getLoadingPlace() == null
                || o.getLoadingPlace().isBlank()
                || o.getUnloadingPlace() == null
                || o.getUnloadingPlace().isBlank()
                || o.getOrderDate() == null
                || o.getTotalPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incomplete order data");
        }
    }

    private static void deleteOrderStorageBestEffort(String root, long orderId) {
        Path dir = Path.of(root).resolve("orders").resolve(Long.toString(orderId));
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(
                            p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {
                                }
                            });
        } catch (IOException ignored) {
        }
    }

    private OrderResponse toDto(Order o) {
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
                o.getTemplateVersion());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
