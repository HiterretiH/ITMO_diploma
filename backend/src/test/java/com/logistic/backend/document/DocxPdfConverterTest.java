package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DocxPdfConverterTest {

    @Test
    void renderedDocxConvertsToLayoutPdfWithCyrillicText() throws Exception {
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

        Map<String, String> context = fixtureContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        byte[] templateBytes;
        try (InputStream in =
                new ClassPathResource("templates/documents/contract_application.docx").getInputStream()) {
            templateBytes = in.readAllBytes();
        }
        byte[] docx = renderer.render(templateBytes, context);

        DocxPdfConverter converter = new DocxPdfConverter();
        byte[] pdf = converter.convert(docx);

        assertThat(pdf.length).isGreaterThan(20);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        try (PDDocument pd = Loader.loadPDF(pdf)) {
            assertThat(pd.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pd);
            assertThat(text).contains("ООО Ромашка");
            assertThat(text).contains("Иванов Иван Иванович");
            assertThat(text).contains("Москва");
            assertThat(text).contains("Санкт-Петербург");
            assertThat(text).doesNotContain("{{");
            assertThat(text).doesNotContain("${");
        }
    }

    private static Map<String, String> fixtureContext(TripPrintSnapshot s) {
        return Map.ofEntries(
                Map.entry("number", String.valueOf(s.tripId())),
                Map.entry("date", String.valueOf(s.loadDate())),
                Map.entry("word_date", "15 мая 2026г."),
                Map.entry("loading_place", s.routeFrom()),
                Map.entry("unloading_place", s.routeTo()),
                Map.entry("contact_loading", "Контакт Погрузки +7 900 123 45 67"),
                Map.entry("contact_unloading", "Контакт Разгрузки +7 900 765 43 21"),
                Map.entry("count", "1"),
                Map.entry("price", "98500.00"),
                Map.entry("total_price", "98500.00"),
                Map.entry("word_price", "Девяносто восемь тысяч пятьсот рублей"),
                Map.entry(
                        "performer_info_ws",
                        "ИП Петров П.П., ИНН 000000000000, БИК 044525225, р/с 40702810000000000001"),
                Map.entry(
                        "customer_info_ws",
                        "ООО Ромашка, ИНН 7701234567, Банк клиента, +7 812 000 00 00"),
                Map.entry("performer_name", "ИП Петров П.П."),
                Map.entry("performer_full_name", "ИП Петров Петр Петрович"),
                Map.entry("performer_info", "ИП Петров Петр Петрович, ИНН 000000000000"),
                Map.entry("performer_phone", "+7 900 111 22 33"),
                Map.entry("performer_bank", "АО Банк"),
                Map.entry("performer_vehicle", s.vehicleModel()),
                Map.entry("performer_vehicle_number", s.vehiclePlate()),
                Map.entry("performer_driver", s.driverName()),
                Map.entry("performer_driver_phone", "+7 900 222 33 44"),
                Map.entry("performer_vehicle_type", "1 рейс"),
                Map.entry("performer_inn", "000000000000"),
                Map.entry("performer_bik", "044525225"),
                Map.entry("performer_kpp", "770101001"),
                Map.entry("performer_rsh", "40702810000000000001"),
                Map.entry("performer_ksh", "30101810400000000225"),
                Map.entry("customer_name", s.shipperName()),
                Map.entry("customer_full_name", s.shipperName()),
                Map.entry("customer_info", "ООО Ромашка, ИНН 7701234567"),
                Map.entry("customer_phone", "+7 812 000 00 00"),
                Map.entry("customer_bank", "Банк клиента"));
    }
}
