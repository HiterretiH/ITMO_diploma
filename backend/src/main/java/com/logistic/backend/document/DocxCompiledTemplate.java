package com.logistic.backend.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Pre-parses a DOCX template into a literal/placeholder segment chain for {@code word/document.xml} and preserves
 * all other ZIP entries in order for fast {@link #render(Map)}.
 */
public final class DocxCompiledTemplate {

    public static final String DOCUMENT_XML_PATH = "word/document.xml";

    private static final Pattern BRACE = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");
    private static final Pattern DOLLAR = Pattern.compile("\\$\\{\\s*([^}]+?)\\s*\\}");

    public enum PlaceholderKind {
        BRACE,
        DOLLAR
    }

    public sealed interface Segment permits Segment.Literal, Segment.Placeholder {
        record Literal(String text) implements Segment {}

        record Placeholder(PlaceholderKind kind, String key, String raw) implements Segment {}
    }

    private sealed interface ZipSlot permits ZipSlot.Binary, ZipSlot.DocumentMarker {
        record Binary(String name, byte[] data, long time) implements ZipSlot {}

        record DocumentMarker(long time) implements ZipSlot {}
    }

    private record Region(int start, int end, String key, PlaceholderKind kind, String raw) {
        int span() {
            return end - start;
        }
    }

    private final List<ZipSlot> zipSlots;
    private final List<Segment> segments;

    private DocxCompiledTemplate(List<ZipSlot> zipSlots, List<Segment> segments) {
        this.zipSlots = List.copyOf(zipSlots);
        this.segments = List.copyOf(segments);
    }

    public static DocxCompiledTemplate compile(byte[] docxZip) throws IOException {
        List<ZipSlot> slots = new ArrayList<>();
        String documentXml = null;
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(docxZip))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                zin.closeEntry();
                String name = entry.getName();
                if (DOCUMENT_XML_PATH.equals(name)) {
                    documentXml = new String(data, StandardCharsets.UTF_8);
                    slots.add(new ZipSlot.DocumentMarker(entry.getTime()));
                } else {
                    slots.add(new ZipSlot.Binary(name, data, entry.getTime()));
                }
            }
        }
        if (documentXml == null) {
            throw new IOException("Missing " + DOCUMENT_XML_PATH + " in DOCX");
        }
        return new DocxCompiledTemplate(slots, buildSegments(documentXml));
    }

    private static List<Segment> buildSegments(String xml) {
        List<Region> raw = new ArrayList<>();
        Matcher mb = BRACE.matcher(xml);
        while (mb.find()) {
            raw.add(new Region(mb.start(), mb.end(), mb.group(1).strip(), PlaceholderKind.BRACE, mb.group()));
        }
        Matcher md = DOLLAR.matcher(xml);
        while (md.find()) {
            raw.add(new Region(md.start(), md.end(), md.group(1).strip(), PlaceholderKind.DOLLAR, md.group()));
        }
        raw.sort(Comparator.comparingInt(Region::start).thenComparing((a, b) -> Integer.compare(b.span(), a.span())));
        List<Region> merged = new ArrayList<>();
        int lastEnd = -1;
        for (Region r : raw) {
            if (r.start < lastEnd) {
                continue;
            }
            merged.add(r);
            lastEnd = r.end;
        }
        merged.sort(Comparator.comparingInt(Region::start));
        List<Segment> out = new ArrayList<>();
        int pos = 0;
        for (Region r : merged) {
            if (pos < r.start()) {
                out.add(new Segment.Literal(xml.substring(pos, r.start())));
            }
            out.add(new Segment.Placeholder(r.kind(), r.key(), r.raw()));
            pos = r.end();
        }
        if (pos < xml.length()) {
            out.add(new Segment.Literal(xml.substring(pos)));
        }
        return out;
    }

    /**
     * Materializes {@code word/document.xml} only; matches {@link DocxOpcXmlSubstitution#substituteXml} for the same
     * {@code context}.
     */
    public String materializeDocumentXml(Map<String, String> context) throws IOException {
        int est = 0;
        for (Segment seg : segments) {
            if (seg instanceof Segment.Literal l) {
                est += l.text().length();
            } else if (seg instanceof Segment.Placeholder p) {
                String v = context.get(p.key());
                est += v == null ? p.raw().length() : v.length() * 2;
            }
        }
        StringBuilder sb = new StringBuilder(Math.max(est, 256));
        for (Segment seg : segments) {
            if (seg instanceof Segment.Literal l) {
                sb.append(l.text());
            } else if (seg instanceof Segment.Placeholder p) {
                String v = context.get(p.key());
                if (v == null) {
                    sb.append(p.raw());
                } else {
                    sb.append(DocxOpcXmlSubstitution.escapeForWtText(v));
                }
            }
        }
        String result = sb.toString();
        DocxOpcXmlSubstitution.checkUnresolvedBraces(result);
        return result;
    }

    public byte[] render(Map<String, String> context) throws IOException {
        byte[] docBytes = materializeDocumentXml(context).getBytes(StandardCharsets.UTF_8);
        try (ByteArrayOutputStream rawOut = new ByteArrayOutputStream(docBytes.length + 65536);
                ZipOutputStream zout = new ZipOutputStream(rawOut)) {
            zout.setLevel(9);
            for (ZipSlot slot : zipSlots) {
                if (slot instanceof ZipSlot.Binary b) {
                    ZipEntry outEntry = new ZipEntry(b.name());
                    outEntry.setTime(b.time());
                    outEntry.setMethod(ZipEntry.DEFLATED);
                    zout.putNextEntry(outEntry);
                    zout.write(b.data());
                    zout.closeEntry();
                } else if (slot instanceof ZipSlot.DocumentMarker d) {
                    ZipEntry outEntry = new ZipEntry(DOCUMENT_XML_PATH);
                    outEntry.setTime(d.time());
                    outEntry.setMethod(ZipEntry.DEFLATED);
                    zout.putNextEntry(outEntry);
                    zout.write(docBytes);
                    zout.closeEntry();
                }
            }
            zout.finish();
            return rawOut.toByteArray();
        }
    }
}
