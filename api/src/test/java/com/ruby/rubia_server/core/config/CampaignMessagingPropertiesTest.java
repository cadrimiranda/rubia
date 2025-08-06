package com.ruby.rubia_server.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@EnableConfigurationProperties(CampaignMessagingProperties.class)
@DisplayName("Campaign Messaging Properties Tests")
class CampaignMessagingPropertiesTest {

    @Autowired
    private CampaignMessagingProperties properties;

    @Test
    @DisplayName("Should load default optimized values")
    void shouldLoadDefaultOptimizedValues() {
        assertThat(properties.getBatchSize()).isEqualTo(30);
        assertThat(properties.getBatchPauseMinutes()).isEqualTo(30);
        assertThat(properties.getMinDelayMs()).isEqualTo(15000);
        assertThat(properties.getMaxDelayMs()).isEqualTo(45000);
        assertThat(properties.getMessageTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getBatchTimeout()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("Should load retry configuration")
    void shouldLoadRetryConfiguration() {
        assertThat(properties.getMaxRetries()).isEqualTo(3);
        assertThat(properties.getRetryDelayMs()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should load business hours configuration")
    void shouldLoadBusinessHoursConfiguration() {
        assertThat(properties.isBusinessHoursOnly()).isTrue();
        assertThat(properties.getBusinessStartHour()).isEqualTo(9);
        assertThat(properties.getBusinessEndHour()).isEqualTo(18);
    }

    @Test
    @DisplayName("Should enable message randomization")
    void shouldEnableMessageRandomization() {
        assertThat(properties.isRandomizeOrder()).isTrue();
    }

    @Test
    @DisplayName("Should validate delay range consistency")
    void shouldValidateDelayRangeConsistency() {
        assertThat(properties.getMinDelayMs()).isLessThan(properties.getMaxDelayMs());
        assertThat(properties.getMaxDelayMs() - properties.getMinDelayMs()).isEqualTo(30000); // 30 second range
    }

    @Test
    @DisplayName("Should validate business hours range")
    void shouldValidateBusinessHoursRange() {
        assertThat(properties.getBusinessStartHour()).isLessThan(properties.getBusinessEndHour());
        assertThat(properties.getBusinessStartHour()).isGreaterThanOrEqualTo(0);
        assertThat(properties.getBusinessEndHour()).isLessThanOrEqualTo(24);
    }

    @Test
    @DisplayName("Should validate timeout configurations")
    void shouldValidateTimeoutConfigurations() {
        assertThat(properties.getMessageTimeout().toSeconds()).isEqualTo(30);
        assertThat(properties.getBatchTimeout().toMinutes()).isEqualTo(15);
        assertThat(properties.getBatchTimeout()).isGreaterThan(properties.getMessageTimeout());
    }
}

@SpringJUnitConfig
@EnableConfigurationProperties(CampaignMessagingProperties.class)
@TestPropertySource(properties = {
    "campaign.messaging.batch-size=50",
    "campaign.messaging.batch-pause-minutes=20",
    "campaign.messaging.min-delay-ms=10000",
    "campaign.messaging.max-delay-ms=30000",
    "campaign.messaging.max-retries=5",
    "campaign.messaging.retry-delay-ms=3000",
    "campaign.messaging.business-hours-only=false",
    "campaign.messaging.business-start-hour=8",
    "campaign.messaging.business-end-hour=20",
    "campaign.messaging.randomize-order=false"
})
@DisplayName("Campaign Messaging Properties Custom Configuration Tests")
class CampaignMessagingPropertiesCustomConfigTest {

    @Autowired
    private CampaignMessagingProperties properties;

    @Test
    @DisplayName("Should load custom configuration values")
    void shouldLoadCustomConfigurationValues() {
        assertThat(properties.getBatchSize()).isEqualTo(50);
        assertThat(properties.getBatchPauseMinutes()).isEqualTo(20);
        assertThat(properties.getMinDelayMs()).isEqualTo(10000);
        assertThat(properties.getMaxDelayMs()).isEqualTo(30000);
        assertThat(properties.getMaxRetries()).isEqualTo(5);
        assertThat(properties.getRetryDelayMs()).isEqualTo(3000);
        assertThat(properties.isBusinessHoursOnly()).isFalse();
        assertThat(properties.getBusinessStartHour()).isEqualTo(8);
        assertThat(properties.getBusinessEndHour()).isEqualTo(20);
        assertThat(properties.isRandomizeOrder()).isFalse();
    }

    @Test
    @DisplayName("Should calculate performance metrics correctly")
    void shouldCalculatePerformanceMetricsCorrectly() {
        // Com batch-size=50 e pause=20min, para 1000 mensagens:
        // 20 lotes * (tempo_processamento + 20min_pausa) - ultima_pausa
        int totalMessages = 1000;
        int batches = (int) Math.ceil((double) totalMessages / properties.getBatchSize());
        
        assertThat(batches).isEqualTo(20); // 1000/50 = 20 lotes exatos
        
        // Verificar configuração para alta performance
        assertThat(properties.getBatchSize()).isGreaterThan(30); // Maior que padrão otimizado
        assertThat(properties.getBatchPauseMinutes()).isLessThan(30); // Menor que padrão otimizado
    }
}