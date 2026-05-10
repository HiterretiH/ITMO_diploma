package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Context keys and money strings should cover the {@code save()} context from legacy
 * {@code .ide/main.py} (docxtpl).
 */
class DocumentSnapshotLegacyMainContextTest {

    private static final Set<String> LEGACY_MAIN_PY_KEYS =
            Set.of(
                    "performer_name",
                    "performer_full_name",
                    "performer_info",
                    "performer_phone",
                    "performer_bank",
                    "performer_vehicle",
                    "performer_vehicle_number",
                    "performer_driver",
                    "performer_driver_phone",
                    "performer_vehicle_type",
                    "performer_inn",
                    "performer_bik",
                    "performer_kpp",
                    "performer_rsh",
                    "performer_ksh",
                    "customer_name",
                    "customer_full_name",
                    "customer_info",
                    "customer_phone",
                    "customer_bank",
                    "number",
                    "date",
                    "contact_loading",
                    "contact_unloading",
                    "count",
                    "price",
                    "total_price",
                    "word_date",
                    "word_price",
                    "loading_place",
                    "unloading_place",
                    "customer_info_ws",
                    "performer_info_ws");

    @Test
    void snapshotContextIncludesAllLegacyMainPyKeys() {
        OrderPrintSnapshot snap = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> ctx = DocumentGenerationService.snapshotToContext(snap);
        assertThat(ctx.keySet()).containsAll(LEGACY_MAIN_PY_KEYS);
    }

    @Test
    void sampleSnapshotMoneyFieldsMatchLegacyFormatting() {
        OrderPrintSnapshot snap = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> ctx = DocumentGenerationService.snapshotToContext(snap);

        assertThat(ctx.get("price")).isEqualTo("98500.00");
        assertThat(ctx.get("total_price")).isEqualTo("98500.00");
        assertThat(ctx.get("word_date")).isEqualTo("15 мая 2026г.");
        assertThat(ctx.get("word_price")).startsWith("Девяносто восемь тысяч пятьсот рублей ");
    }

    @Test
    void sampleUsesWholeRublesOnlyInAssertions() {
        BigDecimal total = DocumentFixtureGenerator.sampleSnapshot().totalPrice();
        assertThat(total.stripTrailingZeros().scale() <= 0).isTrue();
    }
}
