package com.ruby.rubia_server.config;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using REAL containers for both PostgreSQL and Redis.
 * This provides 100% realistic testing environment identical to production.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({TestRedisContainerConfiguration.class, MockAdapterTestConfiguration.class})
public abstract class AbstractIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgresqlContainer = TestContainersConfiguration.getInstance();
    
    @Container
    public static GenericContainer<?> redisContainer = TestRedisContainerConfiguration.getRedisContainer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
        
        // PostgreSQL connection pool for tests
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "20000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "300000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "1200000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
        
        // Redis configuration - ENABLE Redis for tests (override application.yml)
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
        registry.add("spring.data.redis.timeout", () -> "2000ms");
        registry.add("spring.data.redis.database", () -> "0");
    }
}
