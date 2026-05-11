package com.logistic.backend.document;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * CLI: dump AcroForm widget rectangles from a PDF (OpenPDF). Compare with {@link PdfFormLayout} slots when tuning
 * templates. Usage: {@code PdfFieldBoundsDump <input.pdf> <output.txt>}
 */
public final class PdfFieldBoundsDump {

    private PdfFieldBoundsDump() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: PdfFieldBoundsDump <input.pdf> <output.txt>");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Files.createDirectories(out.getParent());
        String text = dump(Files.readAllBytes(in));
        Files.writeString(out, text, StandardCharsets.UTF_8);
        System.out.println("Wrote " + out);
    }

    static String dump(byte[] pdfBytes) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("AcroForm field rectangles (OpenPDF getFieldPositions, user space).\n\n");
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            AcroFields af = reader.getAcroFields();
            if (af == null || af.getAllFields() == null || af.getAllFields().isEmpty()) {
                sb.append("(no AcroForm fields)\n");
                return sb.toString();
            }
            @SuppressWarnings("unchecked")
            Map<String, ?> all = (Map<String, ?>) af.getAllFields();
            for (String name : new TreeMap<>(all).keySet()) {
                float[] pos = af.getFieldPositions(name);
                if (pos == null || pos.length == 0) {
                    sb.append(name).append(" (no positions)\n");
                    continue;
                }
                for (int i = 0; i + 4 < pos.length; i += 5) {
                    sb.append(String.format(
                            "%s page=%d llx=%.2f lly=%.2f urx=%.2f ury=%.2f%n",
                            name, (int) pos[i], pos[i + 1], pos[i + 2], pos[i + 3], pos[i + 4]));
                }
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }
}
