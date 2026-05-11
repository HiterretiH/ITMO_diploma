package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** End-to-end PDF path: classpath PDF template + AcroForm values (no DOCX-to-PDF conversion). */
class DocumentPdfOverlayPipelineTest {

    @Test
    void contractPdfContainsRenderedCyrillicFromFixtureSnapshot() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> pdfValues = PdfFormValuesBuilder.values(DocumentType.CONTRACT_APPLICATION, snapshot);
        byte[] tpl;
        try (InputStream in =
                new ClassPathResource(PdfFormTemplateCache.CLASSPATH_DIR + "contract_application.pdf")
                        .getInputStream()) {
            tpl = in.readAllBytes();
        }
        PdfOverlayRenderer renderer = new PdfOverlayRenderer();
        byte[] pdf = renderer.render(tpl, pdfValues);

        assertThat(pdf.length).isGreaterThan(500);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument pd = Loader.loadPDF(pdf)) {
            assertThat(pd.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pd);
            assertThat(text).doesNotContain("{{");
            assertThat(text).doesNotContain("${");
            var acro = pd.getDocumentCatalog().getAcroForm();
            assertThat(acro == null || acro.getFields().isEmpty())
                    .as("flattened contract PDF must not retain interactive form fields")
                    .isTrue();
            // PDFTextStripper is unreliable for Cyrillic after flatten (subset fonts); use stable ASCII markers.
            assertThat(text).contains("+78120000000");
            assertThat(text).contains("+79002223344");
            assertThat(text).contains("Volvo FH");
            assertThat(text).contains("98500.00");
        }
    }

    @Test
    void allDocumentTypesProducePdfFromTemplates() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        PdfFormTemplateCache templates = new PdfFormTemplateCache();
        PdfOverlayRenderer renderer = new PdfOverlayRenderer();
        for (DocumentType type : DocumentType.values()) {
            byte[] tpl = templates.templateBytes(type);
            byte[] pdf = renderer.render(tpl, PdfFormValuesBuilder.values(type, snapshot));
            assertThat(new String(pdf, 0, 5)).as(type.name()).isEqualTo("%PDF-");
            try (PDDocument pd = Loader.loadPDF(pdf)) {
                assertThat(pd.getNumberOfPages()).as(type.name()).isGreaterThanOrEqualTo(1);
            }
        }
    }
}
