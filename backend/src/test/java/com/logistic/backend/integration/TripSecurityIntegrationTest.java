package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.CounterpartyRequest;
import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.TripUpdateRequest;
import com.logistic.backend.api.dto.UserCreateRequest;
import com.logistic.backend.api.dto.VehicleRequest;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class TripSecurityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String tokenEmpA;
    private String tokenEmpB;

    @BeforeEach
    void seedUsers() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        tokenEmpA = saveUser("emp_a_" + id, "p1", EnumSet.of(Role.EMPLOYEE));
        tokenEmpB = saveUser("emp_b_" + id, "p2", EnumSet.of(Role.EMPLOYEE));
    }

    private String saveUser(String username, String password, Set<Role> roles) throws Exception {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setEnabled(true);
        u.setRoles(EnumSet.copyOf(roles));
        userRepository.save(u);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(new LoginRequest(username, password)),
                                jsonHeaders()),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("token").asText();
    }

    @Test
    void employeeCanCompleteOwnTrip() throws Exception {
        long tripId = createTripReadyToComplete(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/trips/" + tripId + "/complete",
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(r.getBody()).get("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void peerCannotDeleteOthersTrip() throws Exception {
        long tripId = createEmptyTrip(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(bearer(tokenEmpB)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertProblemJson(r, HttpStatus.FORBIDDEN);
    }

    @Test
    void employeeCannotCreateAdminUser() throws Exception {
        String body =
                objectMapper.writeValueAsString(
                        new UserCreateRequest("x_" + UUID.randomUUID(), "pw", Set.of(Role.EMPLOYEE)));
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/admin/users", new HttpEntity<>(body, bearer(tokenEmpA)), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertProblemJson(r, HttpStatus.FORBIDDEN);
    }

    @Test
    void getUnknownTripReturns404() throws Exception {
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/trips/999999",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertProblemJson(r, HttpStatus.NOT_FOUND);
    }

    @Test
    void getUnknownCounterpartyReturns404() throws Exception {
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/counterparties/999999",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertProblemJson(r, HttpStatus.NOT_FOUND);
    }

    @Test
    void updateCompletedTripWithIncompleteDataReturns400() throws Exception {
        long tripId = createCompletedTrip(tokenEmpA);
        String stripCargo = "{\"cargoDescription\":\"\"}";
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId,
                        HttpMethod.PUT,
                        new HttpEntity<>(stripCargo, bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblemJson(r, HttpStatus.BAD_REQUEST);
    }

    @Test
    void completeIncompleteTripReturns400() throws Exception {
        long tripId = createEmptyTrip(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/trips/" + tripId + "/complete",
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblemJson(r, HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidInnReturns400() throws Exception {
        String body =
                objectMapper.writeValueAsString(new CounterpartyRequest("Co", "abc", null, null));
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/counterparties", new HttpEntity<>(body, bearer(tokenEmpA)), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblemJson(r, HttpStatus.BAD_REQUEST);
    }

    @Test
    void peerEmployeeCannotReadOthersTrip() throws Exception {
        long tripId = createEmptyTrip(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId,
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(tokenEmpB)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertProblemJson(r, HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticatedTripListReturns401() {
        ResponseEntity<String> r = restTemplate.getForEntity("/api/v1/trips", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getHeaders().getContentType())
                .isNotNull()
                .matches(ct -> MediaType.APPLICATION_PROBLEM_JSON.includes(ct));
    }

    private void assertProblemJson(ResponseEntity<String> r, HttpStatus expected) throws Exception {
        assertThat(r.getHeaders().getContentType())
                .isNotNull()
                .matches(ct -> MediaType.APPLICATION_PROBLEM_JSON.includes(ct));
        JsonNode n = objectMapper.readTree(r.getBody());
        assertThat(n.path("status").asInt()).isEqualTo(expected.value());
        assertThat(n.path("title").asText()).isNotBlank();
    }

    private long createEmptyTrip(String token) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity("/api/v1/trips", new HttpEntity<>(bearer(token)), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private long createTripReadyToComplete(String token) throws Exception {
        Long shipperId = postCounterparty(token, new CounterpartyRequest("S", "1234567890", "a", "1"));
        Long consigneeId = postCounterparty(token, new CounterpartyRequest("C", "0987654321", "b", "2"));
        Long driverId = postDriver(token, new DriverRequest("D", "7712345678", "B"));
        Long vehicleId = postVehicle(token, new VehicleRequest("A111AA77", "M", 1000));
        long tripId = createEmptyTrip(token);
        ResponseEntity<String> upd =
                restTemplate.exchange(
                        "/api/v1/trips/" + tripId,
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new TripUpdateRequest(
                                                shipperId,
                                                consigneeId,
                                                driverId,
                                                vehicleId,
                                                "Cargo",
                                                new BigDecimal("1.000"),
                                                "A",
                                                "B",
                                                LocalDate.of(2026, 7, 1),
                                                LocalDate.of(2026, 7, 2),
                                                new BigDecimal("100.00"),
                                                "RUB")),
                                bearer(token)),
                        String.class);
        assertThat(upd.getStatusCode()).isEqualTo(HttpStatus.OK);
        return tripId;
    }

    private long createCompletedTrip(String token) throws Exception {
        long tripId = createTripReadyToComplete(token);
        ResponseEntity<String> done =
                restTemplate.postForEntity(
                        "/api/v1/trips/" + tripId + "/complete",
                        new HttpEntity<>(bearer(token)),
                        String.class);
        assertThat(done.getStatusCode()).isEqualTo(HttpStatus.OK);
        return tripId;
    }

    private Long postCounterparty(String token, CounterpartyRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/counterparties",
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
