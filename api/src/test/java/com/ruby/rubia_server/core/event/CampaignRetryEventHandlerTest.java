package com.ruby.rubia_server.core.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Campaign Retry Event Handler Tests")
class CampaignRetryEventHandlerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private ObjectMapper objectMapper;
    private CampaignRetryEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventHandler = new CampaignRetryEventHandler(redisTemplate, objectMapper);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("Should handle retry event successfully")
    void shouldHandleRetryEventSuccessfully() {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        CampaignRetryEvent event = new CampaignRetryEvent(
                this, campaignId, contactId, companyId
        );

        // When - Should not throw exception
        assertDoesNotThrow(() -> eventHandler.handleCampaignRetryEvent(event));

        // Then - Should interact with Redis (any parameters)
        verify(redisTemplate, atLeastOnce()).opsForZSet();
    }

    @Test
    @DisplayName("Should handle null event gracefully")
    void shouldHandleNullEventGracefully() {
        // Given
        CampaignRetryEvent eventWithNulls = new CampaignRetryEvent(
                this, null, null, null
        );

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> eventHandler.handleCampaignRetryEvent(eventWithNulls));
        
        // Should still interact with Redis
        verify(redisTemplate, atLeastOnce()).opsForZSet();
    }

    @Test
    @DisplayName("Should handle Redis errors gracefully")
    void shouldHandleRedisErrorsGracefully() {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        CampaignRetryEvent event = new CampaignRetryEvent(
                this, campaignId, contactId, companyId
        );

        // Mock Redis to throw exception
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - Should not throw exception (graceful error handling)
        assertDoesNotThrow(() -> eventHandler.handleCampaignRetryEvent(event));

        // Verify Redis operation was attempted
        verify(redisTemplate, atLeastOnce()).opsForZSet();
    }

    @Test
    @DisplayName("Should create event handler without errors")
    void shouldCreateEventHandlerWithoutErrors() {
        // When & Then - Constructor should work
        assertDoesNotThrow(() -> {
            CampaignRetryEventHandler handler = new CampaignRetryEventHandler(redisTemplate, objectMapper);
            assertNotNull(handler);
        });
    }
}