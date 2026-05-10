package com.logistic.backend.document;

import com.logistic.backend.order.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    List<GeneratedDocument> findByOrder(Order order);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GeneratedDocument g where g.order = :order")
    void deleteByOrder(@Param("order") Order order);

    Optional<GeneratedDocument> findByOrderAndDocumentTypeAndFileFormat(
            Order order, DocumentType documentType, FileFormat fileFormat);
}
