package com.logistic.backend.document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public final class DocumentFixtureGenerator {

    private DocumentFixtureGenerator() {}

    public static void main(String[] args) throws Exception {
        Path outDir =
                args.length > 0
                        ? Path.of(args[0]).toAbsolutePath()
                        : Path.of("manual-review-docs").toAbsolutePath();
        Files.createDirectories(outDir);

        OrderPrintSnapshot snapshot = OrderPrintSnapshots.manualReviewDemo();
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        PdfFormTemplateCache pdfTemplates = new PdfFormTemplateCache();
        PdfOverlayRenderer pdfRenderer = new PdfOverlayRenderer();
        Map<String, String> docxContext = DocumentGenerationService.snapshotToContext(snapshot);

        for (FileFormat ff : DocumentPrefetchService.PREFETCH_FORMAT_ORDER) {
            for (DocumentType type : DocumentType.values()) {
                byte[] out;
                if (ff == FileFormat.DOCX) {
                    out = renderDocx(type, renderer, docxContext);
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
            DocumentType type, DocxTemplateRenderer renderer, Map<String, String> context)
            throws IOException {
        String templateName =
                switch (type) {
                    case CONTRACT_APPLICATION -> "contract_application.docx";
                    case WAYBILL -> "waybill.docx";
                    case ACT_OF_WORK -> "act_of_work.docx";
                };
        String resourcePath = "templates/documents/" + templateName;
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            return renderer.render(in.readAllBytes(), context);
        }
    }
}
