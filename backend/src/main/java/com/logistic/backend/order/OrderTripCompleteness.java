package com.logistic.backend.order;

/** Mirrors mandatory trip fields required before generating or downloading documents. */
public final class OrderTripCompleteness {

    private OrderTripCompleteness() {}

    public static boolean readyForTripDocuments(Order o) {
        return o.getVehicle() != null
                && o.getDriver() != null
                && o.getLoadingPlace() != null
                && !o.getLoadingPlace().isBlank()
                && o.getUnloadingPlace() != null
                && !o.getUnloadingPlace().isBlank()
                && o.getOrderDate() != null
                && o.getTotalPrice() != null;
    }
}
