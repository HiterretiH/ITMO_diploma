package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByOrderByPlateNumberAsc();

    List<Vehicle> findByPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(String q);

    List<Vehicle> findByOwnerOrderByPlateNumberAsc(Performer owner);

    List<Vehicle> findByOwnerAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(
            Performer owner, String q);

    Optional<Vehicle> findByOwner_IdAndDefaultForPerformerIsTrue(Long ownerId);

    @Modifying
    @Query("update Vehicle v set v.defaultForPerformer = false where v.owner.id = :ownerId")
    void clearDefaultForOwner(@Param("ownerId") Long ownerId);
}
