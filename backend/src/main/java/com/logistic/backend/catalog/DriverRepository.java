package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findAllByOrderByFullNameAsc();

    List<Driver> findByFullNameContainingIgnoreCaseOrderByFullNameAsc(String q);

    List<Driver> findByEmployer_Owner_IdOrderByFullNameAsc(Long userId);

    List<Driver> findByEmployer_Owner_IdAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
            Long userId, String q);

    Optional<Driver> findByIdAndEmployer_Owner_Id(Long id, Long userId);

    List<Driver> findByEmployerOrderByFullNameAsc(Performer employer);

    List<Driver> findByEmployerAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
            Performer employer, String q);
}
