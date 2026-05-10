package com.logistic.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderResponse(
        Long id,
        Long customerId,
        Long performerId,
        Long vehicleId,
        Long driverId,
        Integer orderNumber,
        LocalDate orderDate,
        String loadingPlace,
        String loadingContact,
        String unloadingPlace,
        String unloadingContact,
        int tripCount,
        BigDecimal pricePerTrip,
        BigDecimal totalPrice,
        int templateVersion,
        boolean completed,
        String customerShortName,
        String performerShortName) {}
