package com.logistic.backend.api.dto;

public record CustomerResponse(
        Long id, String shortName, String fullName, String phone, String requisites) {}
