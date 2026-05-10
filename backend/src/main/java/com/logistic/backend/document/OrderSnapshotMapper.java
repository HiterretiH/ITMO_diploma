package com.logistic.backend.document;

import com.logistic.backend.catalog.Driver;
import com.logistic.backend.catalog.Vehicle;
import com.logistic.backend.order.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderSnapshotMapper {

    public OrderPrintSnapshot fromOrder(Order o) {
        Vehicle v = o.getVehicle();
        Driver d = o.getDriver();
        return new OrderPrintSnapshot(
                o.getId(),
                o.getOrderNumber(),
                o.getOrderDate(),
                o.getCustomer().getShortName(),
                o.getCustomer().getFullName(),
                o.getCustomer().getPhone(),
                o.getCustomer().getRequisites(),
                o.getPerformer().getShortName(),
                o.getPerformer().getFullName(),
                o.getPerformer().getPhone(),
                o.getPerformer().getBankName(),
                o.getPerformer().getInn(),
                o.getPerformer().getBik(),
                o.getPerformer().getKpp(),
                o.getPerformer().getPaymentAccount(),
                o.getPerformer().getCorrAccount(),
                o.getPerformer().getRequisites(),
                v != null ? v.getBrandModel() : null,
                v != null ? v.getPlateNumber() : null,
                v != null ? v.getType() : null,
                d != null ? d.getFullName() : null,
                d != null ? d.getPhone() : null,
                o.getLoadingPlace(),
                o.getLoadingContact(),
                o.getUnloadingPlace(),
                o.getUnloadingContact(),
                o.getTripCount(),
                o.getPricePerTrip(),
                o.getTotalPrice());
    }
}
