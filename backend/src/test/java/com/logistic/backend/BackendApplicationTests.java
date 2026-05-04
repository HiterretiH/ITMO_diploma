package com.logistic.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BackendApplicationTests extends com.logistic.backend.integration.AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {}
}
