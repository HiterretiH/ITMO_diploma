package com.logistic.backend.document;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.Docx4J;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocxPdfConverter {

    private static final String[] FONT_ALIASES = {
        "Times New Roman",
        "Times",
        "Arial",
        "Arial Cyr",
        "Calibri",
        "Cambria",
        "Cambria Math",
        "Verdana",
        "Tahoma",
        "Courier New",
        "Segoe UI",
        "MS Sans Serif",
        "MS Serif",
        "Helvetica",
        "sans-serif",
        "Serif",
        "Roboto",
        "Liberation Sans",
        "DejaVu Sans",
        "Symbol",
        "ZapfDingbats",
        "Wingdings",
        "Webdings"
    };

    private static final String REGULAR_FONT_RESOURCE = "fonts/LiberationSans-Regular.ttf";
    private static final String BOLD_FONT_RESOURCE = "fonts/LiberationSans-Bold.ttf";

    private final DocxTemplateRenderer templateRenderer = new DocxTemplateRenderer();

    private static volatile Path fontWorkDir;
    private static final Object FONT_LOCK = new Object();

    public byte[] convert(byte[] renderedDocx) throws IOException {
        ensureBundledPhysicalFonts();
        try (ByteArrayInputStream in = new ByteArrayInputStream(renderedDocx);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.load(in);
            Docx4J.toPDF(pkg, out);
            byte[] pdf = out.toByteArray();
            if (pdf.length >= 5 && pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F') {
                return pdf;
            }
            log.error("DOCX->PDF produced non-PDF output ({} bytes), using plain text fallback", pdf.length);
            return renderPlainTextPdf(renderedDocx);
        } catch (Exception e) {
            log.error("DOCX->PDF conversion failed, using plain text fallback: {}", e.getMessage(), e);
            return renderPlainTextPdf(renderedDocx);
        }
    }

    private static void ensureBundledPhysicalFonts() throws IOException {
        if (fontWorkDir != null) {
            return;
        }
        synchronized (FONT_LOCK) {
            if (fontWorkDir != null) {
                return;
            }
            Path dir = Files.createTempDirectory("logistic-docx4j-fonts");
            Path regular = extractFont(REGULAR_FONT_RESOURCE, dir);
            Path bold = extractFont(BOLD_FONT_RESOURCE, dir);
            PhysicalFonts.addPhysicalFont(regular.toUri());
            PhysicalFonts.addPhysicalFont(bold.toUri());
            PhysicalFont regularPf = resolveBundledRegular();
            if (regularPf != null) {
                for (String alias : FONT_ALIASES) {
                    PhysicalFonts.put(alias, regularPf);
                }
            } else {
                for (String alias : FONT_ALIASES) {
                    PhysicalFonts.addPhysicalFonts(alias, regular.toUri());
                }
            }
            fontWorkDir = dir;
            log.debug("Registered bundled Liberation Sans for PDF (dir={})", dir);
        }
    }

    private static Path extractFont(String classpathLocation, Path dir) throws IOException {
        ClassPathResource res = new ClassPathResource(classpathLocation);
        if (!res.exists()) {
            throw new IOException("Missing classpath font: " + classpathLocation);
        }
        String name = Path.of(classpathLocation).getFileName().toString();
        Path target = dir.resolve(name);
        try (InputStream in = Objects.requireNonNull(res.getInputStream(), "font stream")) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static PhysicalFont resolveBundledRegular() {
        for (String key : new String[] {"liberation sans", "liberation sans regular"}) {
            PhysicalFont pf = PhysicalFonts.get(key);
            if (pf != null) {
                return pf;
            }
        }
        return null;
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
