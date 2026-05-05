package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

class DocxTemplateRendererTest {

    private final DocxTemplateRenderer renderer = new DocxTemplateRenderer();

    @Test
    void replacesDocxPlaceholdersInParagraphAndTableCell() throws Exception {
        byte[] template;
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFRun p = doc.createParagraph().createRun();
            p.setText("Client {{shipperName}}. Route ${routeFrom} -> ${routeTo}");
            var table = doc.createTable(1, 1);
            table.getRow(0).getCell(0).setText("Price {{priceAmount}} {{currency}}");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            template = out.toByteArray();
        }

        byte[] rendered =
                renderer.render(
                        template,
                        Map.of(
                                "shipperName", "Ship LLC",
                                "routeFrom", "Moscow",
                                "routeTo", "Tver",
                                "priceAmount", "45000.00",
                                "currency", "RUB"));

        String text = renderer.extractText(rendered);
        assertThat(text).contains("Client Ship LLC. Route Moscow -> Tver");
        assertThat(text).contains("Price 45000.00 RUB");
        assertThat(text).doesNotContain("{{");
        assertThat(text).doesNotContain("${");
    }
}
