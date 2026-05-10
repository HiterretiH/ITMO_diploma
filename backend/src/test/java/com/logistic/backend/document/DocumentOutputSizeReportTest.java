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
 * build/reports/document-output-sizes.txt} so stdout is not flooded by docx4j logs.
 *
 * <p>Run: {@code ./gradlew test --tests DocumentOutputSizeReportTest}
 *
 * <p><b>How to read the report:</b> template bytes are the classpath .docx; rendered DOCX is
 * similar size after merge fields; PDF is much larger than DOCX here (embedded fonts / PDF
 * structure). Peak heap for one PDF download ≈ rendered DOCX + PDF while {@link
 * DocxPdfConverter#convert} holds both arrays.
 */
class DocumentOutputSizeReportTest {

    @Test
    void printsRenderedDocxAndPdfSizesForAllTemplates() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> context = DocumentGenerationService.snapshotToContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        DocxPdfConverter converter = new DocxPdfConverter();

        record Row(String name, int templateBytes, int docxBytes, int pdfBytes) {}

        List<Row> rows = new ArrayList<>();

        for (String templatePath :
                List.of(
                        "templates/documents/contract_application.docx",
                        "templates/documents/waybill.docx",
                        "templates/documents/act_of_work.docx")) {
            byte[] templateBytes;
            try (InputStream in = new ClassPathResource(templatePath).getInputStream()) {
                templateBytes = in.readAllBytes();
            }
            byte[] docx = renderer.render(templateBytes, context);
            byte[] pdf = converter.convert(docx);

            assertThat(docx.length).as("%s rendered DOCX", templatePath).isGreaterThan(500);
            assertThat(pdf.length).as("%s PDF", templatePath).isGreaterThan(800);

            String shortName = templatePath.substring(templatePath.lastIndexOf('/') + 1);
            rows.add(new Row(shortName, templateBytes.length, docx.length, pdf.length));
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append("Document output sizes (bytes), DocumentFixtureGenerator.sampleSnapshot()\n\n");
        sb.append(String.format("%-32s %12s %12s %12s %12s%n", "template", "template", "rendered", "pdf", "peak≈docx+pdf"));
        sb.append("-".repeat(84)).append('\n');
        int totalPeak = 0;
        for (Row r : rows) {
            int peak = r.docxBytes + r.pdfBytes;
            totalPeak += peak;
            sb.append(String.format("%-32s %12d %12d %12d %12d%n", r.name, r.templateBytes, r.docxBytes, r.pdfBytes, peak));
        }
        sb.append("-".repeat(84)).append('\n');
        sb.append(String.format("%-32s %12s %12s %12s %12d%n", "TOTAL (3 docs, peak sum)", "", "", "", totalPeak));
        sb.append('\n');
        sb.append(
                """
                Analysis:
                - Rendered DOCX stays close to template size (merge fields replace placeholders).
                - PDF is several times larger than rendered DOCX (embedded fonts + PDF structure).
                - Peak≈docx+pdf approximates max byte[] footprint during one PDF conversion.
                - Three sequential PDF downloads peak per request; they do not allocate totalPeak at once.
                - totalPeak is a sanity sum for this fixture; kept under 512 KiB in assertions.
                """);

        Path out = Path.of("build", "reports", "document-output-sizes.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);

        assertThat(totalPeak).isLessThan(512 * 1024);
        assertThat(out).exists();
    }
}
