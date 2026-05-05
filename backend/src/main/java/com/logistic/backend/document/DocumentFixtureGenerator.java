package com.logistic.backend.document;

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
        DocxPdfConverter pdfConverter = new DocxPdfConverter();
        Map<String, String> context = snapshotToContext(snapshot);

        for (DocumentType type : DocumentType.values()) {
            byte[] docx = renderDocx(type, renderer, context);
            byte[] pdf = pdfConverter.convert(docx);
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

    private static Map<String, String> snapshotToContext(TripPrintSnapshot s) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("number", asString(s.tripId()));
        ctx.put("date", asString(s.loadDate()));
        ctx.put("word_date", "15 мая 2026г.");
        ctx.put("loading_place", asString(s.routeFrom()));
        ctx.put("unloading_place", asString(s.routeTo()));
        ctx.put("contact_loading", "Контакт Погрузки +7 900 123 45 67");
        ctx.put("contact_unloading", "Контакт Разгрузки +7 900 765 43 21");
        ctx.put("count", "1");
        ctx.put("price", "98500.00");
        ctx.put("total_price", "98500.00");
        ctx.put("word_price", "Девяносто восемь тысяч пятьсот рублей");
        ctx.put(
                "performer_info_ws",
                "ИП Петров П.П., ИНН 000000000000, БИК 044525225, р/с 40702810000000000001, к/с 30101810400000000225, г. Москва, ул. Ленина, 1");
        ctx.put(
                "customer_info_ws",
                "ООО Ромашка, ИНН 7701234567, Банк клиента, +7 812 000 00 00, г. Москва, ул. Ленина, 1");

        ctx.put("performer_name", "ИП Петров П.П.");
        ctx.put("performer_full_name", "ИП Петров Петр Петрович");
        ctx.put("performer_info", "ИП Петров Петр Петрович, ИНН 000000000000, БИК 044525225, р/с 40702810000000000001, к/с 30101810400000000225, г. Москва, ул. Ленина, 1");
        ctx.put("performer_phone", "+7 900 111 22 33");
        ctx.put("performer_bank", "АО Банк");
        ctx.put("performer_vehicle", asString(s.vehicleModel()));
        ctx.put("performer_vehicle_number", asString(s.vehiclePlate()));
        ctx.put("performer_driver", asString(s.driverName()));
        ctx.put("performer_driver_phone", "+7 900 222 33 44");
        ctx.put("performer_vehicle_type", "1 рейс");
        ctx.put("performer_inn", "000000000000");
        ctx.put("performer_bik", "044525225");
        ctx.put("performer_kpp", "770101001");
        ctx.put("performer_rsh", "40702810000000000001");
        ctx.put("performer_ksh", "30101810400000000225");

        ctx.put("customer_name", asString(s.shipperName()));
        ctx.put("customer_full_name", asString(s.shipperName()));
        ctx.put("customer_info", "ООО Ромашка, ИНН 7701234567, +7 812 000 00 00, Банк клиента, г. Москва, ул. Ленина, 1");
        ctx.put("customer_phone", "+7 812 000 00 00");
        ctx.put("customer_bank", "Банк клиента");

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
