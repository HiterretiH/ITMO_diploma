package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPlaceRepository extends JpaRepository<CustomerPlace, Long> {

    Optional<CustomerPlace> findByCustomer_IdAndKindAndAddressKey(
            long customerId, CustomerPlaceKind kind, String addressKey);

    List<CustomerPlace> findByCustomer_IdAndKindOrderByUpdatedAtDesc(
            long customerId, CustomerPlaceKind kind);
}
