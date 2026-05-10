package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.util.EnumSet;
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
class MeProfileIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String username;
    private String token;

    @BeforeEach
    void seedUser() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        username = "me_" + id;
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode("old-secret"));
        u.setEnabled(true);
        u.setRoles(EnumSet.of(Role.USER));
        userRepository.save(u);
        token = loginToken(username, "old-secret");
    }

    @Test
    void getMe_returnsUsernameAndRoles() throws Exception {
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/me",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode n = objectMapper.readTree(r.getBody());
        assertThat(n.get("username").asText()).isEqualTo(username);
        assertThat(n.get("roles").toString()).contains("USER");
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("currentPassword", "wrong");
        body.put("newPassword", "new-secret-ok");
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/me/password",
                        HttpMethod.PUT,
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePassword_thenLoginUsesNewPassword() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("currentPassword", "old-secret");
        body.put("newPassword", "fresh-pass-12");
        ResponseEntity<Void> put =
                restTemplate.exchange(
                        "/api/v1/me/password",
                        HttpMethod.PUT,
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        Void.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> oldLogin =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new LoginRequest(username, "old-secret")),
                                jsonHeaders()),
                        String.class);
        assertThat(oldLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> newLogin =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new LoginRequest(username, "fresh-pass-12")),
                                jsonHeaders()),
                        String.class);
        assertThat(newLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(newLogin.getBody()).has("token")).isTrue();
    }
}
