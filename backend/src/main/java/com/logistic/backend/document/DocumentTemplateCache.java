package com.logistic.backend.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads DOCX template bytes once at startup. Shared arrays are read-only for callers; {@link
 * DocxTemplateRenderer} must not mutate them. Pre-compiled {@link DocxCompiledTemplate} instances share the same
 * template bytes and speed up {@code word/document.xml} substitution.
 */
@Component
public class DocumentTemplateCache {

    private static final String TEMPLATE_DIR = "templates/documents/";

    private final Map<DocumentType, byte[]> templates;
    private final Map<DocumentType, DocxCompiledTemplate> compiledTemplates;

    public DocumentTemplateCache() {
        EnumMap<DocumentType, byte[]> map = new EnumMap<>(DocumentType.class);
        EnumMap<DocumentType, DocxCompiledTemplate> compiled = new EnumMap<>(DocumentType.class);
        try {
            for (DocumentType type : DocumentType.values()) {
                String path = TEMPLATE_DIR + fileName(type);
                byte[] bytes = readClasspathBytes(path);
                map.put(type, bytes);
                compiled.put(type, DocxCompiledTemplate.compile(bytes));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load DOCX templates at startup", e);
        }
        this.templates = Map.copyOf(map);
        this.compiledTemplates = Map.copyOf(compiled);
    }

    public byte[] templateBytes(DocumentType type) {
        return templates.get(type);
    }

    public DocxCompiledTemplate compiledTemplate(DocumentType type) {
        return compiledTemplates.get(type);
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
