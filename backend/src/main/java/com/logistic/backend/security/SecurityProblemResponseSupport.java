package com.logistic.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistic.backend.api.ProblemDetailRu;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityProblemResponseSupport {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        ProblemDetailRu.setTitle(pd, status);
        objectMapper.writeValue(response.getOutputStream(), pd);
    }
}
