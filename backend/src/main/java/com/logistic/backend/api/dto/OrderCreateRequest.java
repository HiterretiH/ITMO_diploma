package com.logistic.backend.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderCreateRequest(
        @NotNull Long customerId,
        @NotNull Long performerId,
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
        BigDecimal totalPrice) {

    /** Sparse body for tests and legacy clients (addresses empty until applied). */
    public static OrderCreateRequest minimal(long customerId, long performerId) {
        return new OrderCreateRequest(
                customerId,
                performerId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /** Same fields as {@link OrderUpdateRequest} for reuse in {@link com.logistic.backend.order.OrderService}. */
    public OrderUpdateRequest toUpdateMask() {
        return new OrderUpdateRequest(
                customerId,
                performerId,
                vehicleId,
                driverId,
                orderDate,
                orderNumber,
                loadingPlace,
                loadingContact,
                unloadingPlace,
                unloadingContact,
                tripCount,
                pricePerTrip,
                totalPrice);
    }
}
