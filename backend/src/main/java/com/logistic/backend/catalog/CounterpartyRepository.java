package com.logistic.backend.catalog;

import com.logistic.backend.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {

    List<Counterparty> findByOwnerAndNameContainingIgnoreCaseOrderByNameAsc(User owner, String q);

    List<Counterparty> findByOwnerOrderByNameAsc(User owner);
}
