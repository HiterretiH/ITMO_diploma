package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Measures render+overlay timings for the same sequence as {@link DocumentPrefetchService} prefetch
 * (DOCX for all types, then PDF). Templates are preloaded via {@link DocumentTemplateCache} and
 * {@link PdfFormTemplateCache} so the timed window excludes classpath I/O (matches production).
 * Writes {@code build/reports/document-generation-timing.txt} and
 * {@code docs/document-generation-timing-baseline.txt} (numbers vary by machine).
 *
 * <p>Run: {@code ./gradlew test --tests DocumentGenerationTimingReportTest}
 */
class DocumentGenerationTimingReportTest {

    @Test
    void writesTimingReportForPrefetchOrderedGeneration() throws Exception {
        OrderPrintSnapshot snapshot = OrderPrintSnapshots.manualReviewDemo();
        Map<String, String> docxContext = DocumentGenerationService.snapshotToContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        DocumentTemplateCache docxTemplates = new DocumentTemplateCache();
        PdfFormTemplateCache pdfTemplates = new PdfFormTemplateCache();
        PdfOverlayRenderer pdfRenderer = new PdfOverlayRenderer();

        for (int w = 0; w < 2; w++) {
            runSequence(renderer, pdfRenderer, docxTemplates, pdfTemplates, docxContext, snapshot);
        }

        List<Row> rows = new ArrayList<>();
        long totalNanos = 0;
        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType dt : DocumentType.values()) {
                long start = System.nanoTime();
                byte[] out =
                        generateOne(dt, ff, renderer, pdfRenderer, docxTemplates, pdfTemplates, docxContext, snapshot);
                long elapsed = System.nanoTime() - start;
                totalNanos += elapsed;

                assertThat(out.length).isGreaterThan(500);

                rows.add(new Row(dt.name() + "_" + ff.name(), elapsed, out.length));
            }
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append(
                "Document generation timing (nanoseconds), OrderPrintSnapshots.manualReviewDemo; "
                        + "templates preloaded (timed = render/overlay only)\n");
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

        StringBuilder meta = new StringBuilder(512);
        meta.append("# Document generation timing baseline\n");
        meta.append("# Machine-dependent; refreshed when DocumentGenerationTimingReportTest runs.\n");
        meta.append("captured_utc=").append(Instant.now()).append('\n');
        meta.append("java.version=").append(System.getProperty("java.version")).append('\n');
        meta.append("java.vm.name=").append(System.getProperty("java.vm.name")).append('\n');
        meta.append("os.name=").append(System.getProperty("os.name")).append('\n');
        meta.append('\n');
        Path baseline = Path.of("docs", "document-generation-timing-baseline.txt");
        Files.createDirectories(baseline.getParent());
        Files.writeString(baseline, meta + sb.toString(), StandardCharsets.UTF_8);

        assertThat(out).exists();
        assertThat(baseline).exists();
        assertThat(totalNanos).isPositive();
    }

    private record Row(String label, long nanos, int bytesOut) {}

    private static void runSequence(
            DocxTemplateRenderer renderer,
            PdfOverlayRenderer pdfRenderer,
            DocumentTemplateCache docxTemplates,
            PdfFormTemplateCache pdfTemplates,
            Map<String, String> docxContext,
            OrderPrintSnapshot snapshot)
            throws Exception {
        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType dt : DocumentType.values()) {
                generateOne(dt, ff, renderer, pdfRenderer, docxTemplates, pdfTemplates, docxContext, snapshot);
            }
        }
    }

    private static byte[] generateOne(
            DocumentType type,
            FileFormat ff,
            DocxTemplateRenderer renderer,
            PdfOverlayRenderer pdfRenderer,
            DocumentTemplateCache docxTemplates,
            PdfFormTemplateCache pdfTemplates,
            Map<String, String> docxContext,
            OrderPrintSnapshot snapshot)
            throws Exception {
        if (ff == FileFormat.DOCX) {
            byte[] tpl = docxTemplates.templateBytes(type);
            assertThat(tpl).isNotNull().isNotEmpty();
            return renderer.render(tpl, docxContext);
        }
        byte[] tpl = pdfTemplates.templateBytes(type);
        return pdfRenderer.render(tpl, PdfFormValuesBuilder.values(type, snapshot));
    }
}
