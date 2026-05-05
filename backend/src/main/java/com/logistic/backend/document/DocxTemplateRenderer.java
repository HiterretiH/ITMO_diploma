package com.logistic.backend.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

@Component
public class DocxTemplateRenderer {

    public byte[] render(byte[] template, Map<String, String> context) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(template))) {
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                replaceInParagraph(paragraph, context);
            }
            for (XWPFTable table : doc.getTables()) {
                replaceInTable(table, context);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    public String extractText(byte[] docxBody) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBody))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                if (!text.isBlank()) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(text);
                }
            }
            for (XWPFTable table : doc.getTables()) {
                appendTableText(sb, table);
            }
            return sb.toString();
        }
    }

    private static void replaceInTable(XWPFTable table, Map<String, String> context) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    replaceInParagraph(paragraph, context);
                }
                for (XWPFTable nested : cell.getTables()) {
                    replaceInTable(nested, context);
                }
            }
        }
    }

    private static void appendTableText(StringBuilder sb, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    String text = paragraph.getText();
                    if (!text.isBlank()) {
                        if (!sb.isEmpty()) {
                            sb.append('\n');
                        }
                        sb.append(text);
                    }
                }
                for (XWPFTable nested : cell.getTables()) {
                    appendTableText(sb, nested);
                }
            }
        }
    }

    private static void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> context) {
        String original = paragraph.getText();
        if (original == null || original.isBlank()) {
            return;
        }
        String replaced = applyContext(original, context);
        if (replaced.equals(original)) {
            return;
        }
        int size = paragraph.getRuns().size();
        for (int i = size - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        XWPFRun run = paragraph.createRun();
        run.setText(replaced, 0);
    }

    private static String applyContext(String source, Map<String, String> context) {
        String result = source;
        for (Map.Entry<String, String> e : context.entrySet()) {
            String value = e.getValue() == null ? "" : e.getValue();
            result = result.replace("${" + e.getKey() + "}", value);
            result = result.replace("{{" + e.getKey() + "}}", value);
            result = result.replace("{{ " + e.getKey() + " }}", value);
        }
        return result;
    }
}
