package com.logistic.backend.audit;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByOrder_IdOrderByCreatedAtAsc(Long orderId);

    boolean existsByOrder_IdAndEventType(Long orderId, AuditEventType eventType);

    @Query(
            "SELECT DISTINCT e.order.id FROM AuditEvent e WHERE e.order.id IN :ids AND e.eventType = :type")
    Set<Long> findOrderIdsByOrder_IdInAndEventType(
            @Param("ids") Collection<Long> ids, @Param("type") AuditEventType type);
}
