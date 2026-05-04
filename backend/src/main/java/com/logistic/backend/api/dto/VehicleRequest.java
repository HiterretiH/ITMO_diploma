package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleRequest(@NotBlank String plateNumber, String model, Integer loadCapacityKg) {}
