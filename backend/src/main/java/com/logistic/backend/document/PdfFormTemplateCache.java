package com.logistic.backend.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads PDF form template bytes once at startup. Field names match {@code field_name} in
 * {@code utilities/docx_to_pdf_template/config.py}; classpath PDFs are produced from {@code *.form.docx} by
 * {@code utilities/docx_to_pdf_template/run.py} (Word export and AcroForm placement).
 */
@Component
public class PdfFormTemplateCache {

    public static final String CLASSPATH_DIR = "templates/documents/";

    private final Map<DocumentType, byte[]> templates;

    public PdfFormTemplateCache() {
        EnumMap<DocumentType, byte[]> map = new EnumMap<>(DocumentType.class);
        try {
            for (DocumentType type : DocumentType.values()) {
                String path = CLASSPATH_DIR + fileName(type);
                map.put(type, readClasspathBytes(path));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load PDF form templates at startup", e);
        }
        this.templates = Map.copyOf(map);
    }

    public byte[] templateBytes(DocumentType type) {
        return templates.get(type);
    }

    static String fileName(DocumentType type) {
        return switch (type) {
            case CONTRACT_APPLICATION -> "contract_application.pdf";
            case WAYBILL -> "waybill.pdf";
            case ACT_OF_WORK -> "act_of_work.pdf";
        };
    }

    private static byte[] readClasspathBytes(String classpathLocation) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            throw new IOException("Missing classpath resource: " + classpathLocation);
        }
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
