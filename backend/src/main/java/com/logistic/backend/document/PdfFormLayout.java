package com.logistic.backend.document;

import java.util.List;

/**
 * Absolute placement (PDF user space, origin bottom-left) for overlaying values onto flat
 * template PDFs. Slots mirror {@code field_name} entries in {@code utilities/docx_form_prep/config.py}
 * per {@link DocumentType}. LibreOffice does not emit AcroForm widgets for these Writer SDTs, so
 * coordinates are maintained alongside the Python form-prep config.
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
                new Slot("act_title_line", 40f, 800f, 555f, 828f, 10f),
                new Slot("act_contract_ref_line", 40f, 782f, 555f, 798f, 9f),
                new Slot("act_route_line", 40f, 765f, 555f, 780f, 9f),
                new Slot("act_services_total_sentence", 40f, 748f, 555f, 763f, 9f),
                new Slot("performer_info_ws", 42f, 250f, 300f, 420f, 8f),
                new Slot("customer_info_ws", 300f, 250f, 553f, 420f, 8f),
                new Slot("word_price", 40f, 125f, 555f, 235f, 8f),
                new Slot("total_price_rub", 360f, 528f, 540f, 552f, 10f),
                new Slot("count", 343f, 598f, 388f, 618f, 10f),
                new Slot("price", 418f, 598f, 505f, 618f, 9f),
                new Slot("total_price", 498f, 598f, 552f, 618f, 9f));
    }

    private static List<Slot> contractSlots() {
        return List.of(
                new Slot("contract_title_line", 80f, 800f, 520f, 820f, 11f),
                new Slot("contract_word_date_line", 80f, 784f, 350f, 800f, 10f),
                new Slot("contract_total_vat_notice", 320f, 215f, 560f, 238f, 9f),
                new Slot("customer_name", 27f, 705f, 270f, 724f, 9f),
                new Slot("customer_phone", 280f, 705f, 420f, 724f, 9f),
                new Slot("loading_place", 27f, 660f, 553f, 678f, 9f),
                new Slot("contact_loading", 27f, 643f, 553f, 660f, 9f),
                new Slot("unloading_place", 27f, 623f, 553f, 640f, 9f),
                new Slot("contact_unloading", 27f, 606f, 553f, 623f, 9f),
                new Slot("performer_vehicle_type", 27f, 575f, 155f, 595f, 8f),
                new Slot("performer_vehicle", 155f, 558f, 268f, 578f, 9f),
                new Slot("performer_vehicle_number", 268f, 528f, 380f, 548f, 9f),
                new Slot("performer_driver", 27f, 503f, 260f, 522f, 9f),
                new Slot("performer_driver_phone", 305f, 503f, 410f, 522f, 9f),
                new Slot("performer_info", 28f, 185f, 295f, 410f, 8f),
                new Slot("customer_info", 305f, 185f, 560f, 410f, 8f));
    }

    private static List<Slot> waybillSlots() {
        return List.of(
                new Slot("waybill_invoice_title", 40f, 788f, 555f, 805f, 10f),
                new Slot("waybill_contract_ref_line", 40f, 805f, 555f, 822f, 9f),
                new Slot("waybill_route_line", 40f, 448f, 555f, 462f, 9f),
                new Slot("waybill_items_total_line", 40f, 463f, 555f, 478f, 9f),
                new Slot("waybill_total_price_rub", 380f, 438f, 540f, 455f, 9f),
                new Slot("performer_bank", 33f, 715f, 280f, 735f, 8f),
                new Slot("performer_bik", 306f, 758f, 400f, 772f, 9f),
                new Slot("performer_ksh", 306f, 732f, 400f, 748f, 9f),
                new Slot("performer_inn", 95f, 748f, 165f, 765f, 9f),
                new Slot("performer_kpp", 176f, 748f, 240f, 765f, 9f),
                new Slot("performer_rsh", 33f, 688f, 280f, 708f, 9f),
                new Slot("performer_full_name", 33f, 668f, 300f, 688f, 9f),
                new Slot("performer_info_ws", 33f, 612f, 300f, 660f, 8f),
                new Slot("customer_info_ws", 33f, 558f, 300f, 608f, 8f),
                new Slot("count", 312f, 505f, 348f, 524f, 10f),
                new Slot("price", 392f, 505f, 458f, 524f, 9f),
                new Slot("total_price", 458f, 505f, 525f, 524f, 9f),
                new Slot("word_price", 32f, 310f, 485f, 390f, 8f));
    }
}
