package com.logistic.backend.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** In-memory cache of classpath PDF form templates (AcroForm). Field names match {@link PdfFormValuesBuilder} keys. */
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
