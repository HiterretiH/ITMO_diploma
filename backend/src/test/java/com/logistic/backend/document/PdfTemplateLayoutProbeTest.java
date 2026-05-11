package com.logistic.backend.document;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Dumps glyph bounding boxes from flat template PDFs into {@code build/reports/} for calibrating
 * {@link PdfFormLayout}. Run: {@code ./gradlew test --tests PdfTemplateLayoutProbeTest}
 */
class PdfTemplateLayoutProbeTest {

    @Test
    void dumpTemplateTextLayoutReports() throws Exception {
        dumpOne("waybill.pdf", "pdf-waybill-template-layout.txt");
        dumpOne("contract_application.pdf", "pdf-contract-template-layout.txt");
        dumpOne("act_of_work.pdf", "pdf-act-template-layout.txt");
    }

    private static void dumpOne(String classpathPdf, String outName) throws IOException {
        Path out = Path.of("build", "reports", outName);
        Files.createDirectories(out.getParent());
        byte[] bytes;
        try (var in =
                new ClassPathResource(PdfFormTemplateCache.CLASSPATH_DIR + classpathPdf).getInputStream()) {
            bytes = in.readAllBytes();
        }
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PositionStripper stripper = new PositionStripper();
            stripper.setSortByPosition(true);
            stripper.getText(doc);
            Files.writeString(out, stripper.result(), StandardCharsets.UTF_8);
        }
    }

    private static final class PositionStripper extends PDFTextStripper {
        private final List<Line> lines = new ArrayList<>();
        private final StringWriter plain = new StringWriter();

        PositionStripper() throws IOException {
            super.setShouldSeparateByBeads(false);
        }

        String result() {
            StringBuilder sb = new StringBuilder();
            sb.append("PDF text lines (approx), sorted by Y desc then X asc. User space: origin bottom-left.\n\n");
            lines.sort(
                    Comparator.<Line>comparingDouble((Line l) -> l.yMax).reversed()
                            .thenComparingDouble(l -> l.xMin));
            for (Line l : lines) {
                sb.append(String.format("y=[%.1f..%.1f] x=[%.1f..%.1f] %s%n", l.yMin, l.yMax, l.xMin, l.xMax, l.text));
            }
            sb.append("\n--- getText() ---\n");
            sb.append(plain.toString());
            return sb.toString();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            plain.append(text);
            if (textPositions.isEmpty()) {
                return;
            }
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            StringBuilder sb = new StringBuilder();
            for (TextPosition tp : textPositions) {
                minX = Math.min(minX, tp.getXDirAdj());
                maxX = Math.max(maxX, tp.getXDirAdj() + tp.getWidthDirAdj());
                minY = Math.min(minY, tp.getYDirAdj());
                maxY = Math.max(maxY, tp.getYDirAdj() + tp.getHeightDir());
                sb.append(tp.getUnicode());
            }
            String t = sb.toString().replace('\r', ' ').trim();
            if (!t.isBlank()) {
                lines.add(new Line(minX, maxX, minY, maxY, t));
            }
        }
    }

    private record Line(double xMin, double xMax, double yMin, double yMax, String text) {}
}
