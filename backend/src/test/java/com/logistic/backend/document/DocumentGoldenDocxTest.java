package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentGoldenDocxTest {

    private static final List<String> DOCX_FILES =
            List.of(
                    "act_of_work_sample.docx",
                    "contract_application_sample.docx",
                    "waybill_sample.docx");

    @TempDir Path tempDir;

    @Test
    void generatedDocxMatchesApprovedGoldenDocuments() throws Exception {
        DocumentFixtureGenerator.main(new String[] {tempDir.toString()});
        DocxTemplateRenderer renderer = new DocxTemplateRenderer();
        Path goldenDir = Path.of("manual-review-docs");

        for (String file : DOCX_FILES) {
            Path generated = tempDir.resolve(file);
            Path golden = goldenDir.resolve(file);
            assertThat(Files.exists(generated))
                    .as("Generated file should exist: %s", generated)
                    .isTrue();
            assertThat(Files.exists(golden)).as("Golden file should exist: %s", golden).isTrue();

            String generatedText = normalize(renderer.extractText(Files.readAllBytes(generated)));
            String goldenText = normalize(renderer.extractText(Files.readAllBytes(golden)));

            assertThat(generatedText).isEqualTo(goldenText);
            assertThat(generatedText).doesNotContain("{{");
            assertThat(generatedText).doesNotContain("${");
        }
    }

    private static String normalize(String input) {
        return input.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
