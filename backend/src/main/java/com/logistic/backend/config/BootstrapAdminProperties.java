package com.logistic.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.admin")
@Getter
@Setter
public class BootstrapAdminProperties {

    private boolean enabled = false;
    private String username = "";
    private String password = "";
}
