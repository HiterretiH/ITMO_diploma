package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRouteHintRepository extends JpaRepository<CustomerRouteHint, Long> {

    Optional<CustomerRouteHint> findByCustomer_IdAndKindAndPlaceKey(
            long customerId, RouteHintKind kind, String placeKey);

    List<CustomerRouteHint> findByCustomer_IdAndKindOrderByUpdatedAtDesc(long customerId, RouteHintKind kind);
}
