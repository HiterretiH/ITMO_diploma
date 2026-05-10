package com.logistic.backend.api.dto;

public record TripFormDraftResponse(
        Integer nextOrderNumber,
        Long lastPerformerId,
        Long lastDriverId,
        Long lastVehicleId) {}
