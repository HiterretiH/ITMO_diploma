package com.logistic.backend.document;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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

        TripPrintSnapshot snapshot =
                new TripPrintSnapshot(
                        1001L,
                        "ip_petrov",
                        "ООО Ромашка",
                        "7701234567",
                        "г. Москва, ул. Ленина, 1",
                        "ООО Василек",
                        "7810123456",
                        "г. Санкт-Петербург, Невский пр., 10",
                        "Строительные смеси, 24 паллеты",
                        new BigDecimal("12000.500"),
                        "Москва",
                        "Санкт-Петербург",
                        LocalDate.of(2026, 5, 15),
                        LocalDate.of(2026, 5, 16),
                        "Иванов Иван Иванович",
                        "77 01 123456",
                        "А123ВС178",
                        "Volvo FH",
                        20000,
                        new BigDecimal("98500.00"),
                        "RUB");

        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        Map<String, String> context = snapshotToContext(snapshot);

        for (DocumentType type : DocumentType.values()) {
            byte[] docx = renderDocx(type, renderer, context);
            byte[] pdf = renderPdf(renderer.extractText(docx));
            Files.write(outDir.resolve(type.name().toLowerCase() + "_sample.docx"), docx);
            Files.write(outDir.resolve(type.name().toLowerCase() + "_sample.pdf"), pdf);
        }

        Files.writeString(outDir.resolve("context-preview.txt"), context.toString());
        System.out.println("Generated fixtures in: " + outDir);
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

    private static byte[] renderPdf(String text) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, baos);
        doc.open();
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                doc.add(new Paragraph(line));
            }
        }
        doc.close();
        return baos.toByteArray();
    }

    private static Map<String, String> snapshotToContext(TripPrintSnapshot s) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("tripId", asString(s.tripId()));
        ctx.put("ownerUsername", asString(s.ownerUsername()));
        ctx.put("shipperName", asString(s.shipperName()));
        ctx.put("shipperInn", asString(s.shipperInn()));
        ctx.put("shipperAddress", asString(s.shipperAddress()));
        ctx.put("consigneeName", asString(s.consigneeName()));
        ctx.put("consigneeInn", asString(s.consigneeInn()));
        ctx.put("consigneeAddress", asString(s.consigneeAddress()));
        ctx.put("cargoDescription", asString(s.cargoDescription()));
        ctx.put("cargoWeightKg", asString(s.cargoWeightKg()));
        ctx.put("routeFrom", asString(s.routeFrom()));
        ctx.put("routeTo", asString(s.routeTo()));
        ctx.put("loadDate", asString(s.loadDate()));
        ctx.put("unloadDate", asString(s.unloadDate()));
        ctx.put("driverName", asString(s.driverName()));
        ctx.put("driverLicense", asString(s.driverLicense()));
        ctx.put("vehiclePlate", asString(s.vehiclePlate()));
        ctx.put("vehicleModel", asString(s.vehicleModel()));
        ctx.put("vehicleCapacityKg", asString(s.vehicleCapacityKg()));
        ctx.put("priceAmount", asString(s.priceAmount()));
        ctx.put("currency", asString(s.currency()));
        return ctx;
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }
}
