package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PerformerRequest(
        @NotBlank String shortName,
        String fullName,
        String phone,
        String bankName,
        String inn,
        String bik,
        String kpp,
        String paymentAccount,
        String corrAccount,
        String requisites) {}
