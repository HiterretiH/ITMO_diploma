package com.logistic.backend.api;

import com.logistic.backend.api.dto.AuditEventResponse;
import com.logistic.backend.audit.AuditEvent;
import com.logistic.backend.audit.AuditEventRepository;
import com.logistic.backend.security.CurrentUserService;
import com.logistic.backend.trip.TripService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/audit-events")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final TripService tripService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<AuditEventResponse> list(@PathVariable Long tripId) {
        tripService.requireAccessibleTrip(tripId, currentUserService.requireUser());
        return auditEventRepository.findByTrip_IdOrderByCreatedAtAsc(tripId).stream()
                .map(this::toDto)
                .toList();
    }

    private AuditEventResponse toDto(AuditEvent e) {
        return new AuditEventResponse(
                e.getId(),
                e.getEventType(),
                e.getPayload(),
                e.getCreatedAt(),
                e.getUser() != null ? e.getUser().getId() : null);
    }
}
