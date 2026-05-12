package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DocxCompiledTemplateTest {

    @Test
    void materializeDocumentXml_matchesSubstituteXml_onSyntheticXml() throws Exception {
        String xml = "<root>Hi {{  shipperName  }}! ${ routeFrom }</root>";
        Map<String, String> ctx =
                Map.of(
                        "shipperName", "ACME",
                        "routeFrom", "Moscow<x>",
                        "a", "X",
                        "ab", "Y");
        String expected = DocxOpcXmlSubstitution.substituteXml(xml, ctx);
        DocxCompiledTemplate compiled = DocxCompiledTemplate.compile(minimalDocxZip(xml));
        assertThat(compiled.materializeDocumentXml(ctx)).isEqualTo(expected);
    }

    @Test
    void materializeDocumentXml_matchesSubstituteXml_longestKeyFirst() throws Exception {
        String xml = "<w>P{{ab}}Q{{a}}R</w>";
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("a", "X");
        ctx.put("ab", "Y");
        String expected = DocxOpcXmlSubstitution.substituteXml(xml, ctx);
        DocxCompiledTemplate compiled = DocxCompiledTemplate.compile(minimalDocxZip(xml));
        assertThat(compiled.materializeDocumentXml(ctx)).isEqualTo(expected);
    }

    @Test
    void materializeDocumentXml_throwsOnUnresolvedLikeSubstituteXml() throws Exception {
        String xml = "<w>{{left}} {{ orphan }}</w>";
        Map<String, String> ctx = Map.of("left", "OK");
        assertThatThrownBy(() -> DocxOpcXmlSubstitution.substituteXml(xml, ctx))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("orphan");
        DocxCompiledTemplate compiled = DocxCompiledTemplate.compile(minimalDocxZip(xml));
        assertThatThrownBy(() -> compiled.materializeDocumentXml(ctx))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("orphan");
    }

    @Test
    void materializeDocumentXml_matchesSubstituteXml_forEachClasspathTemplate() throws Exception {
        OrderPrintSnapshot snap = OrderPrintSnapshots.manualReviewDemo();
        Map<String, String> ctx = DocumentGenerationService.snapshotToContext(snap);
        for (DocumentType type : DocumentType.values()) {
            byte[] tpl = readTemplateBytes(type);
            String xml = readDocumentXml(tpl);
            String expected = DocxOpcXmlSubstitution.substituteXml(xml, ctx);
            DocxCompiledTemplate compiled = DocxCompiledTemplate.compile(tpl);
            assertThat(compiled.materializeDocumentXml(ctx))
                    .as(type.name())
                    .isEqualTo(expected);
        }
    }

    @Test
    void render_documentXml_matchesSubstituteXml_onClasspathTemplates() throws Exception {
        OrderPrintSnapshot snap = OrderPrintSnapshots.manualReviewDemo();
        Map<String, String> ctx = DocumentGenerationService.snapshotToContext(snap);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        for (DocumentType type : DocumentType.values()) {
            byte[] tpl = readTemplateBytes(type);
            String xmlIn = readDocumentXml(tpl);
            String expectedDocXml = DocxOpcXmlSubstitution.substituteXml(xmlIn, ctx);
            DocxCompiledTemplate compiled = DocxCompiledTemplate.compile(tpl);
            byte[] out = compiled.render(ctx);
            assertThat(readDocumentXml(out)).as("document.xml " + type).isEqualTo(expectedDocXml);
            assertThat(renderer.extractText(out)).as("extractText " + type).isNotBlank();
        }
    }

    private static byte[] readTemplateBytes(DocumentType type) throws IOException {
        String name = DocumentTemplateCache.fileName(type);
        ClassPathResource res = new ClassPathResource("templates/documents/" + name);
        try (InputStream in = res.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static String readDocumentXml(byte[] docxZip) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(docxZip))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (DocxCompiledTemplate.DOCUMENT_XML_PATH.equals(e.getName())) {
                    return new String(zin.readAllBytes(), StandardCharsets.UTF_8);
                }
                zin.closeEntry();
            }
        }
        throw new IOException("missing document.xml");
    }

    /**
     * Minimal valid-enough DOCX ZIP for compile(): only {@code word/document.xml} (some tools require more parts;
     * compile() only needs this entry for segment tests).
     */
    private static byte[] minimalDocxZip(String documentXml) throws IOException {
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                java.util.zip.ZipOutputStream zout = new java.util.zip.ZipOutputStream(bos)) {
            zout.putNextEntry(new ZipEntry(DocxCompiledTemplate.DOCUMENT_XML_PATH));
            zout.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zout.closeEntry();
            zout.finish();
            return bos.toByteArray();
        }
    }
}
