package com.logistic.backend.document;

import com.logistic.backend.trip.Trip;
import org.springframework.stereotype.Component;

@Component
public class TripSnapshotMapper {

    public TripPrintSnapshot fromTrip(Trip trip) {
        return new TripPrintSnapshot(
                trip.getId(),
                trip.getOwner().getUsername(),
                trip.getShipper() != null ? trip.getShipper().getName() : null,
                trip.getShipper() != null ? trip.getShipper().getInn() : null,
                trip.getShipper() != null ? trip.getShipper().getLegalAddress() : null,
                trip.getConsignee() != null ? trip.getConsignee().getName() : null,
                trip.getConsignee() != null ? trip.getConsignee().getInn() : null,
                trip.getConsignee() != null ? trip.getConsignee().getLegalAddress() : null,
                trip.getCargoDescription(),
                trip.getCargoWeightKg(),
                trip.getRouteFrom(),
                trip.getRouteTo(),
                trip.getLoadDate(),
                trip.getUnloadDate(),
                trip.getDriver() != null ? trip.getDriver().getFullName() : null,
                trip.getDriver() != null ? trip.getDriver().getLicenseNumber() : null,
                trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null,
                trip.getVehicle() != null ? trip.getVehicle().getModel() : null,
                trip.getVehicle() != null ? trip.getVehicle().getLoadCapacityKg() : null,
                trip.getPriceAmount(),
                trip.getCurrency());
    }
}
