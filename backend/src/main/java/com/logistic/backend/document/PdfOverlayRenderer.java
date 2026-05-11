package com.logistic.backend.document;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Fills PDF AcroForm fields only: names must match {@link PdfFormValuesBuilder} keys. Templates are
 * produced from {@code *.form.docx} via LibreOffice with form export enabled. There is no
 * coordinate overlay fallback — regenerate PDFs if fields are missing or misnamed.
 */
@Component
public class PdfOverlayRenderer {

    public byte[] render(byte[] templatePdf, Map<String, String> values) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(templatePdf);
            PdfStamper stamper = new PdfStamper(reader, out);
            Set<String> filledByAcro = applyAcroFormValues(stamper, values);
            ensureAllNonBlankKeysApplied(filledByAcro, values);
            stamper.close();
            reader.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("PDF AcroForm fill failed", e);
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
                            + ". Regenerate classpath PDFs from *.form.docx (LibreOffice export with form fields; "
                            + "field names must match utilities/docx_to_pdf_template/config.py field_name).");
        }
    }
}
