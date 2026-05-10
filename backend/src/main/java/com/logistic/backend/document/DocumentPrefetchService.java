package com.logistic.backend.document;

import com.logistic.backend.order.Order;
import com.logistic.backend.order.OrderRepository;
import com.logistic.backend.order.OrderTripCompleteness;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPrefetchService {

    private final OrderRepository orderRepository;
    private final DocumentGenerationService documentGenerationService;
    private final GeneratedDocumentCache generatedDocumentCache;

    /**
     * Generates all document type/format pairs into the in-memory cache for the order owner.
     * Skips work if the order is missing, owner mismatches, or trip data is incomplete.
     */
    @Async("documentPrefetchExecutor")
    public void prefetchOrderDocuments(Long ownerUserId, Long orderId) {
        Order o = orderRepository.findDetailedById(orderId).orElse(null);
        if (o == null) {
            return;
        }
        if (!o.getOwner().getId().equals(ownerUserId)) {
            return;
        }
        if (!OrderTripCompleteness.readyForTripDocuments(o)) {
            return;
        }
        Map<String, byte[]> bundle = new HashMap<>();
        for (DocumentType dt : DocumentType.values()) {
            for (FileFormat ff : FileFormat.values()) {
                try {
                    byte[] bytes = documentGenerationService.generateDocument(o, dt, ff);
                    bundle.put(GeneratedDocumentCache.compositeKey(dt, ff), bytes);
                } catch (IOException e) {
                    log.warn("Prefetch failed for order {} {} {}", orderId, dt, ff, e);
                }
            }
        }
        if (!bundle.isEmpty()) {
            generatedDocumentCache.putBundle(ownerUserId, orderId, bundle);
        }
    }
}
