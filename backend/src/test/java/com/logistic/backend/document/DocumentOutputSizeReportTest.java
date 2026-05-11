package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Writes rendered DOCX / PDF byte sizes for production templates (fixture snapshot) to {@code
 * build/reports/document-output-sizes.txt}.
 *
 * <p>Run: {@code ./gradlew test --tests DocumentOutputSizeReportTest}
 */
class DocumentOutputSizeReportTest {

    @Test
    void printsRenderedDocxAndPdfSizesForAllTemplates() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> docxContext = DocumentGenerationService.snapshotToContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        PdfFormTemplateCache pdfTemplates = new PdfFormTemplateCache();
        PdfOverlayRenderer pdfRenderer = new PdfOverlayRenderer();

        record Row(String name, int templateDocxBytes, int docxBytes, int templatePdfBytes, int pdfBytes) {}

        List<Row> rows = new ArrayList<>();

        for (DocumentType type : DocumentType.values()) {
            String docxName = DocumentTemplateCache.fileName(type);
            String pdfName = PdfFormTemplateCache.fileName(type);
            byte[] templateDocx;
            try (InputStream in = new ClassPathResource("templates/documents/" + docxName).getInputStream()) {
                templateDocx = in.readAllBytes();
            }
            byte[] docx = renderer.render(templateDocx, docxContext);

            byte[] templatePdf = pdfTemplates.templateBytes(type);
            byte[] pdf =
                    pdfRenderer.render(
                            templatePdf, type, PdfFormValuesBuilder.values(type, snapshot));

            assertThat(docx.length).as("%s rendered DOCX", docxName).isGreaterThan(500);
            assertThat(pdf.length).as("%s PDF", pdfName).isGreaterThan(800);

            rows.add(new Row(docxName, templateDocx.length, docx.length, templatePdf.length, pdf.length));
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append("Document output sizes (bytes), DocumentFixtureGenerator.sampleSnapshot()\n\n");
        sb.append(String.format("%-32s %10s %10s %10s %10s %12s%n", "template", "docx tpl", "docx out", "pdf tpl", "pdf out", "peak≈sum"));
        sb.append("-".repeat(88)).append('\n');
        int totalPeak = 0;
        for (Row r : rows) {
            int peak = r.templateDocxBytes + r.docxBytes + r.templatePdfBytes + r.pdfBytes;
            totalPeak += peak;
            sb.append(
                    String.format(
                            "%-32s %10d %10d %10d %10d %12d%n",
                            r.name, r.templateDocxBytes, r.docxBytes, r.templatePdfBytes, r.pdfBytes, peak));
        }
        sb.append("-".repeat(88)).append('\n');
        sb.append(String.format("%-32s %10s %10s %10s %10s %12d%n", "TOTAL (peak sum)", "", "", "", "", totalPeak));
        sb.append('\n');
        sb.append(
                """
                Analysis:
                - Rendered DOCX stays close to template size (merge fields replace placeholders).
                - PDF output overlays values on flat template PDFs (no docx4j conversion).
                - Peak≈sum approximates byte[] footprint if all buffers coexist for one document type.
                - totalPeak is a sanity sum for this fixture; kept under 768 KiB in assertions.
                """);

        Path out = Path.of("build", "reports", "document-output-sizes.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);

        assertThat(totalPeak).isLessThan(768 * 1024);
        assertThat(out).exists();
    }
}
