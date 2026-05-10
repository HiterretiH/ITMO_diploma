package com.logistic.backend.audit;

public enum AuditEventType {
    REGISTER,
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_COMPLETED,
    ORDER_REOPENED,
    ORDER_DELETED,
    DOCUMENTS_GENERATED,
    LOGIN
}
