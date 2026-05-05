package com.logistic.backend.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistic.backend.config.StorageProperties;
import com.logistic.backend.trip.Trip;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private final ObjectMapper objectMapper;
    private final StorageProperties storageProperties;
    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final DocxTemplateRenderer templateRenderer;

    @Transactional
    public void generateAndPersist(Trip trip) throws IOException, DocumentException {
        TripPrintSnapshot snap =
                objectMapper.readValue(trip.getSnapshotJson(), TripPrintSnapshot.class);
        Path base =
                Path.of(storageProperties.getRoot())
                        .resolve("trips")
                        .resolve(trip.getId().toString());
        Files.createDirectories(base);
        generatedDocumentRepository.deleteByTrip(trip);

        for (DocumentType dt : DocumentType.values()) {
            byte[] renderedDocx = renderDocxFromTemplate(dt, snap);
            for (FileFormat ff : FileFormat.values()) {
                byte[] body = ff == FileFormat.DOCX ? renderedDocx : renderPdf(renderedDocx);
                String ext = ff == FileFormat.PDF ? "pdf" : "docx";
                String fileName = dt.name().toLowerCase() + "_" + ff.name().toLowerCase() + "." + ext;
                Path path = base.resolve(fileName);
                Files.write(path, body);

                GeneratedDocument gd = new GeneratedDocument();
                gd.setTrip(trip);
                gd.setDocumentType(dt);
                gd.setFileFormat(ff);
                gd.setStoragePath(path.toAbsolutePath().toString());
                gd.setContentSha256(sha256Hex(body));
                generatedDocumentRepository.save(gd);
            }
        }
    }

    private byte[] renderDocxFromTemplate(DocumentType type, TripPrintSnapshot snapshot)
            throws IOException {
        String resourcePath = "templates/documents/" + templateName(type);
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            return templateRenderer.render(in.readAllBytes(), snapshotToContext(snapshot));
        }
    }

    private static String templateName(DocumentType type) {
        return switch (type) {
            case CONTRACT_APPLICATION -> "contract_application.docx";
            case WAYBILL -> "waybill.docx";
            case ACT_OF_WORK -> "act_of_work.docx";
        };
    }

    private byte[] renderPdf(byte[] renderedDocx) throws DocumentException, IOException {
        String text = templateRenderer.extractText(renderedDocx);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, baos);
        doc.open();
        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            doc.add(new Paragraph(line));
        }
        doc.close();
        return baos.toByteArray();
    }

    private static Map<String, String> snapshotToContext(TripPrintSnapshot s) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("tripId", asString(s.tripId()));
        ctx.put("ownerUsername", asString(s.ownerUsername()));
        ctx.put("shipperName", asString(s.shipperName()));
        ctx.put("shipperInn", asString(s.shipperInn()));
        ctx.put("shipperAddress", asString(s.shipperAddress()));
        ctx.put("consigneeName", asString(s.consigneeName()));
        ctx.put("consigneeInn", asString(s.consigneeInn()));
        ctx.put("consigneeAddress", asString(s.consigneeAddress()));
        ctx.put("cargoDescription", asString(s.cargoDescription()));
        ctx.put("cargoWeightKg", asString(s.cargoWeightKg()));
        ctx.put("routeFrom", asString(s.routeFrom()));
        ctx.put("routeTo", asString(s.routeTo()));
        ctx.put("loadDate", asString(s.loadDate()));
        ctx.put("unloadDate", asString(s.unloadDate()));
        ctx.put("driverName", asString(s.driverName()));
        ctx.put("driverLicense", asString(s.driverLicense()));
        ctx.put("vehiclePlate", asString(s.vehiclePlate()));
        ctx.put("vehicleModel", asString(s.vehicleModel()));
        ctx.put("vehicleCapacityKg", asString(s.vehicleCapacityKg()));
        ctx.put("priceAmount", asString(s.priceAmount()));
        ctx.put("currency", asString(s.currency()));
        return ctx;
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
