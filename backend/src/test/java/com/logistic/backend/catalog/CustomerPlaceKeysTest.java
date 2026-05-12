package com.logistic.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustomerPlaceKeysTest {

    @Test
    void normalizeForKey_collapsesWhitespaceAndLowercases() {
        assertThat(CustomerPlaceKeys.normalizeForKey("  A  B\tc  ")).isEqualTo("a b c");
    }

    @Test
    void addressKeySha256_sameNormalized_sameKey() {
        Optional<String> k1 = CustomerPlaceKeys.addressKeySha256("Moscow  Street");
        Optional<String> k2 = CustomerPlaceKeys.addressKeySha256("  moscow   street  ");
        assertThat(k1).isPresent();
        assertThat(k1).isEqualTo(k2);
        assertThat(k1.get()).hasSize(64);
    }

    @Test
    void addressKeySha256_blank_empty() {
        assertThat(CustomerPlaceKeys.addressKeySha256("   ")).isEmpty();
    }
}
