package com.logistic.backend.document;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

        OrderPrintSnapshot snapshot = sampleSnapshot();
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
                    out = pdfRenderer.render(tpl, type, PdfFormValuesBuilder.values(type, snapshot));
                }
                String ext = ff == FileFormat.DOCX ? ".docx" : ".pdf";
                Files.write(outDir.resolve(type.name().toLowerCase() + "_sample" + ext), out);
            }
        }

        StringBuilder preview = new StringBuilder();
        preview.append("DOCX placeholder context (snapshotToContext):\n");
        preview.append(docxContext).append("\n\nPDF overlay values (PdfFormValuesBuilder):\n");
        for (DocumentType type : DocumentType.values()) {
            preview.append(type.name()).append(" => ").append(PdfFormValuesBuilder.values(type, snapshot)).append('\n');
        }
        Files.writeString(outDir.resolve("context-preview.txt"), preview.toString());
        System.out.println("Generated fixtures in: " + outDir);
    }

    public static OrderPrintSnapshot sampleSnapshot() {
        return new OrderPrintSnapshot(
                1001L,
                42,
                LocalDate.of(2026, 5, 15),
                "ООО Ромашка",
                "ООО Ромашка полное наименование",
                "+78120000000",
                "г. Москва, ул. Ленина, 1",
                "ИП Петров",
                "ИП Петров Петр Петрович",
                "+79001112233",
                "АО Банк",
                "000000000000",
                "044525225",
                "770101001",
                "40702810000000000001",
                "30101810400000000225",
                "г. Москва, ул. Ленина, 1",
                "Volvo FH",
                "А123ВС178",
                "тягач с прицепом",
                "Иванов Иван Иванович",
                "+79002223344",
                "Москва",
                "Контакт погрузки +7 900 123 45 67",
                "Санкт-Петербург",
                "Контакт разгрузки +7 900 765 43 21",
                1,
                new BigDecimal("98500.00"),
                new BigDecimal("98500.00"));
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
