package com.logistic.backend.document;

import com.logistic.backend.trip.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    List<GeneratedDocument> findByTrip(Trip trip);

    void deleteByTrip(Trip trip);

    Optional<GeneratedDocument> findByTripAndDocumentTypeAndFileFormat(
            Trip trip, DocumentType documentType, FileFormat fileFormat);
}
