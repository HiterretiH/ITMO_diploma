package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class RegistrationApprovalIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void seedAdmin() throws Exception {
        String adminUser = "adm_" + UUID.randomUUID().toString().substring(0, 8);
        User admin = new User();
        admin.setUsername(adminUser);
        admin.setPasswordHash(passwordEncoder.encode("admin-pass"));
        admin.setEnabled(true);
        admin.setRegistrationStatus(com.logistic.backend.user.RegistrationStatus.APPROVED);
        admin.setRoles(EnumSet.of(Role.ADMIN));
        userRepository.save(admin);
        adminToken = loginToken(adminUser, "admin-pass");
    }

    @Test
    void registerApproveThenLoginSucceeds() throws Exception {
        String pendingUser = "usr_" + UUID.randomUUID().toString().substring(0, 8);

        ResponseEntity<String> register =
                restTemplate.postForEntity(
                        "/api/v1/auth/register",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        Map.of("username", pendingUser, "password", "user-pass")),
                                jsonHeaders()),
                        String.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        long requestId = findRequestId(pendingUser);

        ResponseEntity<String> approve =
                restTemplate.postForEntity(
                        "/api/v1/admin/registration-requests/" + requestId + "/approve",
                        new HttpEntity<>(bearer(adminToken)),
                        String.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(approve.getBody()).get("registrationStatus").asText())
                .isEqualTo("APPROVED");

        String userToken = loginToken(pendingUser, "user-pass");
        assertThat(userToken).isNotBlank();
    }

    @Test
    void rejectThenApproveThenLoginSucceeds() throws Exception {
        String pendingUser = "usr2_" + UUID.randomUUID().toString().substring(0, 8);

        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(
                        objectMapper.writeValueAsString(
                                Map.of("username", pendingUser, "password", "user-pass")),
                        jsonHeaders()),
                String.class);

        long requestId = findRequestId(pendingUser);

        ResponseEntity<String> reject =
                restTemplate.postForEntity(
                        "/api/v1/admin/registration-requests/" + requestId + "/reject",
                        new HttpEntity<>(bearer(adminToken)),
                        String.class);
        assertThat(reject.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> loginRejected =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new LoginRequest(pendingUser, "user-pass")),
                                jsonHeaders()),
                        String.class);
        assertThat(loginRejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> approve =
                restTemplate.postForEntity(
                        "/api/v1/admin/registration-requests/" + requestId + "/approve",
                        new HttpEntity<>(bearer(adminToken)),
                        String.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);

        String userToken = loginToken(pendingUser, "user-pass");
        assertThat(userToken).isNotBlank();
    }

    private long findRequestId(String username) throws Exception {
        ResponseEntity<String> list =
                restTemplate.exchange(
                        "/api/v1/admin/registration-requests",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(adminToken)),
                        String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode requests = objectMapper.readTree(list.getBody());
        for (JsonNode n : requests) {
            if (username.equals(n.get("username").asText())) {
                return n.get("id").asLong();
            }
        }
        throw new IllegalStateException("registration request not found for " + username);
    }
}
