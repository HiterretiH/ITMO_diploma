package com.logistic.backend.document;

import java.util.List;

/**
 * Rectangle and font size per field key for {@link PdfAcroFormBootstrap} when a template PDF lacks usable AcroForm
 * widgets. PDF user space (origin bottom-left); keep keys aligned with {@link PdfFormValuesBuilder} and templates.
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

    /**
     * Throws if any two slot rectangles intersect (same user space). Run from tests after
     * changing coordinates used by {@link PdfAcroFormBootstrap}.
     */
    public static void validateNoOverlap(DocumentType type) {
        List<Slot> list = slots(type);
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Slot a = list.get(i);
                Slot b = list.get(j);
                if (a.llx() < b.urx()
                        && a.urx() > b.llx()
                        && a.lly() < b.ury()
                        && a.ury() > b.lly()) {
                    throw new IllegalStateException("Overlap: " + a.fieldKey() + " and " + b.fieldKey());
                }
            }
        }
    }

    private static List<Slot> actSlots() {
        return List.of(
                new Slot("act_title_line", 38f, 498f, 558f, 528f, 10f),
                new Slot("act_contract_ref_line", 38f, 476f, 558f, 494f, 9f),
                new Slot("act_route_line", 52f, 218f, 338f, 252f, 8f),
                new Slot("act_services_total_sentence", 38f, 254f, 558f, 274f, 9f),
                new Slot("total_price_rub", 300f, 278f, 538f, 296f, 10f),
                new Slot("count", 346f, 220f, 396f, 250f, 10f),
                new Slot("price", 428f, 220f, 484f, 250f, 9f),
                new Slot("total_price", 498f, 220f, 542f, 250f, 9f),
                new Slot("performer_info_ws", 42f, 96f, 286f, 176f, 8f),
                new Slot("customer_info_ws", 292f, 96f, 556f, 176f, 8f),
                new Slot("word_price", 38f, 302f, 558f, 358f, 7.5f));
    }

    private static List<Slot> contractSlots() {
        return List.of(
                new Slot("contract_title_line", 28f, 738f, 520f, 762f, 11f),
                new Slot("contract_word_date_line", 28f, 716f, 400f, 732f, 10f),
                new Slot("contract_total_vat_notice", 260f, 48f, 560f, 66f, 9f),
                new Slot("customer_name", 130f, 98f, 278f, 114f, 9f),
                new Slot("customer_phone", 318f, 98f, 435f, 114f, 9f),
                new Slot("loading_place", 28f, 118f, 553f, 134f, 9f),
                new Slot("contact_loading", 28f, 136f, 553f, 150f, 9f),
                new Slot("unloading_place", 28f, 154f, 553f, 170f, 9f),
                new Slot("contact_unloading", 28f, 172f, 553f, 188f, 9f),
                new Slot("performer_vehicle_type", 28f, 208f, 260f, 226f, 8f),
                new Slot("performer_vehicle", 95f, 298f, 268f, 316f, 9f),
                new Slot("performer_vehicle_number", 272f, 298f, 400f, 316f, 9f),
                new Slot("performer_driver", 100f, 316f, 295f, 334f, 9f),
                new Slot("performer_driver_phone", 318f, 316f, 420f, 334f, 9f),
                new Slot("performer_info", 28f, 428f, 295f, 508f, 8f),
                new Slot("customer_info", 302f, 428f, 560f, 508f, 8f));
    }

    private static List<Slot> waybillSlots() {
        return List.of(
                new Slot("waybill_invoice_title", 32f, 765f, 560f, 798f, 11f),
                new Slot("waybill_contract_ref_line", 88f, 732f, 548f, 756f, 9f),
                new Slot("waybill_route_line", 42f, 296f, 300f, 332f, 9f),
                new Slot("waybill_items_total_line", 30f, 334f, 550f, 354f, 9f),
                new Slot("waybill_total_price_rub", 195f, 362f, 445f, 382f, 10f),
                new Slot("performer_bank", 100f, 71f, 295f, 82f, 9f),
                new Slot("performer_bik", 330f, 46f, 405f, 59f, 9f),
                new Slot("performer_ksh", 332f, 59f, 418f, 71f, 9f),
                new Slot("performer_rsh", 332f, 71f, 418f, 83f, 9f),
                new Slot("performer_inn", 56f, 83f, 135f, 96f, 9f),
                new Slot("performer_kpp", 200f, 83f, 275f, 96f, 9f),
                new Slot("performer_full_name", 95f, 118f, 520f, 134f, 9f),
                new Slot("performer_info_ws", 95f, 188f, 540f, 224f, 8f),
                new Slot("customer_info_ws", 95f, 224f, 540f, 256f, 8f),
                new Slot("count", 308f, 298f, 352f, 328f, 10f),
                new Slot("price", 392f, 298f, 460f, 328f, 9f),
                new Slot("total_price", 462f, 298f, 528f, 328f, 9f),
                new Slot("word_price", 30f, 395f, 548f, 458f, 7.5f));
    }
}
