package com.logistic.backend.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformerRepository extends JpaRepository<Performer, Long> {

    List<Performer> findAllByOrderByShortNameAsc();

    List<Performer> findByShortNameContainingIgnoreCaseOrderByShortNameAsc(String q);
}
