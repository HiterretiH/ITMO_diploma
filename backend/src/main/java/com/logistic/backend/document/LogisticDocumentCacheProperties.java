package com.logistic.backend.document;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.documents.cache")
public class LogisticDocumentCacheProperties {

    /** How long a cached set of document binaries for one order is kept after last write. */
    private Duration ttl = Duration.ofMinutes(10);

    /** Max number of orders whose generated documents are cached simultaneously per owner user id. */
    private int maxOrderSetsPerOwner = 5;

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getMaxOrderSetsPerOwner() {
        return maxOrderSetsPerOwner;
    }

    public void setMaxOrderSetsPerOwner(int maxOrderSetsPerOwner) {
        this.maxOrderSetsPerOwner = maxOrderSetsPerOwner;
    }
}
