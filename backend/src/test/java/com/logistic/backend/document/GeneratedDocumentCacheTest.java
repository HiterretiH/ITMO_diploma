package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GeneratedDocumentCacheTest {

    @Test
    void getReturnsPutBytesPerOwnerAndOrder() {
        LogisticDocumentCacheProperties props = new LogisticDocumentCacheProperties();
        props.setTtl(Duration.ofHours(1));
        props.setMaxOrderSetsPerOwner(10);
        GeneratedDocumentCache cache = new GeneratedDocumentCache(props);

        byte[] pdf = new byte[] {1, 2, 3};
        cache.put(100L, 1L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF, pdf);

        assertThat(cache.get(100L, 1L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF))
                .contains(pdf);
        assertThat(cache.get(100L, 1L, DocumentType.CONTRACT_APPLICATION, FileFormat.DOCX))
                .isEmpty();
    }

    @Test
    void sixthOrderEvictsEarlierEntryWhenMaxFivePerOwner() {
        LogisticDocumentCacheProperties props = new LogisticDocumentCacheProperties();
        props.setTtl(Duration.ofHours(1));
        props.setMaxOrderSetsPerOwner(5);
        GeneratedDocumentCache cache = new GeneratedDocumentCache(props);
        long owner = 7L;
        for (long orderId = 1; orderId <= 5; orderId++) {
            cache.put(
                    owner,
                    orderId,
                    DocumentType.CONTRACT_APPLICATION,
                    FileFormat.PDF,
                    new byte[] {(byte) orderId});
        }
        cache.put(owner, 6L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF, new byte[] {6});

        boolean stillHasOrder1 =
                cache.get(owner, 1L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF)
                        .isPresent();
        boolean hasOrder6 =
                cache.get(owner, 6L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF)
                        .isPresent();
        assertThat(hasOrder6).isTrue();
        assertThat(stillHasOrder1).isFalse();
    }

    @Test
    void entryExpiresAfterTtlFromLastWrite() {
        AtomicReference<Instant> now =
                new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        Clock clock =
                new Clock() {
                    @Override
                    public java.time.ZoneId getZone() {
                        return ZoneOffset.UTC;
                    }

                    @Override
                    public Clock withZone(java.time.ZoneId zone) {
                        return this;
                    }

                    @Override
                    public Instant instant() {
                        return now.get();
                    }
                };
        LogisticDocumentCacheProperties props = new LogisticDocumentCacheProperties();
        props.setTtl(Duration.ofMinutes(10));
        props.setMaxOrderSetsPerOwner(5);
        GeneratedDocumentCache cache = new GeneratedDocumentCache(props, clock);
        cache.put(1L, 1L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF, new byte[] {1});
        assertThat(cache.get(1L, 1L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF))
                .isPresent();
        now.set(now.get().plus(11, ChronoUnit.MINUTES));
        assertThat(cache.get(1L, 1L, DocumentType.CONTRACT_APPLICATION, FileFormat.PDF))
                .isEmpty();
    }

    @Test
    void invalidateRemovesOrderBundle() {
        LogisticDocumentCacheProperties props = new LogisticDocumentCacheProperties();
        props.setTtl(Duration.ofHours(1));
        props.setMaxOrderSetsPerOwner(5);
        GeneratedDocumentCache cache = new GeneratedDocumentCache(props);
        cache.put(3L, 9L, DocumentType.WAYBILL, FileFormat.DOCX, new byte[] {9});
        cache.invalidate(3L, 9L);
        assertThat(cache.get(3L, 9L, DocumentType.WAYBILL, FileFormat.DOCX)).isEmpty();
    }
}
