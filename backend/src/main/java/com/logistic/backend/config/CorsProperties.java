package com.logistic.backend.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

    private List<String> allowedOriginPatterns = new ArrayList<>();
}
