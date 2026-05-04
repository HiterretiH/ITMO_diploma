package com.logistic.backend.api.dto;

public record CounterpartyResponse(
        Long id, String name, String inn, String legalAddress, String phone) {}
