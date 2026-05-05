package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.PlaceRequest;
import com.logistic.backend.catalog.PlaceType;
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
class PlaceCrudIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void seedUser() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "emp_pl_" + id;
        User u = new User();
        u.setUsername(name);
        u.setPasswordHash(passwordEncoder.encode("secret12"));
        u.setEnabled(true);
        u.setRoles(EnumSet.of(Role.EMPLOYEE));
        userRepository.save(u);
        token = loginTokenAssertOk(name, "secret12");
    }

    @Test
    void crudAndListByType() throws Exception {
        PlaceRequest body =
                new PlaceRequest("Склад №1", "Петров +7", PlaceType.BOTH);
        ResponseEntity<String> created =
                restTemplate.postForEntity(
                        "/api/v1/places",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long placeId = objectMapper.readTree(created.getBody()).get("id").asLong();

        ResponseEntity<String> listLoad =
                restTemplate.exchange(
                        "/api/v1/places?type=LOAD",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(token)),
                        String.class);
        assertThat(listLoad.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = objectMapper.readTree(listLoad.getBody());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("address").asText()).isEqualTo("Склад №1");

        PlaceRequest update =
                new PlaceRequest("Склад №1 обновл.", "Иванов", PlaceType.LOAD);
        ResponseEntity<String> put =
                restTemplate.exchange(
                        "/api/v1/places/" + placeId,
                        HttpMethod.PUT,
                        new HttpEntity<>(objectMapper.writeValueAsString(update), bearer(token)),
                        String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(put.getBody()).get("placeType").asText())
                .isEqualTo("LOAD");

        ResponseEntity<String> get =
                restTemplate.exchange(
                        "/api/v1/places/" + placeId,
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(token)),
                        String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> del =
                restTemplate.exchange(
                        "/api/v1/places/" + placeId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(bearer(token)),
                        Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
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
}
