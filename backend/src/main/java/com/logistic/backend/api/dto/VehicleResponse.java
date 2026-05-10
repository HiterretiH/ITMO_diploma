package com.logistic.backend.api.dto;

public record VehicleResponse(
        Long id, Long performerId, String brandModel, String plateNumber, String type) {}
