package com.logistic.backend.document;

import com.logistic.backend.trip.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    List<GeneratedDocument> findByTrip(Trip trip);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GeneratedDocument g where g.trip = :trip")
    void deleteByTrip(@Param("trip") Trip trip);

    Optional<GeneratedDocument> findByTripAndDocumentTypeAndFileFormat(
            Trip trip, DocumentType documentType, FileFormat fileFormat);
}
