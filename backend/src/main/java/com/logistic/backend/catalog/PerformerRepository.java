package com.logistic.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformerRepository extends JpaRepository<Performer, Long> {

    List<Performer> findAllByOrderByShortNameAsc();

    List<Performer> findByShortNameContainingIgnoreCaseOrderByShortNameAsc(String q);

    List<Performer> findByOwner_IdOrderByShortNameAsc(Long ownerId);

    List<Performer> findByOwner_IdAndShortNameContainingIgnoreCaseOrderByShortNameAsc(
            Long ownerId, String q);

    Optional<Performer> findByIdAndOwner_Id(Long id, Long ownerId);
}
