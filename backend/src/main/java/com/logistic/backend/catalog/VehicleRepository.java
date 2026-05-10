package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    long countByOwner_Id(Long ownerId);

    List<Vehicle> findAllByOrderByPlateNumberAsc();

    List<Vehicle> findByPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(String q);

    List<Vehicle> findByOwner_Owner_IdOrderByPlateNumberAsc(Long userId);

    List<Vehicle> findByOwner_Owner_IdAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(
            Long userId, String q);

    Optional<Vehicle> findByIdAndOwner_Owner_Id(Long id, Long userId);

    List<Vehicle> findByOwnerOrderByPlateNumberAsc(Performer owner);

    List<Vehicle> findByOwnerAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(
            Performer owner, String q);

    Optional<Vehicle> findByOwner_IdAndDefaultForPerformerIsTrue(Long performerId);

    @Modifying
    @Query("update Vehicle v set v.defaultForPerformer = false where v.owner.id = :performerId")
    void clearDefaultForOwner(@Param("performerId") Long performerId);
}
