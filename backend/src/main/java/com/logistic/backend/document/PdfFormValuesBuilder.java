package com.logistic.backend.document;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds {@code field_name → value} maps for PDF AcroForm fill. Keys must match named fields in classpath PDF
 * templates. DOCX placeholders use {@link DocumentGenerationService#snapshotToContext(OrderPrintSnapshot)}.
 */
public final class PdfFormValuesBuilder {

    private PdfFormValuesBuilder() {}

    public static Map<String, String> values(DocumentType type, OrderPrintSnapshot s) {
        return switch (type) {
            case ACT_OF_WORK -> actValues(s);
            case CONTRACT_APPLICATION -> contractValues(s);
            case WAYBILL -> waybillValues(s);
        };
    }

    private static Map<String, String> actValues(OrderPrintSnapshot s) {
        Map<String, String> m = new LinkedHashMap<>();
        String count = tripCountString(s);
        String totalPrice = RubMoneyWords.formatMoneyTwoDecimals(s.totalPrice());
        String wordDate = formatRuDate(s.orderDate());
        String date = asString(s.orderDate());
        String number = asString(s.orderNumber());
        m.put(
                "act_services_total_sentence",
                "Всего оказано услуг " + count + ", на сумму " + totalPrice + " руб.");
        m.put(
                "act_route_line",
                "Автоуслуги по маршруту: " + asString(s.loadingPlace()) + " --- " + asString(s.unloadingPlace()));
        m.put(
                "act_contract_ref_line",
                "Договор-заявка на перевозку груза №" + date + " от " + wordDate);
        m.put("act_title_line", "Акт выполненных работ №" + number + " от " + wordDate);
        m.put("total_price_rub", totalPrice + " руб");
        m.put("performer_info_ws", asString(s.performerRequisites()));
        m.put("customer_info_ws", asString(s.customerRequisites()));
        m.put("count", count);
        m.put("price", RubMoneyWords.formatMoneyTwoDecimals(s.pricePerTrip()));
        m.put("total_price", totalPrice);
        m.put("word_price", RubMoneyWords.amountInWords(s.totalPrice()));
        return Map.copyOf(m);
    }

    private static Map<String, String> contractValues(OrderPrintSnapshot s) {
        Map<String, String> m = new LinkedHashMap<>();
        String totalPrice = RubMoneyWords.formatMoneyTwoDecimals(s.totalPrice());
        m.put("contract_total_vat_notice", totalPrice + " руб. БЕЗ НДС");
        m.put("contract_title_line", "ДОГОВОР-ЗАЯВКА № " + asString(s.orderDate()));
        m.put("contract_word_date_line", "от " + formatRuDate(s.orderDate()));
        m.put("customer_name", asString(s.customerShortName()));
        m.put("customer_phone", asString(s.customerPhone()));
        m.put("loading_place", asString(s.loadingPlace()));
        m.put("contact_loading", asString(s.loadingContact()));
        m.put("unloading_place", asString(s.unloadingPlace()));
        m.put("contact_unloading", asString(s.unloadingContact()));
        m.put("performer_vehicle_type", asString(s.vehicleType()));
        m.put("performer_vehicle", asString(s.vehicleBrandModel()));
        m.put("performer_vehicle_number", asString(s.vehiclePlateNumber()));
        m.put("performer_driver", asString(s.driverFullName()));
        m.put("performer_driver_phone", asString(s.driverPhone()));
        m.put("performer_info", asString(s.performerRequisites()));
        m.put("customer_info", asString(s.customerRequisites()));
        return Map.copyOf(m);
    }

    private static Map<String, String> waybillValues(OrderPrintSnapshot s) {
        Map<String, String> m = new LinkedHashMap<>();
        String count = tripCountString(s);
        String totalPrice = RubMoneyWords.formatMoneyTwoDecimals(s.totalPrice());
        String wordDate = formatRuDate(s.orderDate());
        String date = asString(s.orderDate());
        String number = asString(s.orderNumber());
        m.put(
                "waybill_items_total_line",
                "Всего наименований " + count + ", на сумму " + totalPrice + " руб");
        m.put(
                "waybill_route_line",
                "Автоуслуги по маршруту: " + asString(s.loadingPlace()) + " --- " + asString(s.unloadingPlace()));
        m.put(
                "waybill_contract_ref_line",
                "Договор-заявка на перевозку груза №" + date + " от " + wordDate);
        m.put("waybill_invoice_title", "Счет на оплату №" + number + " от " + wordDate);
        m.put("waybill_total_price_rub", totalPrice + " руб");
        m.put("performer_bank", asString(s.performerBankName()));
        m.put("performer_bik", asString(s.performerBik()));
        m.put("performer_ksh", asString(s.performerCorrAccount()));
        m.put("performer_inn", asString(s.performerInn()));
        m.put("performer_kpp", asString(s.performerKpp()));
        m.put("performer_rsh", asString(s.performerPaymentAccount()));
        m.put("performer_full_name", asString(s.performerFullName()));
        m.put("performer_info_ws", asString(s.performerRequisites()));
        m.put("customer_info_ws", asString(s.customerRequisites()));
        m.put("count", count);
        m.put("price", RubMoneyWords.formatMoneyTwoDecimals(s.pricePerTrip()));
        m.put("total_price", totalPrice);
        m.put("word_price", RubMoneyWords.amountInWords(s.totalPrice()));
        return Map.copyOf(m);
    }

    private static String tripCountString(OrderPrintSnapshot s) {
        return s.tripCount() > 0 ? Integer.toString(s.tripCount()) : "1";
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    /** Same human-readable Russian date as {@link DocumentGenerationService}. */
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
}
