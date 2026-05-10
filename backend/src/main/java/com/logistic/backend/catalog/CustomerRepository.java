package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByShortNameAsc();

    List<Customer> findByShortNameContainingIgnoreCaseOrderByShortNameAsc(String q);

    List<Customer> findByOwner_IdOrderByShortNameAsc(Long ownerId);

    List<Customer> findByOwner_IdAndShortNameContainingIgnoreCaseOrderByShortNameAsc(
            Long ownerId, String q);

    Optional<Customer> findByIdAndOwner_Id(Long id, Long ownerId);
}
