package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.OrderCreateRequest;
import com.logistic.backend.api.dto.OrderUpdateRequest;
import com.logistic.backend.api.dto.PerformerRequest;
import com.logistic.backend.api.dto.VehicleRequest;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class OrderLifecycleIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String employeeToken;

    @BeforeEach
    void seedUsers() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String empName = "emp_" + id;

        User emp = new User();
        emp.setUsername(empName);
        emp.setPasswordHash(passwordEncoder.encode("emp-pass"));
        emp.setEnabled(true);
        emp.setRoles(EnumSet.of(Role.USER));
        userRepository.save(emp);

        employeeToken = loginTokenAssertOk(empName, "emp-pass");
    }

    @Test
    void createUpdateCompleteDocumentsRegenerateWithSecondCompleteAndDelete() throws Exception {
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
                        new DriverRequest(performerId, "Ivan Ivanov", "+79001234567", true));
        Long vehicleId =
                postVehicle(
                        employeeToken,
                        new VehicleRequest(performerId, "GAZelle", "A123BC77", "фургон", true));

        String createBody =
                objectMapper.writeValueAsString(new OrderCreateRequest(customerId, performerId, null, null));
        ResponseEntity<String> createOrder =
                restTemplate.postForEntity(
                        "/api/v1/orders",
                        new HttpEntity<>(createBody, authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(createOrder.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode created = objectMapper.readTree(createOrder.getBody());
        long orderId = created.get("id").asLong();

        OrderUpdateRequest full =
                new OrderUpdateRequest(
                        null,
                        null,
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

        ResponseEntity<String> updateOrder =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(full), authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(updateOrder.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> complete =
                restTemplate.postForEntity(
                        "/api/v1/orders/" + orderId + "/complete",
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(complete.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> docs =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId + "/documents",
                        HttpMethod.GET,
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = objectMapper.readTree(docs.getBody());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(6);
        String shaBefore = arr.get(0).get("sha256").asText();

        OrderUpdateRequest revised =
                new OrderUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("50000.00"));
        ResponseEntity<String> editAfterComplete =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(revised), authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(editAfterComplete.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> completeAgain =
                restTemplate.postForEntity(
                        "/api/v1/orders/" + orderId + "/complete",
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(completeAgain.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> docsAfter =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId + "/documents",
                        HttpMethod.GET,
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(docsAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr2 = objectMapper.readTree(docsAfter.getBody());
        assertThat(arr2.size()).isEqualTo(6);
        assertThat(arr2.get(0).get("sha256").asText()).isNotEqualTo(shaBefore);

        long firstDocId = arr2.get(0).get("id").asLong();
        ResponseEntity<byte[]> file =
                restTemplate.exchange(
                        "/api/v1/generated-documents/" + firstDocId + "/file",
                        HttpMethod.GET,
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        byte[].class);
        assertThat(file.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(file.getBody()).isNotNull();
        assertThat(file.getBody().length).isGreaterThan(10);

        ResponseEntity<Void> del =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> gone =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.GET,
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders authorizedHeaders(String token) {
        return bearer(token);
    }

    private String loginTokenAssertOk(String user, String pass) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(new LoginRequest(user, pass)),
                                jsonHeaders()),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode node = objectMapper.readTree(r.getBody());
        return node.get("token").asText();
    }

    private Long postCustomer(String token, CustomerRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/customers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), authorizedHeaders(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long postPerformer(String token, PerformerRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/performers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), authorizedHeaders(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long postDriver(String token, DriverRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/drivers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), authorizedHeaders(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long postVehicle(String token, VehicleRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/vehicles",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), authorizedHeaders(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }
}
