package com.logistic.backend.api;

import com.logistic.backend.api.dto.AuditEventResponse;
import com.logistic.backend.audit.AuditEvent;
import com.logistic.backend.audit.AuditEventRepository;
import com.logistic.backend.order.OrderService;
import com.logistic.backend.security.CurrentUserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/audit-events")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<AuditEventResponse> list(@PathVariable Long orderId) {
        orderService.requireAccessibleOrder(orderId, currentUserService.requireUser());
        return auditEventRepository.findByOrder_IdOrderByCreatedAtAsc(orderId).stream()
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
