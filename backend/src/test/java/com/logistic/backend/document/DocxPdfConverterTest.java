package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

class DocxPdfConverterTest {

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachConverterLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(DocxPdfConverter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void detachConverterLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(DocxPdfConverter.class);
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void renderedDocxConvertsToLayoutPdfWithCyrillicText() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> context = DocumentGenerationService.snapshotToContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        byte[] templateBytes;
        try (InputStream in =
                new ClassPathResource("templates/documents/contract_application.docx").getInputStream()) {
            templateBytes = in.readAllBytes();
        }
        byte[] docx = renderer.render(templateBytes, context);

        DocxPdfConverter converter = new DocxPdfConverter();
        byte[] pdf = converter.convert(docx);

        assertThat(pdf.length).isGreaterThan(20);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertNoPlainTextFallbackLog();

        try (PDDocument pd = Loader.loadPDF(pdf)) {
            assertThat(pd.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pd);
            assertThat(text).contains("ООО Ромашка");
            assertThat(text).contains("Иванов Иван Иванович");
            assertThat(text).contains("Москва");
            assertThat(text).contains("Санкт-Петербург");
            assertThat(text).doesNotContain("{{");
            assertThat(text).doesNotContain("${");
        }
    }

    @Test
    void allDocumentTemplatesConvertToPdfWithoutFallback() throws Exception {
        OrderPrintSnapshot snapshot = DocumentFixtureGenerator.sampleSnapshot();
        Map<String, String> context = DocumentGenerationService.snapshotToContext(snapshot);
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        DocxPdfConverter converter = new DocxPdfConverter();
        for (String template :
                List.of(
                        "templates/documents/contract_application.docx",
                        "templates/documents/waybill.docx",
                        "templates/documents/act_of_work.docx")) {
            byte[] templateBytes;
            try (InputStream in = new ClassPathResource(template).getInputStream()) {
                templateBytes = in.readAllBytes();
            }
            byte[] docx = renderer.render(templateBytes, context);
            byte[] pdf = converter.convert(docx);
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
            try (PDDocument pd = Loader.loadPDF(pdf)) {
                assertThat(pd.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            }
        }
        assertNoPlainTextFallbackLog();
    }

    private void assertNoPlainTextFallbackLog() {
        boolean bad =
                logAppender.list.stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .anyMatch(m -> m.contains("plain text fallback"));
        assertThat(bad).as("DocxPdfConverter must not use plain text PDF fallback").isFalse();
    }
}
