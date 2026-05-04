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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private final ObjectMapper objectMapper;
    private final StorageProperties storageProperties;
    private final GeneratedDocumentRepository generatedDocumentRepository;

    @Transactional
    public void generateAndPersist(Trip trip) throws IOException, DocumentException {
        TripPrintSnapshot snap =
                objectMapper.readValue(trip.getSnapshotJson(), TripPrintSnapshot.class);
        Path base =
                Path.of(storageProperties.getRoot())
                        .resolve("trips")
                        .resolve(trip.getId().toString());
        Files.createDirectories(base);

        for (DocumentType dt : DocumentType.values()) {
            for (FileFormat ff : FileFormat.values()) {
                byte[] body = render(dt, ff, snap);
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

    private byte[] render(DocumentType type, FileFormat format, TripPrintSnapshot s)
            throws IOException, DocumentException {
        List<String> lines =
                switch (type) {
                    case CONTRACT_APPLICATION -> contractLines(s);
                    case WAYBILL -> waybillLines(s);
                    case ACT_OF_WORK -> actLines(s);
                };
        if (format == FileFormat.PDF) {
            return renderPdf(lines);
        }
        return renderDocx(titleFor(type), lines);
    }

    private static String titleFor(DocumentType type) {
        return switch (type) {
            case CONTRACT_APPLICATION -> "Договор-заявка на перевозку груза";
            case WAYBILL -> "Транспортная накладная (упрощённая форма)";
            case ACT_OF_WORK -> "Акт выполненных работ";
        };
    }

    private static List<String> contractLines(TripPrintSnapshot s) {
        return List.of(
                "Договор-заявка на организацию перевозки груза автомобильным транспортом",
                "Исполнитель (перевозчик): " + nullToEmpty(s.ownerUsername()),
                "Заказчик (грузоотправитель): " + nullToEmpty(s.shipperName()) + ", ИНН: " + nullToEmpty(s.shipperInn()),
                "Адрес: " + nullToEmpty(s.shipperAddress()),
                "Грузополучатель: " + nullToEmpty(s.consigneeName()) + ", ИНН: " + nullToEmpty(s.consigneeInn()),
                "Адрес: " + nullToEmpty(s.consigneeAddress()),
                "Описание груза: " + nullToEmpty(s.cargoDescription()),
                "Масса груза, кг: " + (s.cargoWeightKg() != null ? s.cargoWeightKg().toPlainString() : ""),
                "Маршрут: " + nullToEmpty(s.routeFrom()) + " — " + nullToEmpty(s.routeTo()),
                "Погрузка: " + (s.loadDate() != null ? s.loadDate().toString() : "") + ", разгрузка: "
                        + (s.unloadDate() != null ? s.unloadDate().toString() : ""),
                "Транспорт: " + nullToEmpty(s.vehiclePlate()) + ", " + nullToEmpty(s.vehicleModel()),
                "Водитель: " + nullToEmpty(s.driverName()) + ", удостоверение: " + nullToEmpty(s.driverLicense()),
                "Стоимость услуг: " + (s.priceAmount() != null ? s.priceAmount().toPlainString() : "") + " "
                        + nullToEmpty(s.currency()));
    }

    private static List<String> waybillLines(TripPrintSnapshot s) {
        return List.of(
                "Транспортная накладная",
                "Рейс № " + s.tripId(),
                "Грузоотправитель: " + nullToEmpty(s.shipperName()),
                "Грузополучатель: " + nullToEmpty(s.consigneeName()),
                "Наименование груза: " + nullToEmpty(s.cargoDescription()),
                "Масса: " + (s.cargoWeightKg() != null ? s.cargoWeightKg().toPlainString() : ""),
                "Пункт отправления: " + nullToEmpty(s.routeFrom()),
                "Пункт назначения: " + nullToEmpty(s.routeTo()),
                "Дата отправления: " + (s.loadDate() != null ? s.loadDate().toString() : ""),
                "Дата прибытия: " + (s.unloadDate() != null ? s.unloadDate().toString() : ""),
                "Автомобиль: " + nullToEmpty(s.vehiclePlate()),
                "Водитель: " + nullToEmpty(s.driverName()));
    }

    private static List<String> actLines(TripPrintSnapshot s) {
        return List.of(
                "Акт выполненных работ (оказанных услуг)",
                "Рейс № " + s.tripId(),
                "Заказчик: " + nullToEmpty(s.shipperName()),
                "Исполнитель: " + nullToEmpty(s.ownerUsername()),
                "Услуга: перевозка груза по маршруту " + nullToEmpty(s.routeFrom()) + " — " + nullToEmpty(s.routeTo()),
                "Стоимость: " + (s.priceAmount() != null ? s.priceAmount().toPlainString() : "") + " "
                        + nullToEmpty(s.currency()),
                "Груз сдал представитель грузоотправителя _________________",
                "Груз принял водитель " + nullToEmpty(s.driverName()) + " _________________");
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static byte[] renderPdf(List<String> lines) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, baos);
        doc.open();
        for (String line : lines) {
            doc.add(new Paragraph(line));
        }
        doc.close();
        return baos.toByteArray();
    }

    private static byte[] renderDocx(String title, List<String> lines) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph t = doc.createParagraph();
            XWPFRun tr = t.createRun();
            tr.setBold(true);
            tr.setText(title);
            for (String line : lines) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun r = p.createRun();
                r.setText(line);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
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
