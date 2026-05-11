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

/**
 * Measures render+overlay timings for the same sequence as {@link DocumentPrefetchService} prefetch
 * (DOCX for all types, then PDF). Writes {@code build/reports/document-generation-timing.txt}.
 *
 * <p>Run: {@code ./gradlew test --tests DocumentGenerationTimingReportTest}
 */
class DocumentGenerationTimingReportTest {

    @Test
    void writesTimingReportForPrefetchOrderedGeneration() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> docxContext = DocumentGenerationService.snapshotToContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        PdfFormTemplateCache pdfTemplates = new PdfFormTemplateCache();
        PdfOverlayRenderer pdfRenderer = new PdfOverlayRenderer();

        for (int w = 0; w < 2; w++) {
            runSequence(renderer, pdfRenderer, pdfTemplates, docxContext, snapshot);
        }

        List<Row> rows = new ArrayList<>();
        long totalNanos = 0;
        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType dt : DocumentType.values()) {
                long start = System.nanoTime();
                byte[] out = generateOne(dt, ff, renderer, pdfRenderer, pdfTemplates, docxContext, snapshot);
                long elapsed = System.nanoTime() - start;
                totalNanos += elapsed;

                assertThat(out.length).isGreaterThan(500);

                rows.add(new Row(dt.name() + "_" + ff.name(), elapsed, out.length));
            }
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append("Document generation timing (nanoseconds), sampleSnapshot + production templates\n");
        sb.append("(same order as DocumentPrefetchService: DOCX all types, then PDF all types)\n\n");
        sb.append(String.format("%-40s %18s %12s%n", "step", "nanos", "bytes_out"));
        sb.append("-".repeat(74)).append('\n');
        for (Row r : rows) {
            sb.append(String.format("%-40s %18d %12d%n", r.label, r.nanos, r.bytesOut));
        }
        sb.append("-".repeat(74)).append('\n');
        sb.append(String.format("%-40s %18d%n", "TOTAL", totalNanos));
        sb.append(String.format("%-40s %18.3f ms%n", "TOTAL (ms)", totalNanos / 1_000_000.0));

        Path out = Path.of("build", "reports", "document-generation-timing.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);

        assertThat(out).exists();
        assertThat(totalNanos).isPositive();
    }

    private record Row(String label, long nanos, int bytesOut) {}

    private static void runSequence(
            DocxTemplateRenderer renderer,
            PdfOverlayRenderer pdfRenderer,
            PdfFormTemplateCache pdfTemplates,
            Map<String, String> docxContext,
            OrderPrintSnapshot snapshot)
            throws Exception {
        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType dt : DocumentType.values()) {
                generateOne(dt, ff, renderer, pdfRenderer, pdfTemplates, docxContext, snapshot);
            }
        }
    }

    private static byte[] generateOne(
            DocumentType type,
            FileFormat ff,
            DocxTemplateRenderer renderer,
            PdfOverlayRenderer pdfRenderer,
            PdfFormTemplateCache pdfTemplates,
            Map<String, String> docxContext,
            OrderPrintSnapshot snapshot)
            throws Exception {
        if (ff == FileFormat.DOCX) {
            return renderDocxBytes(type, renderer, docxContext);
        }
        byte[] tpl = pdfTemplates.templateBytes(type);
        return pdfRenderer.render(tpl, type, PdfFormValuesBuilder.values(type, snapshot));
    }

    private static byte[] renderDocxBytes(
            DocumentType type, DocxTemplateRenderer renderer, Map<String, String> context) throws Exception {
        String templateFileName = DocumentTemplateCache.fileName(type);
        try (InputStream in =
                DocumentGenerationTimingReportTest.class
                        .getClassLoader()
                        .getResourceAsStream("templates/documents/" + templateFileName)) {
            assertThat(in).isNotNull();
            return renderer.render(in.readAllBytes(), context);
        }
    }
}
