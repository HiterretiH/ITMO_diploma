package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Every layout slot must have a non-blank value for the canonical fixture snapshot. */
class PdfFormLayoutCoverageTest {

    @Test
    void allSlotsPopulatedForSampleSnapshot() {
        OrderPrintSnapshot snap = DocumentFixtureGenerator.sampleSnapshot();
        for (DocumentType t : DocumentType.values()) {
            var values = PdfFormValuesBuilder.values(t, snap);
            for (PdfFormLayout.Slot slot : PdfFormLayout.slots(t)) {
                assertThat(values)
                        .as("%s missing key", slot.fieldKey())
                        .containsKey(slot.fieldKey());
                assertThat(values.get(slot.fieldKey()))
                        .as("%s %s", t, slot.fieldKey())
                        .isNotBlank();
            }
        }
    }
}
