package com.logistic.backend.document;

import java.util.List;

/**
 * Absolute placement (PDF user space, origin bottom-left) for overlaying values onto flat
 * template PDFs exported by LibreOffice from {@code *.form.docx}. Coordinates are calibrated
 * against {@link PdfTemplateLayoutProbeTest} glyph dumps in {@code build/reports/} for the
 * classpath templates; keep in sync with {@code utilities/docx_form_prep/config.py} {@code field_name} keys.
 */
public final class PdfFormLayout {

    private PdfFormLayout() {}

    public record Slot(String fieldKey, float llx, float lly, float urx, float ury, float fontSizePt) {}

    public static List<Slot> slots(DocumentType type) {
        return switch (type) {
            case ACT_OF_WORK -> actSlots();
            case CONTRACT_APPLICATION -> contractSlots();
            case WAYBILL -> waybillSlots();
        };
    }

    private static List<Slot> actSlots() {
        return List.of(
                new Slot("act_title_line", 40f, 232f, 555f, 252f, 10f),
                new Slot("act_contract_ref_line", 40f, 214f, 555f, 230f, 9f),
                new Slot("act_route_line", 40f, 196f, 555f, 212f, 9f),
                new Slot("act_services_total_sentence", 40f, 178f, 555f, 194f, 9f),
                new Slot("total_price_rub", 340f, 268f, 530f, 288f, 10f),
                new Slot("count", 343f, 222f, 392f, 244f, 10f),
                new Slot("price", 418f, 222f, 508f, 244f, 9f),
                new Slot("total_price", 498f, 222f, 558f, 244f, 9f),
                new Slot("performer_info_ws", 42f, 88f, 302f, 168f, 8f),
                new Slot("customer_info_ws", 302f, 88f, 553f, 168f, 8f),
                new Slot("word_price", 40f, 300f, 555f, 358f, 8f));
    }

    private static List<Slot> contractSlots() {
        return List.of(
                new Slot("contract_title_line", 28f, 805f, 520f, 828f, 11f),
                new Slot("contract_word_date_line", 28f, 788f, 380f, 803f, 10f),
                new Slot("contract_total_vat_notice", 260f, 48f, 560f, 68f, 9f),
                new Slot("customer_name", 130f, 110f, 275f, 124f, 9f),
                new Slot("customer_phone", 318f, 110f, 430f, 124f, 9f),
                new Slot("loading_place", 28f, 122f, 553f, 138f, 9f),
                new Slot("contact_loading", 28f, 104f, 553f, 120f, 9f),
                new Slot("unloading_place", 28f, 158f, 553f, 176f, 9f),
                new Slot("contact_unloading", 28f, 140f, 553f, 156f, 9f),
                new Slot("performer_vehicle_type", 28f, 205f, 255f, 228f, 8f),
                new Slot("performer_vehicle", 95f, 298f, 268f, 316f, 9f),
                new Slot("performer_vehicle_number", 272f, 298f, 398f, 316f, 9f),
                new Slot("performer_driver", 100f, 322f, 295f, 340f, 9f),
                new Slot("performer_driver_phone", 318f, 322f, 415f, 340f, 9f),
                new Slot("performer_info", 28f, 430f, 295f, 505f, 8f),
                new Slot("customer_info", 302f, 430f, 560f, 505f, 8f));
    }

    private static List<Slot> waybillSlots() {
        return List.of(
                new Slot("waybill_invoice_title", 32f, 800f, 560f, 836f, 11f),
                new Slot("waybill_contract_ref_line", 95f, 248f, 548f, 266f, 9f),
                new Slot("waybill_route_line", 42f, 300f, 358f, 332f, 9f),
                new Slot("waybill_items_total_line", 32f, 332f, 548f, 356f, 9f),
                new Slot("waybill_total_price_rub", 200f, 384f, 400f, 404f, 10f),
                new Slot("performer_bank", 100f, 71f, 295f, 82f, 9f),
                new Slot("performer_bik", 330f, 46f, 405f, 59f, 9f),
                new Slot("performer_ksh", 332f, 59f, 418f, 71f, 9f),
                new Slot("performer_rsh", 332f, 71f, 418f, 83f, 9f),
                new Slot("performer_inn", 56f, 83f, 135f, 96f, 9f),
                new Slot("performer_kpp", 200f, 83f, 275f, 96f, 9f),
                new Slot("performer_full_name", 95f, 118f, 520f, 132f, 9f),
                new Slot("performer_info_ws", 95f, 188f, 540f, 228f, 8f),
                new Slot("customer_info_ws", 95f, 226f, 540f, 256f, 8f),
                new Slot("count", 312f, 300f, 352f, 328f, 10f),
                new Slot("price", 392f, 300f, 460f, 328f, 9f),
                new Slot("total_price", 462f, 300f, 530f, 328f, 9f),
                new Slot("word_price", 32f, 408f, 548f, 460f, 7.5f));
    }
}
