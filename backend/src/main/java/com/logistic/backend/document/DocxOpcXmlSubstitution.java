package com.logistic.backend.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XML placeholder substitution and escaping used by {@link DocxCompiledTemplate} tests and legacy comparison via
 * {@link #substituteXml}; ZIP assembly lives in {@link DocxCompiledTemplate#render(Map)}.
 */
final class DocxOpcXmlSubstitution {

    private static final Pattern UNRESOLVED_BRACES =
            Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");

    private DocxOpcXmlSubstitution() {}

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
        checkUnresolvedBraces(out);
        return out;
    }

    static void checkUnresolvedBraces(String out) throws IOException {
        List<String> unresolved = new ArrayList<>();
        Matcher m = UNRESOLVED_BRACES.matcher(out);
        while (m.find()) {
            unresolved.add(m.group(1).strip());
        }
        if (!unresolved.isEmpty()) {
            throw new IOException("Unresolved DOCX placeholders in document.xml: " + unresolved);
        }
    }
}
