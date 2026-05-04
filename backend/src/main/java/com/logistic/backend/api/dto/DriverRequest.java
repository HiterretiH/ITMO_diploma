package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverRequest(
        @NotBlank String fullName,
        @NotBlank String licenseNumber,
        String licenseCategory) {}
