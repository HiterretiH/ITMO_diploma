package com.logistic.backend.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistic.backend.config.StorageProperties;
import com.logistic.backend.trip.Trip;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
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
    private final DocxPdfConverter docxPdfConverter;

    @Transactional
    public void generateAndPersist(Trip trip) throws IOException {
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
                byte[] body =
                        ff == FileFormat.DOCX ? renderedDocx : docxPdfConverter.convert(renderedDocx);
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

    private static Map<String, String> snapshotToContext(TripPrintSnapshot s) {
        Map<String, String> ctx = new LinkedHashMap<>();

        String number = asString(s.tripId());
        String date = asString(s.loadDate());
        String wordDate = formatRuDate(s.loadDate());
        String loadingPlace = asString(s.routeFrom());
        String unloadingPlace = asString(s.routeTo());
        String count = "1";
        String price = money(s.priceAmount());
        String totalPrice = money(s.priceAmount());
        String wordPrice = totalPrice + " рублей";
        String performerInfoWs =
                String.join(
                                ", ",
                                asString(s.ownerUsername()),
                                asString(s.vehiclePlate()),
                                asString(s.driverName()),
                                asString(s.driverLicense()))
                        .replaceAll("(,\\s*)+", ", ")
                        .replaceAll("^,\\s*|,\\s*$", "");
        String customerInfoWs =
                String.join(
                                ", ",
                                asString(s.shipperName()),
                                "ИНН " + asString(s.shipperInn()),
                                asString(s.shipperAddress()))
                        .replaceAll("(,\\s*)+", ", ")
                        .replaceAll("^,\\s*|,\\s*$", "");

        ctx.put("number", number);
        ctx.put("date", date);
        ctx.put("word_date", wordDate);
        ctx.put("loading_place", loadingPlace);
        ctx.put("unloading_place", unloadingPlace);
        ctx.put("count", count);
        ctx.put("price", price);
        ctx.put("total_price", totalPrice);
        ctx.put("word_price", wordPrice);
        ctx.put("performer_info_ws", performerInfoWs);
        ctx.put("customer_info_ws", customerInfoWs);

        ctx.put("performer_name", asString(s.ownerUsername()));
        ctx.put("performer_full_name", asString(s.ownerUsername()));
        ctx.put("performer_info", performerInfoWs);
        ctx.put("performer_phone", "");
        ctx.put("performer_bank", "");
        ctx.put("performer_vehicle", asString(s.vehicleModel()));
        ctx.put("performer_vehicle_number", asString(s.vehiclePlate()));
        ctx.put("performer_driver", asString(s.driverName()));
        ctx.put("performer_driver_phone", "");
        ctx.put("performer_vehicle_type", asString(s.vehicleCapacityKg()));
        ctx.put("performer_inn", "");
        ctx.put("performer_bik", "");
        ctx.put("performer_kpp", "");
        ctx.put("performer_rsh", "");
        ctx.put("performer_ksh", "");

        ctx.put("customer_name", asString(s.shipperName()));
        ctx.put("customer_full_name", asString(s.shipperName()));
        ctx.put("customer_info", customerInfoWs);
        ctx.put("customer_phone", "");
        ctx.put("customer_bank", "");

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

    private static String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String formatRuDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        String month =
                switch (date.getMonthValue()) {
                    case 1 -> "января";
                    case 2 -> "февраля";
                    case 3 -> "марта";
                    case 4 -> "апреля";
                    case 5 -> "мая";
                    case 6 -> "июня";
                    case 7 -> "июля";
                    case 8 -> "августа";
                    case 9 -> "сентября";
                    case 10 -> "октября";
                    case 11 -> "ноября";
                    case 12 -> "декабря";
                    default -> "";
                };
        return String.format("%02d %s %dг.", date.getDayOfMonth(), month, date.getYear());
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
