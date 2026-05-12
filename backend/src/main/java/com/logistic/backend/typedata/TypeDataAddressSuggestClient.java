package com.logistic.backend.typedata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TypeDataAddressSuggestClient {

    private final TypedataProperties typedataProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public List<String> fetchSuggestions(String query) {
        if (!typedataProperties.isEnabled() || !typedataProperties.hasToken()) {
            return List.of();
        }
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.strip();
        try {
            String json =
                    restClient
                            .post()
                            .uri(typedataProperties.getApiUrl())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Token " + typedataProperties.getToken().strip())
                            .body(Map.of("query", q))
                            .retrieve()
                            .body(String.class);
            if (json == null || json.isBlank()) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(json);
            if (!root.has("suggestions") || !root.get("suggestions").isArray()) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (JsonNode n : root.get("suggestions")) {
                if (n != null && n.has("value") && n.get("value").isTextual()) {
                    String v = n.get("value").asText();
                    if (v != null && !v.isBlank()) {
                        out.add(v.strip());
                    }
                }
            }
            return out;
        } catch (RestClientException | JsonProcessingException ex) {
            log.warn("TypeData suggest request failed: {}", ex.getMessage());
            return List.of();
        }
    }
}
