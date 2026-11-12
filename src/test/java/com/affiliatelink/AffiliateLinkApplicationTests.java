package com.affiliatelink;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Testcontainers
class AffiliateLinkApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("affiliatelink_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void contextLoads() {
    }

}
