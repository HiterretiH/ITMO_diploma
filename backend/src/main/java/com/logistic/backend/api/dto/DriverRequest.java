package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DriverRequest(@NotNull Long performerId, @NotBlank String fullName, String phone) {}
