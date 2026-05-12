package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentTemplateCacheTest {

    @Test
    void loadsAllTemplatesWithDocxZipSignature() {
        DocumentTemplateCache cache = new DocumentTemplateCache();
        for (DocumentType type : DocumentType.values()) {
            byte[] bytes = cache.templateBytes(type);
            assertThat(bytes).isNotNull();
            assertThat(bytes.length).isGreaterThan(100);
            assertThat(bytes[0]).isEqualTo((byte) 'P');
            assertThat(bytes[1]).isEqualTo((byte) 'K');
            assertThat(cache.compiledTemplate(type)).isNotNull();
        }
    }
}
