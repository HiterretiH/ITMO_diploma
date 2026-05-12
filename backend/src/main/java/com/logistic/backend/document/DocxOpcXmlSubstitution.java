package com.logistic.backend.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Replaces {@code {{ key }}} (flexible whitespace) and {@code ${ key }} placeholders inside OOXML parts by
 * rewriting ZIP entry bytes, matching {@code utilities/check_docx_structure/generate_docx_xml_substitution.py}.
 *
 * <p>Classpath templates were audited: placeholders appear only in {@code word/document.xml}; other OPC parts are
 * copied verbatim.
 */
final class DocxOpcXmlSubstitution {

    /**
     * OOXML parts whose UTF-8 XML may contain {@code {{...}}} / {@code ${...}}} placeholders. Extend only if templates
     * gain placeholders in headers, footers, etc.
     */
    private static final Set<String> SUBSTITUTABLE_PARTS = Set.of("word/document.xml");

    private static final Pattern UNRESOLVED_BRACES =
            Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");

    private DocxOpcXmlSubstitution() {}

    static byte[] render(byte[] template, Map<String, String> context) throws IOException {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(template);
                ZipInputStream zin = new ZipInputStream(bin);
                ByteArrayOutputStream rawOut = new ByteArrayOutputStream(template.length + 4096);
                ZipOutputStream zout = new ZipOutputStream(rawOut)) {
            zout.setLevel(9);
            ZipEntry inEntry;
            while ((inEntry = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                zin.closeEntry();
                String name = inEntry.getName();
                if (SUBSTITUTABLE_PARTS.contains(name)) {
                    String xml = new String(data, StandardCharsets.UTF_8);
                    xml = substituteXml(xml, context);
                    data = xml.getBytes(StandardCharsets.UTF_8);
                }
                ZipEntry outEntry = new ZipEntry(name);
                outEntry.setTime(inEntry.getTime());
                outEntry.setMethod(ZipEntry.DEFLATED);
                zout.putNextEntry(outEntry);
                zout.write(data);
                zout.closeEntry();
            }
            zout.finish();
            return rawOut.toByteArray();
        }
    }

    /**
     * Same rules as Python {@code escape_for_wt_text}: XML-escape then newline to {@code &#10;}.
     */
    static String escapeForWtText(String value) {
        if (value == null) {
            return "";
        }
        String t = value.replace("\r\n", "\n").replace("\r", "\n");
        StringBuilder sb = new StringBuilder(t.length() + 16);
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                case '\n' -> sb.append("&#10;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    static String substituteXml(String xml, Map<String, String> context) throws IOException {
        String out = xml;
        List<String> keys = new ArrayList<>(context.keySet());
        keys.sort(Comparator.comparingInt(String::length).reversed());
        for (String key : keys) {
            String val = context.get(key);
            if (val == null) {
                continue;
            }
            Pattern pat = Pattern.compile("\\{\\{\\s*" + Pattern.quote(key) + "\\s*\\}\\}");
            out = pat.matcher(out).replaceAll(Matcher.quoteReplacement(escapeForWtText(val)));
        }
        for (String key : keys) {
            String val = context.get(key);
            if (val == null) {
                continue;
            }
            Pattern pat = Pattern.compile("\\$\\{\\s*" + Pattern.quote(key) + "\\s*\\}");
            out = pat.matcher(out).replaceAll(Matcher.quoteReplacement(escapeForWtText(val)));
        }
        List<String> unresolved = new ArrayList<>();
        Matcher m = UNRESOLVED_BRACES.matcher(out);
        while (m.find()) {
            unresolved.add(m.group(1).strip());
        }
        if (!unresolved.isEmpty()) {
            throw new IOException("Unresolved DOCX placeholders in document.xml: " + unresolved);
        }
        return out;
    }
}
