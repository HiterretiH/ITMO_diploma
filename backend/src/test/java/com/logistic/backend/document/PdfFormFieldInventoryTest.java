package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Ensures classpath PDF templates load and are valid single-page documents used for AcroForm fill.
 * Field names follow {@code utilities/docx_to_pdf_template/config.py} and {@link PdfFormValuesBuilder}.
 */
class PdfFormFieldInventoryTest {

    @Test
    void pdfFormTemplateCacheLoadsAllTemplates() {
        PdfFormTemplateCache cache = new PdfFormTemplateCache();
        for (DocumentType t : DocumentType.values()) {
            assertThat(cache.templateBytes(t))
                    .as(t.name())
                    .isNotNull()
                    .startsWith("%PDF".getBytes());
        }
    }

    @Test
    void eachTemplatePdfIsSinglePage() throws Exception {
        for (DocumentType t : DocumentType.values()) {
            String pdf = PdfFormTemplateCache.fileName(t);
            try (PDDocument doc =
                    Loader.loadPDF(
                            new ClassPathResource(PdfFormTemplateCache.CLASSPATH_DIR + pdf)
                                    .getContentAsByteArray())) {
                assertThat(doc.getNumberOfPages()).as(t.name()).isEqualTo(1);
            }
        }
    }
}
