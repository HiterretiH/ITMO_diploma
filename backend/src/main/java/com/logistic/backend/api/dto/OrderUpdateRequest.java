package com.logistic.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderUpdateRequest(
        Long customerId,
        Long performerId,
        Long vehicleId,
        Long driverId,
        LocalDate orderDate,
        Integer orderNumber,
        String loadingPlace,
        String loadingContact,
        String unloadingPlace,
        String unloadingContact,
        Integer tripCount,
        BigDecimal pricePerTrip,
        BigDecimal totalPrice) {}
