package com.logistic.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoutePlaceKeysTest {

    @Test
    void normalizeForKey_collapsesWhitespaceAndLowercases() {
        assertThat(RoutePlaceKeys.normalizeForKey("  A  B\tc  ")).isEqualTo("a b c");
    }

    @Test
    void placeKeySha256_sameNormalized_sameKey() {
        Optional<String> k1 = RoutePlaceKeys.placeKeySha256("Moscow  Street");
        Optional<String> k2 = RoutePlaceKeys.placeKeySha256("  moscow   street  ");
        assertThat(k1).isPresent();
        assertThat(k1).isEqualTo(k2);
        assertThat(k1.get()).hasSize(64);
    }

    @Test
    void placeKeySha256_blank_empty() {
        assertThat(RoutePlaceKeys.placeKeySha256("   ")).isEmpty();
    }
}
