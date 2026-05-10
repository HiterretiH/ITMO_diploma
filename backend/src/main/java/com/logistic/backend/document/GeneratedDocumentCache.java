package com.logistic.backend.document;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * In-memory document binaries keyed by order owner user id. At most {@link
 * LogisticDocumentCacheProperties#getMaxOrderSetsPerOwner()} orders per owner (LRU). Each set
 * expires {@link LogisticDocumentCacheProperties#getTtl()} after last write. No disk.
 */
@Service
public class GeneratedDocumentCache {

    private final LogisticDocumentCacheProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<Long, LinkedHashMap<Long, CachedBundle>> cacheByOwner =
            new ConcurrentHashMap<>();

    @Autowired
    public GeneratedDocumentCache(LogisticDocumentCacheProperties properties) {
        this(properties, Clock.systemUTC());
    }

    GeneratedDocumentCache(LogisticDocumentCacheProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public static String compositeKey(DocumentType type, FileFormat format) {
        return type.name() + '|' + format.name();
    }

    /** Access-order LRU: evict eldest when size exceeds max. */
    private LinkedHashMap<Long, CachedBundle> ordersForOwner(long ownerUserId) {
        int max = Math.max(1, properties.getMaxOrderSetsPerOwner());
        return cacheByOwner.computeIfAbsent(
                ownerUserId,
                id ->
                        new LinkedHashMap<>(16, 0.75f, true) {
                            @Override
                            protected boolean removeEldestEntry(Map.Entry<Long, CachedBundle> eldest) {
                                return size() > max;
                            }
                        });
    }

    private boolean isExpired(CachedBundle b) {
        return b.writtenAt().plus(properties.getTtl()).isBefore(clock.instant());
    }

    public Optional<byte[]> get(
            long ownerUserId, long orderId, DocumentType type, FileFormat format) {
        LinkedHashMap<Long, CachedBundle> orders = ordersForOwner(ownerUserId);
        synchronized (orders) {
            CachedBundle b = orders.get(orderId);
            if (b == null) {
                return Optional.empty();
            }
            if (isExpired(b)) {
                orders.remove(orderId);
                return Optional.empty();
            }
            Map<String, byte[]> data = b.bytes();
            byte[] arr = data.get(compositeKey(type, format));
            return Optional.ofNullable(arr);
        }
    }

    public void put(
            long ownerUserId,
            long orderId,
            DocumentType type,
            FileFormat format,
            byte[] bytes) {
        LinkedHashMap<Long, CachedBundle> orders = ordersForOwner(ownerUserId);
        synchronized (orders) {
            CachedBundle existing = orders.get(orderId);
            ConcurrentHashMap<String, byte[]> map;
            if (existing != null && !isExpired(existing)) {
                map = existing.bytes();
            } else {
                map = new ConcurrentHashMap<>();
            }
            map.put(compositeKey(type, format), bytes);
            orders.put(orderId, new CachedBundle(clock.instant(), map));
        }
    }

    public void putBundle(long ownerUserId, long orderId, Map<String, byte[]> bundle) {
        LinkedHashMap<Long, CachedBundle> orders = ordersForOwner(ownerUserId);
        synchronized (orders) {
            orders.put(orderId, new CachedBundle(clock.instant(), new ConcurrentHashMap<>(bundle)));
        }
    }

    public void invalidate(long ownerUserId, long orderId) {
        LinkedHashMap<Long, CachedBundle> orders = cacheByOwner.get(ownerUserId);
        if (orders != null) {
            synchronized (orders) {
                orders.remove(orderId);
            }
        }
    }

    private record CachedBundle(Instant writtenAt, ConcurrentHashMap<String, byte[]> bytes) {}
}
