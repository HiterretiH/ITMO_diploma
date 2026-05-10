package com.logistic.backend.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByShortNameAsc();

    List<Customer> findByShortNameContainingIgnoreCaseOrderByShortNameAsc(String q);
}
