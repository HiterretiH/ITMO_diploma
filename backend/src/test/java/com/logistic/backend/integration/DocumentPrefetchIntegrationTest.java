package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.OrderCreateRequest;
import com.logistic.backend.api.dto.PerformerRequest;
import com.logistic.backend.api.dto.VehicleRequest;
import com.logistic.backend.document.DocumentGenerationService;
import com.logistic.backend.document.DocumentType;
import com.logistic.backend.document.FileFormat;
import com.logistic.backend.integration.support.DocumentGenerationSpyConfiguration;
import com.logistic.backend.order.Order;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(DocumentGenerationSpyConfiguration.class)
class DocumentPrefetchIntegrationTest extends AbstractPostgresIntegrationTest {

    /** {@link DocumentType} × {@link com.logistic.backend.document.FileFormat} prefetch pairs. */
    private static final int PREFETCH_GENERATION_CALLS = 6;

    @Autowired private DocumentGenerationService documentGenerationService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String employeeToken;

    @BeforeEach
    void seedUser() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String empName = "prefetch_" + id;
        User emp = new User();
        emp.setUsername(empName);
        emp.setPasswordHash(passwordEncoder.encode("pass"));
        emp.setEnabled(true);
        emp.setRoles(EnumSet.of(Role.USER));
        userRepository.save(emp);
        employeeToken = loginToken(empName, "pass");
    }

    @Test
    void fullCreate_triggersAsyncPrefetchDocumentGeneration() throws Exception {
        Long customerId =
                postCustomer(
                        employeeToken,
                        new CustomerRequest("Ship LLC", "Ship Full", "+1", "ИНН 123"));
        Long performerId =
                postPerformer(
                        employeeToken,
                        new PerformerRequest(
                                "Perf LLC",
                                "Perf Full",
                                "+2",
                                "Bank",
                                "7701234567",
                                "044525225",
                                "770101001",
                                "40702810000000000001",
                                "30101810400000000225",
                                "г. Москва"));
        Long driverId =
                postDriver(
                        employeeToken,
                        new DriverRequest(performerId, "Ivan Ivanov", "+79001234567"));
        Long vehicleId =
                postVehicle(
                        employeeToken,
                        new VehicleRequest(performerId, "GAZelle", "A123BC77", "фургон"));

        OrderCreateRequest fullCreate =
                new OrderCreateRequest(
                        customerId,
                        performerId,
                        vehicleId,
                        driverId,
                        LocalDate.of(2026, 6, 1),
                        null,
                        "Москва, склад 1",
                        "Петров +7",
                        "Тверь, база 2",
                        "Сидоров +7",
                        2,
                        new BigDecimal("22500.00"),
                        new BigDecimal("45000.00"));

        ResponseEntity<String> createOrder =
                restTemplate.postForEntity(
                        "/api/v1/orders",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(fullCreate),
                                bearer(employeeToken)),
                        String.class);
        assertThat(createOrder.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode created = objectMapper.readTree(createOrder.getBody());
        assertThat(created.get("id").asLong()).isPositive();

        assertThat(Mockito.mockingDetails(documentGenerationService).isSpy()).isTrue();

        verify(documentGenerationService, timeout(15_000).times(PREFETCH_GENERATION_CALLS))
                .generateDocument(any(Order.class), any(), any());

        var order = inOrder(documentGenerationService);
        order.verify(documentGenerationService)
                .generateDocument(any(Order.class), eq(DocumentType.CONTRACT_APPLICATION), eq(FileFormat.DOCX));
        order.verify(documentGenerationService)
                .generateDocument(any(Order.class), eq(DocumentType.WAYBILL), eq(FileFormat.DOCX));
        order.verify(documentGenerationService)
                .generateDocument(any(Order.class), eq(DocumentType.ACT_OF_WORK), eq(FileFormat.DOCX));
        order.verify(documentGenerationService)
                .generateDocument(any(Order.class), eq(DocumentType.CONTRACT_APPLICATION), eq(FileFormat.PDF));
        order.verify(documentGenerationService)
                .generateDocument(any(Order.class), eq(DocumentType.WAYBILL), eq(FileFormat.PDF));
        order.verify(documentGenerationService)
                .generateDocument(any(Order.class), eq(DocumentType.ACT_OF_WORK), eq(FileFormat.PDF));
    }

    private Long postCustomer(String token, CustomerRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/customers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long postPerformer(String token, PerformerRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/performers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long postDriver(String token, DriverRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/drivers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long postVehicle(String token, VehicleRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/vehicles",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }
}
