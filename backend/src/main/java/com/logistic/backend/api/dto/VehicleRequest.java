package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotNull;

public record VehicleRequest(
        @NotNull Long performerId,
        String brandModel,
        String plateNumber,
        String type) {}
