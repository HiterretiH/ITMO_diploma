package com.logistic.backend.document;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Fills AcroForm fields from {@link PdfFormValuesBuilder} keys, applies multiline/left alignment for
 * long text fields, then flattens the form so the result is a non-interactive PDF (in-memory only).
 */
@Component
public class PdfOverlayRenderer {

    private static final String LIBERATION_SANS_RESOURCE = "/fonts/LiberationSans-Regular.ttf";

    /**
     * Text fields aligned with Word multi-line SDT placeholders in generated form PDFs. Keep in sync
     * with template field names used for those placeholders.
     */
    private static final Set<String> MULTILINE_ACROFORM_FIELD_NAMES = Set.of(
            "performer_info_ws",
            "customer_info_ws",
            "performer_info",
            "customer_info",
            "word_price");

    private static volatile BaseFont liberationSansCache;

    public byte[] render(byte[] templatePdf, Map<String, String> values) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(templatePdf);
            PdfStamper stamper = new PdfStamper(reader, out);
            AcroFields af = stamper.getAcroFields();
            applyEmbeddedCyrillicFont(af);
            applyMultilineLeftQuadding(af);
            Set<String> filledByAcro = applyAcroFormValues(stamper, values);
            ensureAllNonBlankKeysApplied(filledByAcro, values);
            stamper.setFormFlattening(true);
            stamper.close();
            reader.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("PDF AcroForm fill failed", e);
        }
    }

    private static BaseFont liberationSans() throws IOException, DocumentException {
        BaseFont cached = liberationSansCache;
        if (cached != null) {
            return cached;
        }
        synchronized (PdfOverlayRenderer.class) {
            cached = liberationSansCache;
            if (cached != null) {
                return cached;
            }
            try (InputStream in = PdfOverlayRenderer.class.getResourceAsStream(LIBERATION_SANS_RESOURCE)) {
                if (in == null) {
                    throw new IOException("Missing classpath resource: " + LIBERATION_SANS_RESOURCE);
                }
                byte[] ttf = in.readAllBytes();
                BaseFont bf = BaseFont.createFont(
                        "LiberationSans-Regular.ttf",
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED,
                        true,
                        ttf,
                        null,
                        false,
                        false);
                liberationSansCache = bf;
                return bf;
            }
        }
    }

    private static void applyEmbeddedCyrillicFont(AcroFields af) throws IOException, DocumentException {
        if (af == null) {
            return;
        }
        Map<String, AcroFields.Item> all = af.getAllFields();
        if (all == null || all.isEmpty()) {
            return;
        }
        BaseFont bf = liberationSans();
        for (String fieldName : all.keySet()) {
            af.setFieldProperty(fieldName, "textfont", bf, null);
        }
    }

    private static void applyMultilineLeftQuadding(AcroFields af) {
        if (af == null) {
            return;
        }
        Map<String, AcroFields.Item> all = af.getAllFields();
        if (all == null || all.isEmpty()) {
            return;
        }
        int qTargets =
                AcroFields.Item.WRITE_MERGED | AcroFields.Item.WRITE_WIDGET | AcroFields.Item.WRITE_VALUE;
        PdfNumber qLeft = new PdfNumber(PdfFormField.Q_LEFT);
        for (String field : MULTILINE_ACROFORM_FIELD_NAMES) {
            if (!all.containsKey(field)) {
                continue;
            }
            af.setFieldProperty(field, "setfflags", PdfFormField.FF_MULTILINE, null);
            AcroFields.Item item = af.getFieldItem(field);
            if (item != null) {
                item.writeToAll(PdfName.Q, qLeft, qTargets);
                item.markUsed(af, AcroFields.Item.WRITE_VALUE | AcroFields.Item.WRITE_WIDGET);
            }
        }
    }

    private static Set<String> applyAcroFormValues(PdfStamper stamper, Map<String, String> values)
            throws IOException, DocumentException {
        AcroFields af = stamper.getAcroFields();
        if (af == null || af.getAllFields() == null || af.getAllFields().isEmpty()) {
            return Set.of();
        }
        Set<String> done = new HashSet<>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (val == null || val.isBlank()) {
                continue;
            }
            if (!af.getAllFields().containsKey(key)) {
                continue;
            }
            if (af.setField(key, val)) {
                done.add(key);
            }
        }
        return done;
    }

    private static void ensureAllNonBlankKeysApplied(Set<String> applied, Map<String, String> values)
            throws IOException {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            String v = e.getValue();
            if (v == null || v.isBlank()) {
                continue;
            }
            if (!applied.contains(e.getKey())) {
                missing.add(e.getKey());
            }
        }
        if (!missing.isEmpty()) {
            throw new IOException(
                    "PDF AcroForm is missing fields or setField failed for non-blank keys: "
                            + missing
                            + ". Regenerate classpath PDF templates or align field names with "
                            + "PdfFormValuesBuilder keys.");
        }
    }
}
