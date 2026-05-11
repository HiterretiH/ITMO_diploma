package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Classpath PDF templates must expose an AcroForm widget for every {@link PdfFormValuesBuilder}
 * key (matching {@code utilities/docx_to_pdf_template/config.py} {@code field_name}).
 */
class PdfAcroFormTemplateKeysTest {

    @Test
    void eachTemplatePdfDefinesAcroFieldsForAllValueKeys() throws Exception {
        OrderPrintSnapshot snap = DocumentFixtureGenerator.sampleSnapshot();
        PdfFormTemplateCache cache = new PdfFormTemplateCache();
        for (DocumentType t : DocumentType.values()) {
            Map<String, String> values = PdfFormValuesBuilder.values(t, snap);
            PdfReader reader = new PdfReader(cache.templateBytes(t));
            try {
                AcroFields af = reader.getAcroFields();
                assertThat(af).as(t.name()).isNotNull();
                @SuppressWarnings("unchecked")
                Map<String, ?> fields = (Map<String, ?>) af.getAllFields();
                assertThat(fields).as("%s AcroForm fields", t).isNotEmpty();
                for (String key : values.keySet()) {
                    assertThat(fields).as("%s missing AcroForm field %s", t, key).containsKey(key);
                }
            } finally {
                reader.close();
            }
        }
    }
}
