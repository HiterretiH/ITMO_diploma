package com.logistic.backend.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class DocumentFixtureGenerator {

    private DocumentFixtureGenerator() {}

    public static void main(String[] args) throws Exception {
        Path outDir =
                args.length > 0
                        ? Path.of(args[0]).toAbsolutePath()
                        : Path.of("manual-review-docs").toAbsolutePath();
        Files.createDirectories(outDir);

        OrderPrintSnapshot snapshot = OrderPrintSnapshots.manualReviewDemo();
        DocumentTemplateCache docxTemplates = new DocumentTemplateCache();
        PdfFormTemplateCache pdfTemplates = new PdfFormTemplateCache();
        PdfOverlayRenderer pdfRenderer = new PdfOverlayRenderer();
        Map<String, String> docxContext = DocumentGenerationService.snapshotToContext(snapshot);

        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType type : DocumentType.values()) {
                byte[] out;
                if (ff == FileFormat.DOCX) {
                    out = renderDocx(type, docxTemplates, docxContext);
                } else {
                    byte[] tpl = pdfTemplates.templateBytes(type);
                    out = pdfRenderer.render(tpl, PdfFormValuesBuilder.values(type, snapshot));
                }
                String ext = ff == FileFormat.DOCX ? ".docx" : ".pdf";
                Files.write(outDir.resolve(type.name().toLowerCase() + "_sample" + ext), out);
            }
        }

        StringBuilder preview = new StringBuilder();
        preview.append("DOCX placeholder context (snapshotToContext):\n");
        preview.append(docxContext).append("\n\nPDF AcroForm values (PdfFormValuesBuilder):\n");
        for (DocumentType type : DocumentType.values()) {
            preview.append(type.name()).append(" => ").append(PdfFormValuesBuilder.values(type, snapshot)).append('\n');
        }
        Files.writeString(outDir.resolve("context-preview.txt"), preview.toString());
        System.out.println("Generated fixtures in: " + outDir);
    }

    /** Same as {@link OrderPrintSnapshots#manualReviewDemo()} (kept for existing tests and call sites). */
    public static OrderPrintSnapshot sampleSnapshot() {
        return OrderPrintSnapshots.manualReviewDemo();
    }

    private static byte[] renderDocx(
            DocumentType type, DocumentTemplateCache docxTemplates, Map<String, String> context)
            throws IOException {
        return docxTemplates.compiledTemplate(type).render(context);
    }
}
