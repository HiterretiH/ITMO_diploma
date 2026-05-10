package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotNull Long customerId,
        @NotNull Long performerId,
        Long vehicleId,
        Long driverId) {}
