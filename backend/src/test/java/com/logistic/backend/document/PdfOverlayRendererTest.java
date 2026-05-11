package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PdfOverlayRendererTest {

    @Test
    void overlaysSampleValuesOntoActPdf() throws Exception {
        byte[] tpl;
        try (InputStream in = new ClassPathResource("templates/documents/act_of_work.pdf").getInputStream()) {
            tpl = in.readAllBytes();
        }
        PdfOverlayRenderer renderer = new PdfOverlayRenderer();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("act_title_line", "Акт выполненных работ №99 от 15 мая 2026г.");
        values.put("count", "3");
        values.put("total_price", "12 345,67");
        byte[] out = renderer.render(tpl, values);
        assertThat(out).startsWith("%PDF".getBytes());
        try (PDDocument pd = Loader.loadPDF(out)) {
            PDAcroForm acro = pd.getDocumentCatalog().getAcroForm();
            assertThat(acro == null || acro.getFields().isEmpty())
                    .as("flattened PDF must not retain interactive form fields")
                    .isTrue();
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pd);
            assertThat(text).contains("99");
            assertThat(text).contains("3");
            assertThat(text).contains("12");
        }
    }
}
