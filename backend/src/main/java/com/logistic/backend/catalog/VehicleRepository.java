package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByOrderByPlateNumberAsc();

    List<Vehicle> findByPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(String q);

    List<Vehicle> findByOwner_Owner_IdOrderByPlateNumberAsc(Long userId);

    List<Vehicle> findByOwner_Owner_IdAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(
            Long userId, String q);

    Optional<Vehicle> findByIdAndOwner_Owner_Id(Long id, Long userId);

    List<Vehicle> findByOwnerOrderByPlateNumberAsc(Performer owner);

    List<Vehicle> findByOwnerAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(
            Performer owner, String q);
}
