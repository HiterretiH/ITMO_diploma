package com.logistic.backend.api.dto;

public record PerformerResponse(
        Long id,
        String shortName,
        String fullName,
        String phone,
        String bankName,
        String inn,
        String bik,
        String kpp,
        String paymentAccount,
        String corrAccount,
        String requisites) {}
