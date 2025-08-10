package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Campaign Messaging Service - Retry System Tests")
class CampaignMessagingServiceRetryTest {

    @Mock
    private MessagingService messagingService;

    @Mock
    private CampaignDelaySchedulingService delaySchedulingService;

    @Mock
    private CampaignMessagingProperties properties;

    @Mock
    private CampaignContact campaignContact;

    @Mock
    private Customer customer;

    @Mock
    private Campaign campaign;

    @Mock
    private MessageTemplate messageTemplate;

    @Mock
    private MessageResult messageResult;

    private CampaignMessagingService service;

    @BeforeEach
    void setUp() {
        // Mock the new dependencies
        ChatLidMappingService mockChatLidMappingService = mock(ChatLidMappingService.class);
        ConversationService mockConversationService = mock(ConversationService.class);
        SecureCampaignQueueService mockSecureCampaignQueueService = mock(SecureCampaignQueueService.class);
        
        service = new CampaignMessagingService(messagingService, delaySchedulingService, properties, 
                                             mockChatLidMappingService, mockConversationService, mockSecureCampaignQueueService);
        
        // Setup basic mocks
        when(campaignContact.getId()).thenReturn(UUID.randomUUID());
        when(campaignContact.getCustomer()).thenReturn(customer);
        when(campaignContact.getCampaign()).thenReturn(campaign);
        when(customer.getPhone()).thenReturn("+5511999999999");
        when(customer.getName()).thenReturn("Test Customer");
        when(campaign.getInitialMessageTemplate()).thenReturn(messageTemplate);
        when(messageTemplate.getContent()).thenReturn("Olá {{nome}}, esta é uma mensagem de teste!");
        when(messageTemplate.getId()).thenReturn(UUID.randomUUID());
        
        // Setup properties defaults
        when(properties.getMinDelayMs()).thenReturn(15000);
        when(properties.getMaxDelayMs()).thenReturn(45000);
        when(properties.getMaxRetries()).thenReturn(3);
        when(properties.getRetryDelayMs()).thenReturn(5000);
    }

    @Test
    @DisplayName("Should succeed on first attempt without retry")
    void shouldSucceedOnFirstAttemptWithoutRetry() throws Exception {
        // Given
        when(messageResult.isSuccess()).thenReturn(true);
        when(messageResult.getMessageId()).thenReturn("msg-123");
        when(messagingService.sendMessage(anyString(), anyString())).thenReturn(messageResult);
        
        // When
        boolean result = invokePerformActualSendWithRetry();
        
        // Then
        assertThat(result).isTrue();
        verify(messagingService, times(1)).sendMessage(eq("+5511999999999"), anyString());
        verifyNoMoreInteractions(messagingService);
    }

    @Test
    @DisplayName("Should retry on failure and succeed on second attempt")
    void shouldRetryOnFailureAndSucceedOnSecondAttempt() throws Exception {
        // Given
        when(messageResult.isSuccess())
            .thenReturn(false)  // Primeira tentativa falha
            .thenReturn(true);  // Segunda tentativa sucesso
        when(messageResult.getError()).thenReturn("Temporary failure");
        when(messageResult.getMessageId()).thenReturn("msg-456");
        when(messagingService.sendMessage(anyString(), anyString())).thenReturn(messageResult);
        
        // When
        boolean result = invokePerformActualSendWithRetry();
        
        // Then
        assertThat(result).isTrue();
        verify(messagingService, times(2)).sendMessage(eq("+5511999999999"), anyString());
    }

    @Test
    @DisplayName("Should fail after maximum retry attempts")
    void shouldFailAfterMaximumRetryAttempts() throws Exception {
        // Given
        when(messageResult.isSuccess()).thenReturn(false);
        when(messageResult.getError()).thenReturn("Persistent failure");
        when(messagingService.sendMessage(anyString(), anyString())).thenReturn(messageResult);
        
        // When
        boolean result = invokePerformActualSendWithRetry();
        
        // Then
        assertThat(result).isFalse();
        verify(messagingService, times(3)).sendMessage(eq("+5511999999999"), anyString());
    }

    @Test
    @DisplayName("Should handle exception during message sending")
    void shouldHandleExceptionDuringMessageSending() throws Exception {
        // Given
        when(messagingService.sendMessage(anyString(), anyString()))
            .thenThrow(new RuntimeException("Network error"));
        
        // When
        boolean result = invokePerformActualSendWithRetry();
        
        // Then
        assertThat(result).isFalse();
        verify(messagingService, times(3)).sendMessage(eq("+5511999999999"), anyString());
    }

    @Test
    @DisplayName("Should retry with correct delay between attempts")
    void shouldRetryWithCorrectDelayBetweenAttempts() throws Exception {
        // Given
        when(messageResult.isSuccess()).thenReturn(false);
        when(messageResult.getError()).thenReturn("Temporary failure");
        when(messagingService.sendMessage(anyString(), anyString())).thenReturn(messageResult);
        
        long startTime = System.currentTimeMillis();
        
        // When
        boolean result = invokePerformActualSendWithRetry();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Then
        assertThat(result).isFalse();
        
        // Verificar que o tempo de execução inclui os delays de retry
        // 2 retries * 5000ms = pelo menos 10 segundos
        assertThat(executionTime).isGreaterThanOrEqualTo(10000);
        
        verify(messagingService, times(3)).sendMessage(eq("+5511999999999"), anyString());
    }

    @Test
    @DisplayName("Should personalize message template correctly")
    void shouldPersonalizeMessageTemplateCorrectly() throws Exception {
        // Given
        when(messageResult.isSuccess()).thenReturn(true);
        when(messageResult.getMessageId()).thenReturn("msg-789");
        when(messagingService.sendMessage(anyString(), anyString())).thenReturn(messageResult);
        
        // When
        boolean result = invokePerformActualSendWithRetry();
        
        // Then
        assertThat(result).isTrue();
        
        // Verificar que a mensagem foi personalizada corretamente
        verify(messagingService).sendMessage(
            eq("+5511999999999"), 
            eq("Olá Test Customer, esta é uma mensagem de teste!")
        );
    }

    @Test
    @DisplayName("Should handle null customer name in template")
    void shouldHandleNullCustomerNameInTemplate() throws Exception {
        // Given
        when(customer.getName()).thenReturn(null);
        when(messageResult.isSuccess()).thenReturn(true);
        when(messageResult.getMessageId()).thenReturn("msg-null");
        when(messagingService.sendMessage(anyString(), anyString())).thenReturn(messageResult);
        
        // When
        boolean result = invokePerformActualSendWithRetry();
        
        // Then
        assertThat(result).isTrue();
        
        // Verificar que {{nome}} foi substituído por string vazia
        verify(messagingService).sendMessage(
            eq("+5511999999999"), 
            eq("Olá , esta é uma mensagem de teste!")
        );
    }

    @Test
    @DisplayName("Should validate contact before processing")
    void shouldValidateContactBeforeProcessing() throws Exception {
        // Given - contact inválido (telefone null)
        when(customer.getPhone()).thenReturn(null);
        
        // When
        CompletableFuture<Boolean> future = service.sendSingleMessageAsync(campaignContact);
        
        // Then
        assertThat(future.join()).isFalse();
        verifyNoInteractions(delaySchedulingService);
        verifyNoInteractions(messagingService);
    }

    @Test
    @DisplayName("Should validate campaign contact has customer")
    void shouldValidateCampaignContactHasCustomer() throws Exception {
        // Given
        when(campaignContact.getCustomer()).thenReturn(null);
        
        // When
        CompletableFuture<Boolean> future = service.sendSingleMessageAsync(campaignContact);
        
        // Then
        assertThat(future.join()).isFalse();
        verifyNoInteractions(delaySchedulingService);
        verifyNoInteractions(messagingService);
    }

    @Test
    @DisplayName("Should validate campaign contact has campaign")
    void shouldValidateCampaignContactHasCampaign() throws Exception {
        // Given
        when(campaignContact.getCampaign()).thenReturn(null);
        
        // When
        CompletableFuture<Boolean> future = service.sendSingleMessageAsync(campaignContact);
        
        // Then
        assertThat(future.join()).isFalse();
        verifyNoInteractions(delaySchedulingService);
        verifyNoInteractions(messagingService);
    }

    @Test
    @DisplayName("Should validate campaign has message template")
    void shouldValidateCampaignHasMessageTemplate() throws Exception {
        // Given
        when(campaign.getInitialMessageTemplate()).thenReturn(null);
        
        // When
        CompletableFuture<Boolean> future = service.sendSingleMessageAsync(campaignContact);
        
        // Then
        assertThat(future.join()).isFalse();
        verifyNoInteractions(delaySchedulingService);
        verifyNoInteractions(messagingService);
    }

    @Test
    @DisplayName("Should validate message template has content")
    void shouldValidateMessageTemplateHasContent() throws Exception {
        // Given
        when(messageTemplate.getContent()).thenReturn("");
        
        // When
        CompletableFuture<Boolean> future = service.sendSingleMessageAsync(campaignContact);
        
        // Then
        assertThat(future.join()).isFalse();
        verifyNoInteractions(delaySchedulingService);
        verifyNoInteractions(messagingService);
    }

    @Test
    @DisplayName("Should calculate random delay within configured range")
    void shouldCalculateRandomDelayWithinConfiguredRange() throws Exception {
        // Given
        when(delaySchedulingService.scheduleMessageSend(any(), anyInt(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // When
        service.sendSingleMessageAsync(campaignContact);
        
        // Then
        verify(delaySchedulingService).scheduleMessageSend(
            eq(campaignContact),
            intThat(delay -> delay >= 15000 && delay <= 45000),
            any()
        );
    }

    // Helper method to invoke private method using reflection
    private boolean invokePerformActualSendWithRetry() throws Exception {
        return (boolean) ReflectionTestUtils.invokeMethod(
            service, "performActualSendWithRetry", campaignContact);
    }
}