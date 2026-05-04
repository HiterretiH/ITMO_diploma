package com.logistic.backend.audit;

import com.logistic.backend.trip.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByTrip_IdOrderByCreatedAtAsc(Long tripId);
}
