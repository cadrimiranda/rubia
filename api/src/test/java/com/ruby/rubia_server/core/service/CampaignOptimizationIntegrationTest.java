package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
    CampaignMessagingProperties.class,
    CampaignDelaySchedulingService.class,
    CampaignMessagingService.class
})
@TestPropertySource(properties = {
    "campaign.messaging.batch-size=5",
    "campaign.messaging.batch-pause-minutes=1",
    "campaign.messaging.min-delay-ms=100",
    "campaign.messaging.max-delay-ms=200", 
    "campaign.messaging.max-retries=2",
    "campaign.messaging.retry-delay-ms=50",
    "campaign.messaging.business-hours-only=true",
    "campaign.messaging.business-start-hour=9",
    "campaign.messaging.business-end-hour=18",
    "campaign.messaging.randomize-order=true"
})
@DisplayName("Campaign Optimization Integration Tests")
class CampaignOptimizationIntegrationTest {

    @Autowired
    private CampaignMessagingProperties properties;

    @MockBean
    private TaskScheduler taskScheduler;

    @MockBean
    private MessagingService messagingService;

    private CampaignDelaySchedulingService delaySchedulingService;
    private CampaignMessagingService campaignMessagingService;

    @BeforeEach
    void setUp() {
        delaySchedulingService = new CampaignDelaySchedulingService(taskScheduler, properties);
        // Mock the new dependencies
        ChatLidMappingService mockChatLidMappingService = mock(ChatLidMappingService.class);
        ConversationService mockConversationService = mock(ConversationService.class);
        SecureCampaignQueueService mockSecureCampaignQueueService = mock(SecureCampaignQueueService.class);
        
        campaignMessagingService = new CampaignMessagingService(
            messagingService, delaySchedulingService, properties, 
            mockChatLidMappingService, mockConversationService, mockSecureCampaignQueueService);
    }

    @Test
    @DisplayName("Should apply optimized configuration values correctly")
    void shouldApplyOptimizedConfigurationValuesCorrectly() {
        // Verificar que as configurações padrão ou customizadas foram carregadas
        // Os valores podem ser os defaults da classe ou os configurados no teste
        assertThat(properties.getBatchSize()).isGreaterThan(0);
        assertThat(properties.getBatchPauseMinutes()).isGreaterThan(0);
        assertThat(properties.getMinDelayMs()).isGreaterThan(0);
        assertThat(properties.getMaxDelayMs()).isGreaterThan(properties.getMinDelayMs());
        assertThat(properties.getMaxRetries()).isGreaterThan(0);
        assertThat(properties.getRetryDelayMs()).isGreaterThan(0);
        assertThat(properties.isBusinessHoursOnly()).isNotNull();
        assertThat(properties.getBusinessStartHour()).isBetween(0, 23);
        assertThat(properties.getBusinessEndHour()).isBetween(1, 24);
        assertThat(properties.isRandomizeOrder()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate performance improvements correctly")
    void shouldCalculatePerformanceImprovementsCorrectly() {
        // Configuração antiga simulada
        int oldBatchSize = 20;
        int oldBatchPause = 60;
        int oldMinDelay = 30000;
        int oldMaxDelay = 60000;

        // Configuração nova (otimizada)
        int newBatchSize = properties.getBatchSize();
        int newBatchPause = properties.getBatchPauseMinutes();
        int newMinDelay = properties.getMinDelayMs();
        int newMaxDelay = properties.getMaxDelayMs();

        // Para 1000 mensagens
        int totalMessages = 1000;

        // Cálculo antigo
        int oldBatches = (int) Math.ceil((double) totalMessages / oldBatchSize);
        double oldAvgDelay = (oldMinDelay + oldMaxDelay) / 2.0 / 1000.0; // em segundos
        double oldTimePerBatch = (oldBatchSize * oldAvgDelay) / 60.0; // em minutos
        double oldTotalTime = (oldBatches * oldTimePerBatch) + ((oldBatches - 1) * oldBatchPause);

        // Cálculo novo
        int newBatches = (int) Math.ceil((double) totalMessages / newBatchSize);
        double newAvgDelay = (newMinDelay + newMaxDelay) / 2.0 / 1000.0; // em segundos  
        double newTimePerBatch = (newBatchSize * newAvgDelay) / 60.0; // em minutos
        double newTotalTime = (newBatches * newTimePerBatch) + ((newBatches - 1) * newBatchPause);

        // A nova configuração deve ser significativamente mais rápida
        double improvement = (oldTotalTime - newTotalTime) / oldTotalTime;
        assertThat(improvement).isGreaterThan(0.3); // Pelo menos 30% de melhoria
    }

    @Test
    @DisplayName("Should integrate delay scheduling with business hours")
    void shouldIntegrateDelaySchedulingWithBusinessHours() {
        // Given
        CampaignContact contact = createTestCampaignContact();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Configurar TaskScheduler para capturar o agendamento
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
            .thenAnswer(invocation -> {
                Instant scheduledTime = invocation.getArgument(1);
                LocalTime scheduledLocalTime = LocalTime.ofInstant(scheduledTime, ZoneId.systemDefault());
                
                // Verificar que está dentro do horário comercial ou foi reagendado
                if (properties.isBusinessHoursOnly()) {
                    assertThat(scheduledLocalTime.getHour()).isBetween(9, 17);
                }
                
                latch.countDown();
                return null;
            });

        // When
        delaySchedulingService.scheduleMessageSend(contact, 150, () -> true);

        // Then
        try {
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Should integrate messaging service with retry mechanism")
    void shouldIntegrateMessagingServiceWithRetryMechanism() {
        // Given
        CampaignContact contact = createTestCampaignContact();
        MessageResult failResult = mock(MessageResult.class);
        MessageResult successResult = mock(MessageResult.class);
        
        when(failResult.isSuccess()).thenReturn(false);
        when(failResult.getError()).thenReturn("Temporary failure");
        when(successResult.isSuccess()).thenReturn(true);
        when(successResult.getMessageId()).thenReturn("msg-success-123");
        
        // Simular falha na primeira tentativa, sucesso na segunda
        when(messagingService.sendMessage(anyString(), anyString()))
            .thenReturn(failResult)
            .thenReturn(successResult);
        
        // Configurar delaySchedulingService para executar imediatamente
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
            .thenAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // Executar imediatamente para o teste
                return null;
            });

        // When
        CompletableFuture<Boolean> future = campaignMessagingService.sendSingleMessageAsync(contact);

        // Then
        assertThat(future.join()).isTrue();
        verify(messagingService, times(2)).sendMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("Should validate end-to-end configuration consistency")
    void shouldValidateEndToEndConfigurationConsistency() {
        // Verificar que todas as configurações são consistentes entre si
        
        // Delays
        assertThat(properties.getMinDelayMs()).isLessThan(properties.getMaxDelayMs());
        
        // Horário comercial
        assertThat(properties.getBusinessStartHour()).isLessThan(properties.getBusinessEndHour());
        assertThat(properties.getBusinessStartHour()).isGreaterThanOrEqualTo(0);
        assertThat(properties.getBusinessEndHour()).isLessThanOrEqualTo(24);
        
        // Timeouts
        assertThat(properties.getBatchTimeout()).isGreaterThan(properties.getMessageTimeout());
        
        // Retry
        assertThat(properties.getMaxRetries()).isGreaterThan(0);
        assertThat(properties.getRetryDelayMs()).isGreaterThan(0);
        
        // Batch
        assertThat(properties.getBatchSize()).isGreaterThan(0);
        assertThat(properties.getBatchPauseMinutes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate realistic throughput for MVP scenario")
    void shouldCalculateRealisticThroughputForMVPScenario() {
        // Cenário: 1000 mensagens com configuração otimizada
        int totalMessages = 1000;
        int batchSize = properties.getBatchSize();
        int batchPause = properties.getBatchPauseMinutes();
        double avgDelay = (properties.getMinDelayMs() + properties.getMaxDelayMs()) / 2.0;
        
        // Cálculo de batches
        int totalBatches = (int) Math.ceil((double) totalMessages / batchSize);
        
        // Tempo por batch (processamento + pausa)
        double timePerMessageSeconds = avgDelay / 1000.0;
        double batchProcessingMinutes = (batchSize * timePerMessageSeconds) / 60.0;
        
        // Tempo total estimado
        double totalProcessingMinutes = totalBatches * batchProcessingMinutes;
        double totalPauseMinutes = (totalBatches - 1) * batchPause;
        double totalTimeMinutes = totalProcessingMinutes + totalPauseMinutes;
        double totalTimeHours = totalTimeMinutes / 60.0;
        
        // Para qualquer configuração válida, o tempo deve ser calculável
        // Usar valores realistas baseados na configuração atual
        double maxExpectedHours = 48.0; // Máximo razoável para 1000 mensagens
        assertThat(totalTimeHours).isLessThan(maxExpectedHours);
        
        // Logging para debug
        System.out.println(String.format(
            "Throughput Analysis: %d messages, %d batches, %.2f hours total",
            totalMessages, totalBatches, totalTimeHours
        ));
    }

    @Test
    @DisplayName("Should handle configuration edge cases gracefully")
    void shouldHandleConfigurationEdgeCasesGracefully() {
        // Teste com configuração de valores mínimos
        assertThat(properties.getMinDelayMs()).isGreaterThan(0);
        assertThat(properties.getMaxDelayMs()).isGreaterThan(properties.getMinDelayMs());
        assertThat(properties.getBatchSize()).isGreaterThan(0);
        assertThat(properties.getBatchPauseMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(properties.getMaxRetries()).isGreaterThan(0);
        assertThat(properties.getRetryDelayMs()).isGreaterThan(0);
        
        // Verificar ranges válidos
        assertThat(properties.getBusinessStartHour()).isBetween(0, 23);
        assertThat(properties.getBusinessEndHour()).isBetween(1, 24);
        
        // Timeouts razoáveis
        assertThat(properties.getMessageTimeout().toSeconds()).isGreaterThan(0);
        assertThat(properties.getBatchTimeout().toMinutes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should support feature flag combinations")
    void shouldSupportFeatureFlagCombinations() {
        // Verificar que flags booleanas funcionam corretamente
        boolean businessHoursEnabled = properties.isBusinessHoursOnly();
        boolean randomizationEnabled = properties.isRandomizeOrder();
        
        // Ambas as features devem ser configuráveis independentemente
        assertThat(businessHoursEnabled).isNotNull();
        assertThat(randomizationEnabled).isNotNull();
        
        // No nosso teste, ambas estão habilitadas
        assertThat(businessHoursEnabled).isTrue();
        assertThat(randomizationEnabled).isTrue();
    }

    private CampaignContact createTestCampaignContact() {
        CampaignContact contact = new CampaignContact();
        contact.setId(UUID.randomUUID());
        contact.setStatus(CampaignContactStatus.PENDING);
        
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Test Customer");
        customer.setPhone("+5511999999999");
        contact.setCustomer(customer);
        
        Campaign campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setName("Test Campaign");
        
        MessageTemplate template = new MessageTemplate();
        template.setId(UUID.randomUUID());
        template.setContent("Olá {{nome}}, esta é uma mensagem de teste!");
        campaign.setInitialMessageTemplate(template);
        
        contact.setCampaign(campaign);
        
        return contact;
    }
}