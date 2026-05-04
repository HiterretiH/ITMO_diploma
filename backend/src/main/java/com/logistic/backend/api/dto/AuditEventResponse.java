package com.logistic.backend.api.dto;

import com.logistic.backend.audit.AuditEventType;
import java.time.Instant;

public record AuditEventResponse(
        Long id, AuditEventType eventType, String payload, Instant createdAt, Long userId) {}
