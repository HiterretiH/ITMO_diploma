package com.logistic.backend.document;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Overlays printable values onto flat template PDFs using Liberation Sans (Cyrillic). Each slot is
 * clipped to its rectangle so overflow does not cover neighbouring labels. Coordinates are defined
 * in {@link PdfFormLayout}.
 */
@Component
public class PdfOverlayRenderer {

    private static final String FONT_RESOURCE = "fonts/LiberationSans-Regular.ttf";

    private volatile BaseFont cyrillicBase;

    public byte[] render(byte[] templatePdf, DocumentType type, Map<String, String> values)
            throws IOException {
        BaseFont bf = cyrillicBaseFont();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(templatePdf);
            PdfStamper stamper = new PdfStamper(reader, out);
            PdfContentByte cb = stamper.getOverContent(1);
            for (PdfFormLayout.Slot slot : PdfFormLayout.slots(type)) {
                String v = values.get(slot.fieldKey());
                if (v == null || v.isBlank()) {
                    continue;
                }
                drawInSlot(cb, bf, slot, v);
            }
            stamper.close();
            reader.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("PDF overlay failed", e);
        }
    }

    private static void drawInSlot(PdfContentByte cb, BaseFont bf, PdfFormLayout.Slot slot, String text)
            throws DocumentException {
        float w = slot.urx() - slot.llx();
        float h = slot.ury() - slot.lly();
        if (w <= 0 || h <= 0) {
            return;
        }
        float fontSize = scaleFontToFit(slot.fontSizePt(), text, w, h, bf);
        Font font = new Font(bf, fontSize);
        float leading = fontSize * 1.12f;
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setLeading(leading);

        cb.saveState();
        cb.rectangle(slot.llx(), slot.lly(), w, h);
        cb.clip();
        cb.newPath();

        ColumnText ct = new ColumnText(cb);
        ct.setSimpleColumn(
                paragraph, slot.llx(), slot.lly(), slot.urx(), slot.ury(), leading, Element.ALIGN_LEFT);
        ct.go();

        cb.restoreState();
    }

    /**
     * Slightly shrink font when the text is long relative to slot area so multi-line blocks stay
     * inside the clip (especially {@code word_price}).
     */
    private static float scaleFontToFit(
            float requestedPt, String text, float slotWidth, float slotHeight, BaseFont bf) {
        float size = requestedPt;
        for (int i = 0; i < 6 && size >= 6f; i++) {
            float lineHeight = size * 1.12f;
            int approxLines =
                    Math.max(
                            1,
                            (int)
                                    Math.ceil(
                                            bf.getWidthPoint(text, size) / Math.max(1f, slotWidth - 2f)));
            if (approxLines * lineHeight <= slotHeight + 0.5f) {
                return size;
            }
            size -= 0.75f;
        }
        return Math.max(6f, size);
    }

    private BaseFont cyrillicBaseFont() throws IOException {
        BaseFont cached = cyrillicBase;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cyrillicBase == null) {
                ClassPathResource res = new ClassPathResource(FONT_RESOURCE);
                if (!res.exists()) {
                    throw new IOException("Missing classpath font: " + FONT_RESOURCE);
                }
                byte[] bytes;
                try (InputStream in = res.getInputStream()) {
                    bytes = in.readAllBytes();
                }
                cyrillicBase =
                        BaseFont.createFont(
                                "LiberationSans.ttf",
                                BaseFont.IDENTITY_H,
                                BaseFont.EMBEDDED,
                                true,
                                bytes,
                                null);
            }
            return cyrillicBase;
        }
    }
}
