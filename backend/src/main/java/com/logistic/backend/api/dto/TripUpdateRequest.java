package com.logistic.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripUpdateRequest(
        Long shipperId,
        Long consigneeId,
        Long driverId,
        Long vehicleId,
        String cargoDescription,
        BigDecimal cargoWeightKg,
        String routeFrom,
        String routeTo,
        LocalDate loadDate,
        LocalDate unloadDate,
        BigDecimal priceAmount,
        String currency) {}
