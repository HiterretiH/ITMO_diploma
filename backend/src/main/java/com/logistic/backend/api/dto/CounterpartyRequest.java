package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CounterpartyRequest(
        @NotBlank String name,
        @Pattern(regexp = "^$|^([0-9]{10}|[0-9]{12})$") String inn,
        String legalAddress,
        String phone) {}
