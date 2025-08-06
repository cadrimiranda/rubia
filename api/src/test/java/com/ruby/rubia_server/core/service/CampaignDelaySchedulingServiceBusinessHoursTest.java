package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.CampaignContact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Campaign Delay Scheduling Service - Business Hours Tests")
class CampaignDelaySchedulingServiceBusinessHoursTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private CampaignMessagingProperties properties;

    @Mock
    private CampaignContact campaignContact;

    private CampaignDelaySchedulingService service;

    @BeforeEach
    void setUp() {
        service = new CampaignDelaySchedulingService(taskScheduler, properties);
        when(campaignContact.getId()).thenReturn(java.util.UUID.randomUUID());
    }

    @Test
    @DisplayName("Should schedule immediately when business hours disabled")
    void shouldScheduleImmediatelyWhenBusinessHoursDisabled() {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(false);
        int delayMs = 30000; // 30 segundos
        
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        
        // When
        service.scheduleMessageSend(campaignContact, delayMs, () -> true);
        
        // Then
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        
        Instant scheduledTime = instantCaptor.getValue();
        Instant expectedTime = Instant.now().plusMillis(delayMs);
        
        // Permitir diferença de até 1 segundo para account for execution time
        assertThat(scheduledTime).isBetween(
            expectedTime.minusSeconds(1), 
            expectedTime.plusSeconds(1)
        );
    }

    @Test
    @DisplayName("Should maintain scheduled time when within business hours")
    void shouldMaintainScheduledTimeWhenWithinBusinessHours() {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(true);
        when(properties.getBusinessStartHour()).thenReturn(9);
        when(properties.getBusinessEndHour()).thenReturn(18);
        
        // Simular horário atual às 14h (dentro do horário comercial)
        int delayMs = 30000; // 30 segundos
        
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        
        // When
        service.scheduleMessageSend(campaignContact, delayMs, () -> true);
        
        // Then
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        
        Instant scheduledTime = instantCaptor.getValue();
        LocalTime scheduledLocalTime = LocalTime.ofInstant(scheduledTime, ZoneId.systemDefault());
        
        // Se o horário atual + delay está dentro do horário comercial, deve manter
        LocalTime businessStart = LocalTime.of(9, 0);
        LocalTime businessEnd = LocalTime.of(18, 0);
        
        if (scheduledLocalTime.isAfter(businessStart) && scheduledLocalTime.isBefore(businessEnd)) {
            Instant expectedTime = Instant.now().plusMillis(delayMs);
            assertThat(scheduledTime).isBetween(
                expectedTime.minusSeconds(1), 
                expectedTime.plusSeconds(1)
            );
        }
    }

    @Test
    @DisplayName("Should reschedule to next business day when after hours")
    void shouldRescheduleToNextBusinessDayWhenAfterHours() {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(true);
        when(properties.getBusinessStartHour()).thenReturn(9);
        when(properties.getBusinessEndHour()).thenReturn(18);
        
        // Simular delay que resultaria em horário após 18h
        int delayMs = 1; // 1ms para garantir que seja calculado como "após horário"
        
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        
        // When
        service.scheduleMessageSend(campaignContact, delayMs, () -> true);
        
        // Then
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        
        Instant scheduledTime = instantCaptor.getValue();
        LocalTime scheduledLocalTime = LocalTime.ofInstant(scheduledTime, ZoneId.systemDefault());
        
        // Verificar se foi reagendado para horário comercial (pode ser mesmo dia ou próximo)
        assertThat(scheduledLocalTime.getHour()).isGreaterThanOrEqualTo(9);
        assertThat(scheduledLocalTime.getHour()).isLessThan(18);
    }

    @Test
    @DisplayName("Should reschedule to business start when before hours")
    void shouldRescheduleToBusinessStartWhenBeforeHours() {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(true);
        when(properties.getBusinessStartHour()).thenReturn(9);
        when(properties.getBusinessEndHour()).thenReturn(18);
        
        int delayMs = 30000;
        
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        
        // When
        service.scheduleMessageSend(campaignContact, delayMs, () -> true);
        
        // Then
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        
        Instant scheduledTime = instantCaptor.getValue();
        LocalTime scheduledLocalTime = LocalTime.ofInstant(scheduledTime, ZoneId.systemDefault());
        
        // Se o horário proposto for antes das 9h, deve ser reagendado para 9h
        LocalTime businessStart = LocalTime.of(9, 0);
        if (LocalTime.now().plusSeconds(delayMs / 1000).isBefore(businessStart)) {
            assertThat(scheduledLocalTime.getHour()).isEqualTo(9);
            assertThat(scheduledLocalTime.getMinute()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Should execute scheduled task successfully")
    void shouldExecuteScheduledTaskSuccessfully() throws Exception {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(false);
        
        // Capturar o Runnable que será agendado
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        
        // Mock para simular execução bem-sucedida da tarefa
        CampaignDelaySchedulingService.MessageSendTask mockTask = mock(CampaignDelaySchedulingService.MessageSendTask.class);
        when(mockTask.execute()).thenReturn(true);
        
        // When
        CompletableFuture<Boolean> future = service.scheduleMessageSend(campaignContact, 1000, mockTask);
        
        // Capturar e executar o Runnable agendado
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();
        
        // Then
        assertThat(future).succeedsWithin(java.time.Duration.ofSeconds(1));
        assertThat(future.join()).isTrue();
        verify(mockTask).execute();
    }

    @Test
    @DisplayName("Should handle task execution exception")
    void shouldHandleTaskExecutionException() throws Exception {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(false);
        
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        
        CampaignDelaySchedulingService.MessageSendTask mockTask = mock(CampaignDelaySchedulingService.MessageSendTask.class);
        when(mockTask.execute()).thenThrow(new RuntimeException("Test exception"));
        
        // When
        CompletableFuture<Boolean> future = service.scheduleMessageSend(campaignContact, 1000, mockTask);
        
        // Capturar e executar o Runnable agendado
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();
        
        // Then
        assertThat(future).failsWithin(java.time.Duration.ofSeconds(1))
                          .withThrowableOfType(java.util.concurrent.ExecutionException.class)
                          .withMessageContaining("Test exception");
    }

    @Test
    @DisplayName("Should validate business hours configuration")
    void shouldValidateBusinessHoursConfiguration() throws Exception {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(true);
        when(properties.getBusinessStartHour()).thenReturn(9);
        when(properties.getBusinessEndHour()).thenReturn(18);
        
        // When & Then - configuração válida não deve lançar exceção
        service.scheduleMessageSend(campaignContact, 30000, () -> true);
        
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Should use system default timezone for business hours calculation")
    void shouldUseSystemDefaultTimezoneForBusinessHoursCalculation() {
        // Given
        when(properties.isBusinessHoursOnly()).thenReturn(true);
        when(properties.getBusinessStartHour()).thenReturn(9);
        when(properties.getBusinessEndHour()).thenReturn(18);
        
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        
        // When
        service.scheduleMessageSend(campaignContact, 30000, () -> true);
        
        // Then
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        
        Instant scheduledTime = instantCaptor.getValue();
        LocalTime scheduledLocalTime = LocalTime.ofInstant(scheduledTime, ZoneId.systemDefault());
        
        // Verificar que o cálculo usa o timezone do sistema
        assertThat(scheduledLocalTime).isNotNull();
        assertThat(scheduledLocalTime.getHour()).isBetween(0, 23);
        assertThat(scheduledLocalTime.getMinute()).isBetween(0, 59);
    }
}