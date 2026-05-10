package com.logistic.backend.api.dto;

public record DriverResponse(
        Long id, Long performerId, String fullName, String phone, boolean isDefault) {}
