package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findAllByOrderByFullNameAsc();

    List<Driver> findByFullNameContainingIgnoreCaseOrderByFullNameAsc(String q);

    List<Driver> findByEmployerOrderByFullNameAsc(Performer employer);

    List<Driver> findByEmployerAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
            Performer employer, String q);

    Optional<Driver> findByEmployer_IdAndDefaultForEmployerIsTrue(Long employerId);

    @Modifying
    @Query("update Driver d set d.defaultForEmployer = false where d.employer.id = :employerId")
    void clearDefaultForEmployer(@Param("employerId") Long employerId);
}
