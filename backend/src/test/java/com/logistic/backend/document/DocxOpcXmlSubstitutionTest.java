package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DocxOpcXmlSubstitutionTest {

    @Test
    void escapeForWtText_escapesXmlAndLineBreaks() {
        assertThat(DocxOpcXmlSubstitution.escapeForWtText("a<b>&\"'\nc"))
                .isEqualTo("a&lt;b&gt;&amp;&quot;&apos;&#10;c");
    }

    @Test
    void substituteXml_replacesLongestKeysFirst() throws Exception {
        String xml = "<w:t>P{{ab}}Q{{a}}R</w:t>";
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("a", "X");
        ctx.put("ab", "Y");
        assertThat(DocxOpcXmlSubstitution.substituteXml(xml, ctx)).isEqualTo("<w:t>PYQXR</w:t>");
    }

    @Test
    void substituteXml_allowsFlexibleWhitespaceInBraces() throws Exception {
        String xml = "<w:t>Hi {{  shipperName  }}!</w:t>";
        Map<String, String> ctx = Map.of("shipperName", "ACME");
        assertThat(DocxOpcXmlSubstitution.substituteXml(xml, ctx)).isEqualTo("<w:t>Hi ACME!</w:t>");
    }

    @Test
    void substituteXml_replacesDollarBracePlaceholders() throws Exception {
        String xml = "<w:t>${ routeFrom }</w:t>";
        Map<String, String> ctx = Map.of("routeFrom", "Moscow");
        assertThat(DocxOpcXmlSubstitution.substituteXml(xml, ctx)).isEqualTo("<w:t>Moscow</w:t>");
    }

    @Test
    void substituteXml_throwsWhenBracesRemain() {
        String xml = "<w:t>{{left}} {{ orphan }}</w:t>";
        Map<String, String> ctx = Map.of("left", "OK");
        assertThatThrownBy(() -> DocxOpcXmlSubstitution.substituteXml(xml, ctx))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("orphan");
    }
}
