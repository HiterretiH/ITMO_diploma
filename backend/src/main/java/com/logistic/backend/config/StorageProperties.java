package com.logistic.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logistic.storage")
@Getter
@Setter
public class StorageProperties {

    private String root = System.getProperty("user.home") + "/.logistic-storage";
}
