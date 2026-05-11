package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfFormValuesBuilderTest {

    @Test
    void actCompositesMatchConfigPlaceholders() {
        OrderPrintSnapshot s = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> m = PdfFormValuesBuilder.values(DocumentType.ACT_OF_WORK, s);
        assertThat(m.get("act_title_line")).contains("42").contains("2026");
        assertThat(m.get("act_contract_ref_line")).contains("2026-05-15").contains("15 мая");
        assertThat(m.get("act_route_line")).contains("Москва").contains("Санкт-Петербург");
        assertThat(m.get("act_services_total_sentence")).contains("1").contains("98500.00").contains("руб.");
        assertThat(m.get("count")).isEqualTo("1");
        assertThat(m.get("total_price_rub")).isEqualTo("98500.00 руб");
    }

    @Test
    void contractTitleAndVatNotice() {
        OrderPrintSnapshot s = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> m = PdfFormValuesBuilder.values(DocumentType.CONTRACT_APPLICATION, s);
        assertThat(m.get("contract_title_line")).isEqualTo("ДОГОВОР-ЗАЯВКА № 2026-05-15");
        assertThat(m.get("contract_word_date_line")).isEqualTo("от 15 мая 2026г.");
        assertThat(m.get("contract_total_vat_notice")).isEqualTo("98500.00 руб. БЕЗ НДС");
        assertThat(m.get("customer_name")).isEqualTo("ООО Ромашка");
    }

    @Test
    void waybillInvoiceTitle() {
        OrderPrintSnapshot s = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> m = PdfFormValuesBuilder.values(DocumentType.WAYBILL, s);
        assertThat(m.get("waybill_invoice_title")).contains("42").contains("15 мая");
        assertThat(m.get("waybill_items_total_line")).contains("1").contains("98500.00");
    }
}
