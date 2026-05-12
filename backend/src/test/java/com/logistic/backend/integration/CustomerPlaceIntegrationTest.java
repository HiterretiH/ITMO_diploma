package com.logistic.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.OrderCreateRequest;
import com.logistic.backend.api.dto.OrderUpdateRequest;
import com.logistic.backend.api.dto.PerformerRequest;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
class CustomerPlaceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void seedUser() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "place_" + id;
        User u = new User();
        u.setUsername(name);
        u.setPasswordHash(passwordEncoder.encode("pw"));
        u.setEnabled(true);
        u.setRoles(EnumSet.of(Role.USER));
        userRepository.save(u);
        token = loginToken(name, "pw");
    }

    @Test
    void orderCreateThenPlaces_listedAndContactUpdatedOnPut() throws Exception {
        Long customerId =
                postCustomer(token, new CustomerRequest("PlaceCo", "Full", "+1", null));
        Long performerId =
                postPerformer(
                        token,
                        new PerformerRequest(
                                "PlacePerf",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null));

        OrderCreateRequest createReq =
                new OrderCreateRequest(
                        customerId,
                        performerId,
                        null,
                        null,
                        LocalDate.of(2026, 4, 1),
                        null,
                        "Warehouse  North",
                        "Call +1",
                        "Shop  East",
                        "Unload +2",
                        1,
                        BigDecimal.ONE,
                        BigDecimal.ONE);
        ResponseEntity<String> createOrder =
                restTemplate.postForEntity(
                        "/api/v1/orders",
                        new HttpEntity<>(objectMapper.writeValueAsString(createReq), bearer(token)),
                        String.class);
        assertThat(createOrder.getStatusCode()).isEqualTo(HttpStatus.OK);
        long orderId = objectMapper.readTree(createOrder.getBody()).get("id").asLong();

        JsonNode loadPlaces = getPlaces(customerId, "LOAD");
        assertThat(loadPlaces).hasSize(1);
        assertThat(loadPlaces.get(0).get("address").asText()).isEqualTo("Warehouse  North");
        assertThat(loadPlaces.get(0).get("contact").asText()).isEqualTo("Call +1");

        JsonNode unloadPlaces = getPlaces(customerId, "UNLOAD");
        assertThat(unloadPlaces).hasSize(1);
        assertThat(unloadPlaces.get(0).get("address").asText()).isEqualTo("Shop  East");

        OrderUpdateRequest upd =
                new OrderUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Warehouse  north",
                        "Call +99",
                        "Shop  East",
                        null,
                        null,
                        null,
                        null);
        ResponseEntity<String> put =
                restTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.PUT,
                        new HttpEntity<>(objectMapper.writeValueAsString(upd), bearer(token)),
                        String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode loadPlaces2 = getPlaces(customerId, "LOAD");
        assertThat(loadPlaces2).hasSize(1);
        assertThat(loadPlaces2.get(0).get("contact").asText()).isEqualTo("Call +99");

        JsonNode suggestions = getPlaceSuggestions(customerId, "LOAD", "Ware");
        assertThat(suggestions).hasSizeGreaterThanOrEqualTo(1);
        assertThat(suggestions.get(0).get("source").asText()).isEqualTo("HISTORY");
        assertThat(suggestions.get(0).get("kind").asText()).isEqualTo("LOAD");
        assertThat(suggestions.get(0).get("address").asText()).contains("Warehouse");
        assertThat(suggestions.get(0).get("contact").asText()).isEqualTo("Call +99");
    }

    private JsonNode getPlaceSuggestions(long customerId, String kind, String q) throws Exception {
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/customers/"
                                + customerId
                                + "/place-suggestions?kind="
                                + kind
                                + "&q="
                                + URLEncoder.encode(q, StandardCharsets.UTF_8),
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody());
    }

    private JsonNode getPlaces(long customerId, String kind) throws Exception {
        ResponseEntity<String> r =
                restTemplate.exchange(
                        "/api/v1/customers/" + customerId + "/places?kind=" + kind,
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(token)),
                        String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(r.getBody());
    }

    private Long postCustomer(String token, CustomerRequest body) throws Exception {
        ResponseEntity<String> res =
                restTemplate.postForEntity(
                        "/api/v1/customers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(res.getBody()).get("id").asLong();
    }

    private Long postPerformer(String token, PerformerRequest body) throws Exception {
        ResponseEntity<String> res =
                restTemplate.postForEntity(
                        "/api/v1/performers",
                        new HttpEntity<>(objectMapper.writeValueAsString(body), bearer(token)),
                        String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(res.getBody()).get("id").asLong();
    }
}
