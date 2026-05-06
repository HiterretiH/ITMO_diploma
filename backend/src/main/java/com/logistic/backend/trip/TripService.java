package com.logistic.backend.trip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistic.backend.api.dto.DocumentDownload;
import com.logistic.backend.api.dto.GeneratedDocumentResponse;
import com.logistic.backend.api.dto.TripResponse;
import com.logistic.backend.api.dto.TripUpdateRequest;
import com.logistic.backend.audit.AuditEventType;
import com.logistic.backend.audit.AuditService;
import com.logistic.backend.catalog.Counterparty;
import com.logistic.backend.catalog.CounterpartyRepository;
import com.logistic.backend.catalog.Driver;
import com.logistic.backend.catalog.DriverRepository;
import com.logistic.backend.catalog.Vehicle;
import com.logistic.backend.catalog.VehicleRepository;
import com.logistic.backend.config.StorageProperties;
import com.logistic.backend.document.DocumentGenerationService;
import com.logistic.backend.document.FileFormat;
import com.logistic.backend.document.GeneratedDocument;
import com.logistic.backend.document.GeneratedDocumentRepository;
import com.logistic.backend.document.TripPrintSnapshot;
import com.logistic.backend.document.TripSnapshotMapper;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class TripService {

    private final TripRepository tripRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ObjectMapper objectMapper;
    private final TripSnapshotMapper tripSnapshotMapper;
    private final AuditService auditService;
    private final DocumentGenerationService documentGenerationService;
    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final StorageProperties storageProperties;

    @Transactional
    public TripResponse create(User owner) {
        Trip t = new Trip();
        t.setOwner(owner);
        t.setStatus(TripStatus.IN_PROGRESS);
        tripRepository.save(t);
        auditService.record(
                owner, t, AuditEventType.TRIP_CREATED, Map.of("tripId", t.getId().toString()));
        return toDto(t);
    }

    @Transactional
    public TripResponse update(Long id, TripUpdateRequest req, User current) {
        Trip t = loadForOwnerEdit(id, current);
        apply(t, req, current);
        if (t.getStatus() == TripStatus.COMPLETED) {
            validateReadyForComplete(t);
        }
        tripRepository.save(t);
        if (t.getStatus() == TripStatus.COMPLETED) {
            refreshSnapshotAndRegenerateDocuments(t);
        }
        auditService.record(
                current, t, AuditEventType.TRIP_UPDATED, Map.of("tripId", t.getId().toString()));
        return toDto(t);
    }

    @Transactional
    public TripResponse complete(Long id, User actor) {
        Trip t = loadTripDetailedForCompleteOrDelete(id, actor);
        if (t.getStatus() == TripStatus.COMPLETED) {
            throw conflict("Trip already completed");
        }
        validateReadyForComplete(t);
        TripPrintSnapshot snap = tripSnapshotMapper.fromTrip(t);
        try {
            t.setSnapshotJson(objectMapper.writeValueAsString(snap));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "snapshot failed");
        }
        try {
            documentGenerationService.generateAndPersist(t);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Document generation failed", e);
        }
        t.setStatus(TripStatus.COMPLETED);
        tripRepository.save(t);
        auditService.record(actor, t, AuditEventType.TRIP_COMPLETED, Map.of("tripId", id.toString()));
        auditService.record(
                actor,
                t,
                AuditEventType.DOCUMENTS_GENERATED,
                Map.of("tripId", id.toString()));
        return toDto(t);
    }

    @Transactional
    public void delete(Long id, User actor) {
        Trip t = loadTripDetailedForCompleteOrDelete(id, actor);
        Long tripId = t.getId();
        deleteTripStorageBestEffort(storageProperties.getRoot(), tripId);
        tripRepository.delete(t);
        // Log without Trip FK — avoids Hibernate flush ordering issues; DB still keeps row with trip_id NULL.
        auditService.record(actor, AuditEventType.TRIP_DELETED, Map.of("tripId", tripId.toString()));
    }

    @Transactional(readOnly = true)
    public TripResponse get(Long id, User current) {
        Trip t = requireAccessibleTrip(id, current);
        return toDto(t);
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocumentResponse> listDocuments(Long tripId, User current) {
        Trip t = requireAccessibleTrip(tripId, current);
        if (t.getStatus() != TripStatus.COMPLETED) {
            return List.of();
        }
        return generatedDocumentRepository.findByTrip(t).stream()
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
    public DocumentDownload prepareDocumentDownload(Long documentId, User current)
            throws IOException {
        GeneratedDocument gd =
                generatedDocumentRepository.findById(documentId).orElseThrow(this::notFound);
        requireAccessibleTrip(gd.getTrip().getId(), current);
        byte[] bytes = Files.readAllBytes(Path.of(gd.getStoragePath()));
        String ext = gd.getFileFormat() == FileFormat.PDF ? ".pdf" : ".docx";
        String filename = gd.getDocumentType().name().toLowerCase() + ext;
        String contentType =
                gd.getFileFormat() == FileFormat.PDF
                        ? MediaType.APPLICATION_PDF_VALUE
                        : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return new DocumentDownload(new ByteArrayResource(bytes), filename, contentType);
    }

    public Trip requireAccessibleTrip(Long id, User current) {
        return loadForView(id, current);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> list(User current, TripStatus status) {
        if (isPrivileged(current)) {
            if (status == null) {
                return tripRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList();
            }
            return tripRepository.findByStatusOrderByUpdatedAtDesc(status).stream()
                    .map(this::toDto)
                    .toList();
        }
        if (status == null) {
            return tripRepository.findByOwnerOrderByUpdatedAtDesc(current).stream()
                    .map(this::toDto)
                    .toList();
        }
        return tripRepository.findByOwnerAndStatusOrderByUpdatedAtDesc(current, status).stream()
                .map(this::toDto)
                .toList();
    }

    private Trip loadForOwnerEdit(Long id, User current) {
        return tripRepository.findDetailedForOwner(id, current).orElseThrow(this::notFound);
    }

    /** Owner or privileged user (manager/admin); otherwise 403 if trip exists. */
    private Trip loadTripDetailedForCompleteOrDelete(Long id, User actor) {
        Trip t = tripRepository.findDetailedById(id).orElseThrow(this::notFound);
        if (isPrivileged(actor)) {
            return t;
        }
        if (!t.getOwner().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return t;
    }

    private Trip loadForView(Long id, User current) {
        Trip t = tripRepository.findById(id).orElseThrow(this::notFound);
        if (isPrivileged(current)) {
            return t;
        }
        if (!t.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return t;
    }

    private void refreshSnapshotAndRegenerateDocuments(Trip t) {
        TripPrintSnapshot snap = tripSnapshotMapper.fromTrip(t);
        try {
            t.setSnapshotJson(objectMapper.writeValueAsString(snap));
            tripRepository.save(t);
            documentGenerationService.generateAndPersist(t);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "snapshot failed", e);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Document generation failed", e);
        }
    }

    private static void deleteTripStorageBestEffort(String root, long tripId) {
        Path dir = Path.of(root).resolve("trips").resolve(Long.toString(tripId));
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

    private void apply(Trip t, TripUpdateRequest req, User owner) {
        if (req.shipperId() != null) {
            Counterparty c = counterpartyRepository.findById(req.shipperId()).orElseThrow(this::notFound);
            assertOwned(owner, c.getOwner().getId());
            t.setShipper(c);
        }
        if (req.consigneeId() != null) {
            Counterparty c = counterpartyRepository.findById(req.consigneeId()).orElseThrow(this::notFound);
            assertOwned(owner, c.getOwner().getId());
            t.setConsignee(c);
        }
        if (req.driverId() != null) {
            Driver d = driverRepository.findById(req.driverId()).orElseThrow(this::notFound);
            assertOwned(owner, d.getOwner().getId());
            t.setDriver(d);
        }
        if (req.vehicleId() != null) {
            Vehicle v = vehicleRepository.findById(req.vehicleId()).orElseThrow(this::notFound);
            assertOwned(owner, v.getOwner().getId());
            t.setVehicle(v);
        }
        if (req.cargoDescription() != null) {
            t.setCargoDescription(req.cargoDescription());
        }
        if (req.cargoWeightKg() != null) {
            t.setCargoWeightKg(req.cargoWeightKg());
        }
        if (req.routeFrom() != null) {
            t.setRouteFrom(req.routeFrom());
        }
        if (req.routeTo() != null) {
            t.setRouteTo(req.routeTo());
        }
        if (req.loadDate() != null) {
            t.setLoadDate(req.loadDate());
        }
        if (req.unloadDate() != null) {
            t.setUnloadDate(req.unloadDate());
        }
        if (req.priceAmount() != null) {
            t.setPriceAmount(req.priceAmount());
        }
        if (req.currency() != null) {
            t.setCurrency(req.currency());
        }
    }

    private static void validateReadyForComplete(Trip t) {
        if (t.getShipper() == null
                || t.getConsignee() == null
                || t.getDriver() == null
                || t.getVehicle() == null
                || t.getCargoDescription() == null
                || t.getCargoDescription().isBlank()
                || t.getCargoWeightKg() == null
                || t.getRouteFrom() == null
                || t.getRouteFrom().isBlank()
                || t.getRouteTo() == null
                || t.getRouteTo().isBlank()
                || t.getLoadDate() == null
                || t.getUnloadDate() == null
                || t.getPriceAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incomplete trip data");
        }
    }

    private static void assertOwned(User owner, Long entityOwnerId) {
        if (!owner.getId().equals(entityOwnerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foreign catalog entry");
        }
    }

    private TripResponse toDto(Trip t) {
        return new TripResponse(
                t.getId(),
                t.getOwner().getId(),
                t.getOwner().getUsername(),
                t.getStatus(),
                t.getShipper() != null ? t.getShipper().getId() : null,
                t.getConsignee() != null ? t.getConsignee().getId() : null,
                t.getDriver() != null ? t.getDriver().getId() : null,
                t.getVehicle() != null ? t.getVehicle().getId() : null,
                t.getCargoDescription(),
                t.getCargoWeightKg(),
                t.getRouteFrom(),
                t.getRouteTo(),
                t.getLoadDate(),
                t.getUnloadDate(),
                t.getPriceAmount(),
                t.getCurrency(),
                t.getUpdatedAt());
    }

    private static boolean isPrivileged(User u) {
        Set<Role> roles = u.getRoles();
        return roles.contains(Role.MANAGER) || roles.contains(Role.ADMIN);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
