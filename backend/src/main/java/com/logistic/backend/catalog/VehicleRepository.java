package com.logistic.backend.catalog;

import com.logistic.backend.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(User owner, String q);

    List<Vehicle> findByOwnerOrderByPlateNumberAsc(User owner);
}
