package com.logistic.backend.document;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.TextField;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Adds empty {@link TextField}s at {@link PdfFormLayout} rectangles when a template PDF lacks the AcroForm fields
 * required for {@link PdfOverlayRenderer}. Leaves the PDF unchanged if every layout key is already present. Run via
 * {@code ./gradlew bootstrapPdfAcroForms}.
 */
public final class PdfAcroFormBootstrap {

    private PdfAcroFormBootstrap() {}

    public static void main(String[] args) throws Exception {
        Path dir =
                Path.of(args.length > 0 ? args[0] : "src/main/resources/templates/documents")
                        .toAbsolutePath()
                        .normalize();
        for (DocumentType t : DocumentType.values()) {
            Path pdf = dir.resolve(PdfFormTemplateCache.fileName(t));
            byte[] in = Files.readAllBytes(pdf);
            byte[] out = addEmptyFieldsIfNeeded(in, t);
            Files.write(pdf, out);
            System.out.println("AcroForm bootstrap: " + pdf);
        }
    }

    /** Returns {@code templatePdf} unchanged if AcroForm already lists every layout field key. */
    public static byte[] addEmptyFieldsIfNeeded(byte[] templatePdf, DocumentType type)
            throws IOException, DocumentException {
        PdfReader probe = new PdfReader(templatePdf);
        try {
            AcroFields af = probe.getAcroFields();
            if (af != null && af.getAllFields() != null && !af.getAllFields().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, ?> fields = (Map<String, ?>) af.getAllFields();
                List<PdfFormLayout.Slot> slots = PdfFormLayout.slots(type);
                boolean all = slots.stream().allMatch(s -> fields.containsKey(s.fieldKey()));
                if (all && fields.size() >= slots.size()) {
                    return templatePdf;
                }
            }
        } finally {
            probe.close();
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(templatePdf);
        PdfStamper stamper = new PdfStamper(reader, bos);
        PdfWriter writer = stamper.getWriter();
        for (PdfFormLayout.Slot slot : PdfFormLayout.slots(type)) {
            Rectangle r = new Rectangle(slot.llx(), slot.lly(), slot.urx(), slot.ury());
            TextField tf = new TextField(writer, r, slot.fieldKey());
            tf.setFontSize(slot.fontSizePt());
            tf.setOptions(TextField.MULTILINE | TextField.DO_NOT_SCROLL);
            PdfFormField fld = tf.getTextField();
            stamper.addAnnotation(fld, 1);
        }
        stamper.close();
        reader.close();
        return bos.toByteArray();
    }
}
