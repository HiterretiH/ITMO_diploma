package com.logistic.backend.document;

import com.logistic.backend.order.Order;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private final DocxTemplateRenderer templateRenderer;
    private final DocxPdfConverter docxPdfConverter;
    private final OrderSnapshotMapper orderSnapshotMapper;

    /**
     * Renders a single document in memory (no disk, no DB). Caller must ensure order data is complete
     * if required for the template.
     */
    public byte[] generateDocument(Order order, DocumentType type, FileFormat format) throws IOException {
        OrderPrintSnapshot snap = orderSnapshotMapper.fromOrder(order);
        byte[] renderedDocx = renderDocxFromTemplate(type, snap);
        return format == FileFormat.DOCX ? renderedDocx : docxPdfConverter.convert(renderedDocx);
    }

    public static String downloadFileName(DocumentType type, FileFormat format) {
        String ext = format == FileFormat.PDF ? ".pdf" : ".docx";
        return type.name().toLowerCase() + ext;
    }

    public static String contentTypeFor(FileFormat format) {
        return format == FileFormat.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    private byte[] renderDocxFromTemplate(DocumentType type, OrderPrintSnapshot snapshot) throws IOException {
        String resourcePath = "templates/documents/" + templateName(type);
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            return templateRenderer.render(in.readAllBytes(), snapshotToContext(snapshot));
        }
    }

    private static String templateName(DocumentType type) {
        return switch (type) {
            case CONTRACT_APPLICATION -> "contract_application.docx";
            case WAYBILL -> "waybill.docx";
            case ACT_OF_WORK -> "act_of_work.docx";
        };
    }

    static Map<String, String> snapshotToContext(OrderPrintSnapshot s) {
        Map<String, String> ctx = new LinkedHashMap<>();

        String number = asString(s.orderNumber());
        String date = asString(s.orderDate());
        String wordDate = formatRuDate(s.orderDate());
        String loadingPlace = asString(s.loadingPlace());
        String unloadingPlace = asString(s.unloadingPlace());
        String contactLoading = asString(s.loadingContact());
        String contactUnloading = asString(s.unloadingContact());
        String count = s.tripCount() > 0 ? Integer.toString(s.tripCount()) : "1";
        String price = money(s.pricePerTrip());
        String totalPrice = money(s.totalPrice());
        String wordPrice = capitalize(totalPrice + " рублей");

        String performerInfoWs =
                joinWs(
                        asString(s.performerFullName()),
                        "ИНН " + asString(s.performerInn()),
                        "БИК " + asString(s.performerBik()),
                        "р/с " + asString(s.performerPaymentAccount()),
                        "к/с " + asString(s.performerCorrAccount()),
                        asString(s.performerRequisites()));
        String customerInfoWs =
                joinWs(
                        asString(s.customerFullName()),
                        asString(s.customerRequisites()),
                        "тел. " + asString(s.customerPhone()));

        ctx.put("number", number);
        ctx.put("date", date);
        ctx.put("word_date", wordDate);
        ctx.put("loading_place", loadingPlace);
        ctx.put("unloading_place", unloadingPlace);
        ctx.put("contact_loading", contactLoading);
        ctx.put("contact_unloading", contactUnloading);
        ctx.put("count", count);
        ctx.put("price", price);
        ctx.put("total_price", totalPrice);
        ctx.put("word_price", wordPrice);
        ctx.put("performer_info_ws", performerInfoWs);
        ctx.put("customer_info_ws", customerInfoWs);

        ctx.put("performer_name", asString(s.performerShortName()));
        ctx.put("performer_full_name", asString(s.performerFullName()));
        ctx.put("performer_info", asString(s.performerRequisites()));
        ctx.put("performer_phone", asString(s.performerPhone()));
        ctx.put("performer_bank", asString(s.performerBankName()));
        ctx.put("performer_vehicle", asString(s.vehicleBrandModel()));
        ctx.put("performer_vehicle_number", asString(s.vehiclePlateNumber()));
        ctx.put("performer_driver", asString(s.driverFullName()));
        ctx.put("performer_driver_phone", asString(s.driverPhone()));
        ctx.put("performer_vehicle_type", asString(s.vehicleType()));
        ctx.put("performer_inn", asString(s.performerInn()));
        ctx.put("performer_bik", asString(s.performerBik()));
        ctx.put("performer_kpp", asString(s.performerKpp()));
        ctx.put("performer_rsh", asString(s.performerPaymentAccount()));
        ctx.put("performer_ksh", asString(s.performerCorrAccount()));

        ctx.put("customer_name", asString(s.customerShortName()));
        ctx.put("customer_full_name", asString(s.customerFullName()));
        ctx.put("customer_info", asString(s.customerRequisites()));
        ctx.put("customer_phone", asString(s.customerPhone()));
        ctx.put("customer_bank", "");

        ctx.put("orderId", asString(s.orderId()));
        ctx.put("shipperName", asString(s.customerShortName()));
        ctx.put("shipperInn", "");
        ctx.put("shipperAddress", asString(s.customerRequisites()));
        ctx.put("consigneeName", "");
        ctx.put("consigneeInn", "");
        ctx.put("consigneeAddress", "");
        ctx.put("cargoDescription", "");
        ctx.put("cargoWeightKg", "");
        ctx.put("routeFrom", loadingPlace);
        ctx.put("routeTo", unloadingPlace);
        ctx.put("loadDate", date);
        ctx.put("unloadDate", "");
        ctx.put("driverName", asString(s.driverFullName()));
        ctx.put("driverLicense", "");
        ctx.put("vehiclePlate", asString(s.vehiclePlateNumber()));
        ctx.put("vehicleModel", asString(s.vehicleBrandModel()));
        ctx.put("vehicleCapacityKg", "");
        ctx.put("priceAmount", totalPrice);
        ctx.put("currency", "RUB");
        return ctx;
    }

    private static String joinWs(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String formatRuDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        String month =
                switch (date.getMonthValue()) {
                    case 1 -> "января";
                    case 2 -> "февраля";
                    case 3 -> "марта";
                    case 4 -> "апреля";
                    case 5 -> "мая";
                    case 6 -> "июня";
                    case 7 -> "июля";
                    case 8 -> "августа";
                    case 9 -> "сентября";
                    case 10 -> "октября";
                    case 11 -> "ноября";
                    case 12 -> "декабря";
                    default -> "";
                };
        return String.format("%02d %s %dг.", date.getDayOfMonth(), month, date.getYear());
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
