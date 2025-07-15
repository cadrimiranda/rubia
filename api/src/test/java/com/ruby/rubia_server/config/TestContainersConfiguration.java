package com.ruby.rubia_server.config;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class TestContainersConfiguration {
    
    private static volatile PostgreSQLContainer<?> container;
    
    public static PostgreSQLContainer<?> getInstance() {
        if (container == null) {
            synchronized (TestContainersConfiguration.class) {
                if (container == null) {
                    container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:13.3"))
                            .withDatabaseName("testdb")
                            .withUsername("testuser")
                            .withPassword("testpass")
                            .withReuse(true)
                            .withStartupTimeoutSeconds(120)
                            .withConnectTimeoutSeconds(60);
                }
            }
        }
        return container;
    }
}