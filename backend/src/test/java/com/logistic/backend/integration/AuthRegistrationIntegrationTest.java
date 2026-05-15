package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.UserCreateRequest;
import com.logistic.backend.user.Role;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class AuthRegistrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void registerReturnsCreatedPendingWithoutJwt() throws Exception {
        String u = "reg_" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/auth/register",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        Map.of("username", u, "password", "secret12")),
                                jsonHeaders()),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode node = objectMapper.readTree(r.getBody());
        assertThat(node.get("username").asText()).isEqualTo(u);
        assertThat(node.get("registrationStatus").asText()).isEqualTo("PENDING");
        assertThat(node.get("token")).isNull();
    }

    @Test
    void registerDuplicateReturns409() throws Exception {
        String u = "dup_" + UUID.randomUUID().toString().substring(0, 8);
        String body =
                objectMapper.writeValueAsString(Map.of("username", u, "password", "secret12"));
        ResponseEntity<String> r1 =
                restTemplate.postForEntity(
                        "/api/v1/auth/register",
                        new HttpEntity<>(body, jsonHeaders()),
                        String.class);
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<String> r2 =
                restTemplate.postForEntity(
                        "/api/v1/auth/register",
                        new HttpEntity<>(body, jsonHeaders()),
                        String.class);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginPendingRegistrationReturns403() throws Exception {
        String u = "pending_" + UUID.randomUUID().toString().substring(0, 8);
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(
                        objectMapper.writeValueAsString(Map.of("username", u, "password", "secret12")),
                        jsonHeaders()),
                String.class);

        ResponseEntity<String> login =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(new LoginRequest(u, "secret12")),
                                jsonHeaders()),
                        String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
