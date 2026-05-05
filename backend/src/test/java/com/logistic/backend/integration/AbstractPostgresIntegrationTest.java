package com.logistic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistic.backend.api.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class AbstractPostgresIntegrationTest {

    @Autowired protected TestRestTemplate restTemplate;
    @Autowired protected ObjectMapper objectMapper;

    protected static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    protected static HttpHeaders bearer(String token) {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(token);
        return h;
    }

    protected String loginToken(String username, String password) throws Exception {
        ResponseEntity<String> r =
                restTemplate.postForEntity(
                        "/api/v1/auth/login",
                        new HttpEntity<>(
                                objectMapper.writeValueAsString(new LoginRequest(username, password)),
                                jsonHeaders()),
                        String.class);
        if (!r.getStatusCode().is2xxSuccessful() || r.getBody() == null) {
            throw new IllegalStateException("login failed: " + r.getStatusCode());
        }
        return objectMapper.readTree(r.getBody()).get("token").asText();
    }
}
