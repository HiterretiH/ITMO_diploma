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
class TripFormDraftIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void seedUser() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "draft_" + id;
        User u = new User();
        u.setUsername(name);
        u.setPasswordHash(passwordEncoder.encode("pw"));
        u.setEnabled(true);
        u.setRoles(EnumSet.of(Role.USER));
        userRepository.save(u);
        token = loginTokenAssertOk(name, "pw");
    }

    @Test
    void firstDraft_hasNextOne_andNullDefaults() throws Exception {
        JsonNode draft = getDraft(null);
        assertThat(draft.get("nextOrderNumber").asInt()).isEqualTo(1);
        assertThat(draft.get("lastPerformerId").isNull()).isTrue();
        assertThat(draft.get("lastDriverId").isNull()).isTrue();
        assertThat(draft.get("lastVehicleId").isNull()).isTrue();
    }

    @Test
    void afterOrderUpdate_defaultsReflectLastSelections() throws Exception {
        Long customerId =
                postCustomer(
                        token,
                        new CustomerRequest("Co", "Co Full", "+1", null));
        Long performerId =
                postPerformer(
                        token,
                        new PerformerRequest(
                                "Perf",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null));
        Long driverId =
                postDriver(token, new DriverRequest(performerId, "Ivan", null));
        Long vehicleId =
                postVehicle(token, new VehicleRequest(performerId, null, "A111AA77", null));

        ResponseEntity<String> createOrder =
                restTemplate.postForEntity(
                        "/api/v1/orders",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new OrderCreateRequest(customerId, performerId, null, null)),
                                bearer(token)),
                        String.class);
        assertThat(createOrder.getStatusCode()).isEqualTo(HttpStatus.OK);
        long orderId = objectMapper.readTree(createOrder.getBody()).get("id").asLong();

        OrderUpdateRequest upd =
                new OrderUpdateRequest(
                        null,
                        null,
                        vehicleId,
                        driverId,
                        LocalDate.of(2026, 3, 1),
                        null,
                        "A",
                        null,
                        "B",
                        null,
                        1,
                        BigDecimal.ONE,
                        BigDecimal.ONE);
        ResponseEntity<String> put =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.PUT,
                        new HttpEntity<>(objectMapper.writeValueAsString(upd), bearer(token)),
                        String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode draft = getDraft(customerId);
        assertThat(draft.get("nextOrderNumber").asInt()).isEqualTo(2);
        assertThat(draft.get("lastPerformerId").asLong()).isEqualTo(performerId);
        assertThat(draft.get("lastDriverId").asLong()).isEqualTo(driverId);
        assertThat(draft.get("lastVehicleId").asLong()).isEqualTo(vehicleId);
    }

    private JsonNode getDraft(Long customerId) throws Exception {
        String url =
                customerId == null
                        ? "/api/v1/me/trip-form-draft"
                        : "/api/v1/me/trip-form-draft?customerId=" + customerId;
        ResponseEntity<String> r =
                restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody());
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
        return objectMapper.readTree(r.getBody()).get("token").asText();
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
