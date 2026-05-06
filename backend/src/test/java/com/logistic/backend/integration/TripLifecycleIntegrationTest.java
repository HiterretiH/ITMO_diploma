package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.CounterpartyRequest;
import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.TripUpdateRequest;
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
class TripLifecycleIntegrationTest extends AbstractPostgresIntegrationTest {

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
        emp.setRoles(EnumSet.of(Role.EMPLOYEE));
        userRepository.save(emp);

        employeeToken = loginTokenAssertOk(empName, "emp-pass");
    }

    @Test
    void inProgressCompleteDocumentsEditCompletedRegenerateAndDelete() throws Exception {
        Long shipperId = createCounterparty(employeeToken, new CounterpartyRequest("Ship LLC", "1234567890", "A", "1"));
        Long consigneeId = createCounterparty(employeeToken, new CounterpartyRequest("Recv LLC", "0987654321", "B", "2"));
        Long driverId = createDriver(employeeToken, new DriverRequest("Ivan Ivanov", "7712345678", "B"));
        Long vehicleId = createVehicle(employeeToken, new VehicleRequest("A123BC77", "GAZelle", 1500));

        ResponseEntity<String> createTrip =
                restTemplate.postForEntity(
                        "/api/v1/trips",
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(createTrip.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode created = objectMapper.readTree(createTrip.getBody());
        long tripId = created.get("id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("IN_PROGRESS");

        TripUpdateRequest full =
                new TripUpdateRequest(
                        shipperId,
                        consigneeId,
                        driverId,
                        vehicleId,
                        "Bricks",
                        new BigDecimal("1200.500"),
                        "Moscow",
                        "Tver",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 2),
                        new BigDecimal("45000.00"),
                        "RUB");

        ResponseEntity<String> updateTrip =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId,
                        HttpMethod.PUT,
                        new HttpEntity<>(objectMapper.writeValueAsString(full), authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(updateTrip.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> complete =
                restTemplate.postForEntity(
                        "/api/v1/trips/" + tripId + "/complete",
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(complete.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(complete.getBody()).get("status").asText()).isEqualTo("COMPLETED");

        ResponseEntity<String> docs =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId + "/documents",
                        HttpMethod.GET,
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = objectMapper.readTree(docs.getBody());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(6);
        String shaBefore = arr.get(0).get("sha256").asText();

        TripUpdateRequest revised =
                new TripUpdateRequest(
                        shipperId,
                        consigneeId,
                        driverId,
                        vehicleId,
                        "Bricks revised",
                        new BigDecimal("1200.500"),
                        "Moscow",
                        "Tver",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 2),
                        new BigDecimal("45000.00"),
                        "RUB");
        ResponseEntity<String> editCompleted =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId,
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(revised), authorizedHeaders(employeeToken)),
                        String.class);
        assertThat(editCompleted.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> docsAfter =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId + "/documents",
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
                        "/api/v1/trips/" + tripId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(authorizedHeaders(employeeToken)),
                        Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> gone =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId,
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

    private Long createCounterparty(String token, CounterpartyRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/counterparties",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), authorizedHeaders(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long createDriver(String token, DriverRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/drivers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), authorizedHeaders(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private Long createVehicle(String token, VehicleRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/vehicles",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), authorizedHeaders(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }
}
