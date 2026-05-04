package com.logistic.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret = "dev-secret-change-in-production-min-32-chars-long!!";
    private long expirationMs = 86_400_000L;
}
