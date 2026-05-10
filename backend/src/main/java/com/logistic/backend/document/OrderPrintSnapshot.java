package com.logistic.backend.document;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderPrintSnapshot(
        Long orderId,
        Integer orderNumber,
        LocalDate orderDate,
        String customerShortName,
        String customerFullName,
        String customerPhone,
        String customerRequisites,
        String performerShortName,
        String performerFullName,
        String performerPhone,
        String performerBankName,
        String performerInn,
        String performerBik,
        String performerKpp,
        String performerPaymentAccount,
        String performerCorrAccount,
        String performerRequisites,
        String vehicleBrandModel,
        String vehiclePlateNumber,
        String vehicleType,
        String driverFullName,
        String driverPhone,
        String loadingPlace,
        String loadingContact,
        String unloadingPlace,
        String unloadingContact,
        int tripCount,
        BigDecimal pricePerTrip,
        BigDecimal totalPrice) {}
