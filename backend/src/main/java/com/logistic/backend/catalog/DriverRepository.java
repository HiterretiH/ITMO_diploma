package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    long countByEmployer_Id(Long employerId);

    List<Driver> findAllByOrderByFullNameAsc();

    List<Driver> findByFullNameContainingIgnoreCaseOrderByFullNameAsc(String q);

    List<Driver> findByEmployer_Owner_IdOrderByFullNameAsc(Long userId);

    List<Driver> findByEmployer_Owner_IdAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
            Long userId, String q);

    Optional<Driver> findByIdAndEmployer_Owner_Id(Long id, Long userId);

    List<Driver> findByEmployerOrderByFullNameAsc(Performer employer);

    List<Driver> findByEmployerAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
            Performer employer, String q);

    Optional<Driver> findByEmployer_IdAndDefaultForEmployerIsTrue(Long employerId);

    @Modifying
    @Query("update Driver d set d.defaultForEmployer = false where d.employer.id = :employerId")
    void clearDefaultForEmployer(@Param("employerId") Long employerId);
}
