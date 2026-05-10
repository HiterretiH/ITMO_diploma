package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.OrderCreateRequest;
import com.logistic.backend.api.dto.OrderUpdateRequest;
import com.logistic.backend.api.dto.PerformerRequest;
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
class OrderSecurityIntegrationTest extends AbstractPostgresIntegrationTest {

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
    void employeeCanCompleteOrder() throws Exception {
        long orderId = createOrderReadyToComplete(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/orders/" + orderId + "/complete",
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(r.getBody()).get("templateVersion").asInt()).isGreaterThan(0);
    }

    @Test
    void peerCanDeleteOrderCreatedByAnotherEmployee() throws Exception {
        long orderId = createEmptyOrder(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(bearer(tokenEmpB)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
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
    void getUnknownOrderReturns404() throws Exception {
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/orders/999999",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertProblemJson(r, HttpStatus.NOT_FOUND);
    }

    @Test
    void getUnknownCustomerReturns404() throws Exception {
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/customers/999999",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertProblemJson(r, HttpStatus.NOT_FOUND);
    }

    @Test
    void completeIncompleteOrderReturns400() throws Exception {
        long orderId = createEmptyOrder(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/orders/" + orderId + "/complete",
                        new HttpEntity<>(bearer(tokenEmpA)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblemJson(r, HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidCustomerShortNameReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(new CustomerRequest("", null, null, null));
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/customers", new HttpEntity<>(body, bearer(tokenEmpA)), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblemJson(r, HttpStatus.BAD_REQUEST);
    }

    @Test
    void peerEmployeeCanReadOrderCreatedByAnother() throws Exception {
        long orderId = createEmptyOrder(tokenEmpA);
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(tokenEmpB)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unauthenticatedOrderListReturns401() {
        ResponseEntity<String> r = restTemplate.getForEntity("/api/v1/orders", String.class);
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

    private long createEmptyOrder(String token) throws Exception {
        Long customerId =
                postCustomer(token, new CustomerRequest("C", "CFull", null, null));
        Long performerId =
                postPerformer(
                        token,
                        new PerformerRequest("P", "PFull", null, null, null, null, null, null, null, null));
        String body =
                objectMapper.writeValueAsString(new OrderCreateRequest(customerId, performerId, null, null));
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/orders", new HttpEntity<>(body, bearer(token)), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }

    private long createOrderReadyToComplete(String token) throws Exception {
        Long customerId = postCustomer(token, new CustomerRequest("S", "SFull", null, null));
        Long performerId =
                postPerformer(
                        token,
                        new PerformerRequest(
                                "P",
                                "PFull",
                                null,
                                null,
                                "1234567890",
                                null,
                                null,
                                null,
                                null,
                                null));
        Long driverId = postDriver(token, new DriverRequest(performerId, "D", null, false));
        Long vehicleId = postVehicle(token, new VehicleRequest(performerId, "M", "A111AA77", null, false));
        String create =
                objectMapper.writeValueAsString(new OrderCreateRequest(customerId, performerId, null, null));
        ResponseEntity<String> createR =
                restTemplate.postForEntity(
                        "/api/v1/orders", new HttpEntity<>(create, bearer(token)), String.class);
        assertThat(createR.getStatusCode()).isEqualTo(HttpStatus.OK);
        long orderId = objectMapper.readTree(createR.getBody()).get("id").asLong();

        OrderUpdateRequest full =
                new OrderUpdateRequest(
                        null,
                        null,
                        vehicleId,
                        driverId,
                        LocalDate.of(2026, 7, 1),
                        null,
                        "A",
                        null,
                        "B",
                        null,
                        null,
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"));
        ResponseEntity<String> upd =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.PUT,
                        new HttpEntity<>(objectMapper.writeValueAsString(full), bearer(token)),
                        String.class);
        assertThat(upd.getStatusCode()).isEqualTo(HttpStatus.OK);
        return orderId;
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
