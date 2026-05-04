package com.logistic.backend.api.dto;

import com.logistic.backend.trip.TripStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TripResponse(
        Long id,
        Long ownerId,
        String ownerUsername,
        TripStatus status,
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
        String currency,
        Instant updatedAt) {}
