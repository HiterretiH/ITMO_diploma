package com.logistic.backend.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads DOCX template bytes once at startup. Shared arrays are read-only for callers; {@link
 * DocxTemplateRenderer} must not mutate them.
 */
@Component
public class DocumentTemplateCache {

    private static final String TEMPLATE_DIR = "templates/documents/";

    private final Map<DocumentType, byte[]> templates;

    public DocumentTemplateCache() {
        EnumMap<DocumentType, byte[]> map = new EnumMap<>(DocumentType.class);
        try {
            for (DocumentType type : DocumentType.values()) {
                String path = TEMPLATE_DIR + fileName(type);
                map.put(type, readClasspathBytes(path));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load DOCX templates at startup", e);
        }
        this.templates = Map.copyOf(map);
    }

    public byte[] templateBytes(DocumentType type) {
        return templates.get(type);
    }

    static String fileName(DocumentType type) {
        return switch (type) {
            case CONTRACT_APPLICATION -> "contract_application.docx";
            case WAYBILL -> "waybill.docx";
            case ACT_OF_WORK -> "act_of_work.docx";
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
