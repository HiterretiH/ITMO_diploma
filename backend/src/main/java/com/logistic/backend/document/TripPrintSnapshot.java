package com.logistic.backend.document;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripPrintSnapshot(
        Long tripId,
        String ownerUsername,
        String shipperName,
        String shipperInn,
        String shipperAddress,
        String consigneeName,
        String consigneeInn,
        String consigneeAddress,
        String cargoDescription,
        BigDecimal cargoWeightKg,
        String routeFrom,
        String routeTo,
        LocalDate loadDate,
        LocalDate unloadDate,
        String driverName,
        String driverLicense,
        String vehiclePlate,
        String vehicleModel,
        Integer vehicleCapacityKg,
        BigDecimal priceAmount,
        String currency) {}