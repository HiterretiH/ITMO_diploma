package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
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
class ErrorContractIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void seed() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "err_" + id;
        User u = new User();
        u.setUsername(name);
        u.setPasswordHash(passwordEncoder.encode("p"));
        u.setEnabled(true);
        u.setRoles(EnumSet.of(Role.EMPLOYEE));
        userRepository.save(u);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(new LoginRequest(name, "p")),
                                jsonHeaders()),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        token = objectMapper.readTree(r.getBody()).get("token").asText();
    }

    @Test
    void malformedJsonLoginReturns400ProblemJson() throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/auth/login", new HttpEntity<>("{not-json", h), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType())
                .isNotNull()
                .matches(ct -> MediaType.APPLICATION_PROBLEM_JSON.includes(ct));
        assertProblemShape(r.getBody(), 400);
    }

    @Test
    void missingAuthReturns401ProblemJson() throws Exception {
        ResponseEntity<String> r =
                restTemplate.getForEntity("/api/v1/orders", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getHeaders().getContentType())
                .isNotNull()
                .matches(ct -> MediaType.APPLICATION_PROBLEM_JSON.includes(ct));
        assertProblemShape(r.getBody(), 401);
    }

    @Test
    void unknownOrderReturns404ProblemJson() throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/orders/999999",
                        HttpMethod.GET,
                        new HttpEntity<>(h),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getHeaders().getContentType())
                .isNotNull()
                .matches(ct -> MediaType.APPLICATION_PROBLEM_JSON.includes(ct));
        assertProblemShape(r.getBody(), 404);
    }

    private void assertProblemShape(String body, int expectedStatus) throws Exception {
        JsonNode n = objectMapper.readTree(body);
        assertThat(n.path("status").asInt()).isEqualTo(expectedStatus);
        assertThat(n.path("title").asText()).isNotBlank();
    }
}
