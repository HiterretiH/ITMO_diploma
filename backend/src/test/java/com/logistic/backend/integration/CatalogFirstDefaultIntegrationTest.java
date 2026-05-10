package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.PerformerRequest;
import com.logistic.backend.api.dto.VehicleRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class CatalogFirstDefaultIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void seedUser() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "def_" + id;
        User u = new User();
        u.setUsername(name);
        u.setPasswordHash(passwordEncoder.encode("pw"));
        u.setEnabled(true);
        u.setRoles(EnumSet.of(Role.USER));
        userRepository.save(u);
        token = login(name, "pw");
    }

    @Test
    void firstDriver_and_first_vehicle_are_default_even_when_false_requested() throws Exception {
        Long performerId =
                postPerformer(
                        token,
                        new PerformerRequest(
                                "P",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null));

        ResponseEntity<String> d1 =
                restTemplate.postForEntity(
                        "/api/v1/drivers",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new DriverRequest(performerId, "A", null, false)),
                                bearer(token)),
                        String.class);
        assertThat(d1.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode dj = objectMapper.readTree(d1.getBody());
        assertThat(dj.get("isDefault").asBoolean()).isTrue();

        ResponseEntity<String> d2 =
                restTemplate.postForEntity(
                        "/api/v1/drivers",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new DriverRequest(performerId, "B", null, false)),
                                bearer(token)),
                        String.class);
        assertThat(d2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(d2.getBody()).get("isDefault").asBoolean()).isFalse();

        ResponseEntity<String> v1 =
                restTemplate.postForEntity(
                        "/api/v1/vehicles",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new VehicleRequest(performerId, null, "X001XX77", null, false)),
                                bearer(token)),
                        String.class);
        assertThat(v1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(v1.getBody()).get("isDefault").asBoolean()).isTrue();

        ResponseEntity<String> v2 =
                restTemplate.postForEntity(
                        "/api/v1/vehicles",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(
                                        new VehicleRequest(performerId, null, "Y002YY77", null, false)),
                                bearer(token)),
                        String.class);
        assertThat(v2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(v2.getBody()).get("isDefault").asBoolean()).isFalse();
    }

    private String login(String user, String pass) throws Exception {
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

    private Long postPerformer(String token, PerformerRequest body) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/performers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody()).get("id").asLong();
    }
}
