package com.ruby.rubia_server.core.adapter.impl;

import com.ruby.rubia_server.core.entity.MessageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZApiAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ZApiAdapter zApiAdapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(zApiAdapter, "instanceUrl", "https://api.z-api.io/instances/test");
        ReflectionTestUtils.setField(zApiAdapter, "token", "test-token");
        ReflectionTestUtils.setField(zApiAdapter, "webhookToken", "webhook-token");
        ReflectionTestUtils.setField(zApiAdapter, "restTemplate", restTemplate);
    }

    @Test
    void shouldHandleZApiTimeoutGracefully() {
        // Given
        String phoneNumber = "+5511999999999";
        String message = "Test message";
        
        // Simulate timeout exception
        ResourceAccessException timeoutException = new ResourceAccessException(
            "Read timed out", new SocketTimeoutException("Read timed out"));
        
        when(restTemplate.exchange(
            anyString(), 
            eq(HttpMethod.POST), 
            any(HttpEntity.class), 
            eq(Map.class)
        )).thenThrow(timeoutException);

        // When
        MessageResult result = zApiAdapter.sendMessage(phoneNumber, message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Error sending message via Z-API");
        assertThat(result.getError()).contains("Read timed out");
        assertThat(result.getProvider()).isEqualTo("z-api");
        
        verify(restTemplate, times(1)).exchange(
            anyString(), 
            eq(HttpMethod.POST), 
            any(HttpEntity.class), 
            eq(Map.class)
        );
    }
}