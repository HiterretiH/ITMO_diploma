package com.logistic.backend.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

@Component
public class DocxTemplateRenderer {

    public byte[] render(byte[] template, Map<String, String> context) throws IOException {
        return DocxOpcXmlSubstitution.render(template, context);
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
}
