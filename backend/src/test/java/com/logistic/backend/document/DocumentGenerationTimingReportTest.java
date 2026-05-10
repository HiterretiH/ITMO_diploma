package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Measures render+convert timings for the same sequence as {@link DocumentPrefetchService}
 * prefetch (DOCX for all types, then PDF). Writes {@code build/reports/document-generation-timing.txt}.
 *
 * <p>Run: {@code ./gradlew test --tests DocumentGenerationTimingReportTest}
 */
class DocumentGenerationTimingReportTest {

    @Test
    void writesTimingReportForPrefetchOrderedGeneration() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> context = DocumentGenerationService.snapshotToContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        DocxPdfConverter converter = new DocxPdfConverter();

        record Row(String label, long nanos, int bytesOut) {}

        for (int w = 0; w < 2; w++) {
            runSequence(renderer, converter, context);
        }

        List<Row> rows = new ArrayList<>();
        long totalNanos = 0;
        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType dt : DocumentType.values()) {
                long start = System.nanoTime();
                byte[] docx =
                        renderDocxBytes(dt, renderer, DocumentTemplateCache.fileName(dt), context);
                byte[] out = ff == FileFormat.DOCX ? docx : converter.convert(docx);
                long elapsed = System.nanoTime() - start;
                totalNanos += elapsed;

                assertThat(out.length).isGreaterThan(500);

                rows.add(
                        new Row(
                                dt.name() + "_" + ff.name(),
                                elapsed,
                                out.length));
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

    private static void runSequence(
            DocxTemplateRenderer renderer,
            DocxPdfConverter converter,
            Map<String, String> context)
            throws Exception {
        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType dt : DocumentType.values()) {
                byte[] docx =
                        renderDocxBytes(dt, renderer, DocumentTemplateCache.fileName(dt), context);
                if (ff == FileFormat.PDF) {
                    converter.convert(docx);
                }
            }
        }
    }

    private static byte[] renderDocxBytes(
            DocumentType type,
            DocxTemplateRenderer renderer,
            String templateFileName,
            Map<String, String> context)
            throws java.io.IOException {
        try (var in =
                DocumentGenerationTimingReportTest.class
                        .getClassLoader()
                        .getResourceAsStream("templates/documents/" + templateFileName)) {
            assertThat(in).isNotNull();
            return renderer.render(in.readAllBytes(), context);
        }
    }
}
