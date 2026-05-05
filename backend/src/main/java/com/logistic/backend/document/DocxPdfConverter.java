package com.logistic.backend.document;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocxPdfConverter {

    private final DocxTemplateRenderer templateRenderer = new DocxTemplateRenderer();

    public byte[] convert(byte[] renderedDocx) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(renderedDocx);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.load(in);
            Docx4J.toPDF(pkg, out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("DOCX->PDF conversion fell back to plain text rendering: {}", e.getMessage());
            return renderPlainTextPdf(renderedDocx);
        }
    }

    private byte[] renderPlainTextPdf(byte[] renderedDocx) throws IOException {
        String text = templateRenderer.extractText(renderedDocx);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            for (String line : text.split("\\R")) {
                if (!line.isBlank()) {
                    doc.add(new Paragraph(line));
                }
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IOException("Unable to build fallback PDF", ex);
        }
    }
}
