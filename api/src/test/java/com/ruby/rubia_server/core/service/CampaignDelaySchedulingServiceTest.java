package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignDelaySchedulingServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private CampaignMessagingProperties properties;

    @InjectMocks
    private CampaignDelaySchedulingService schedulingService;

    private CampaignContact testContact;

    @BeforeEach
    void setUp() {
        Campaign campaign = new Campaign();
        campaign.setId(UUID.randomUUID());

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setPhone("+1234567890");

        testContact = new CampaignContact();
        testContact.setId(UUID.randomUUID());
        testContact.setCampaign(campaign);
        testContact.setCustomer(customer);

        // Mock CampaignMessagingProperties
        when(properties.isBusinessHoursOnly()).thenReturn(false);
        when(properties.getBusinessStartHour()).thenReturn(9);
        when(properties.getBusinessEndHour()).thenReturn(18);
    }

    @Test
    void scheduleMessageSend_WithValidTask_ShouldScheduleAndReturnFuture() throws InterruptedException, ExecutionException {
        // Arrange
        int delayMs = 1000;
        CampaignDelaySchedulingService.MessageSendTask messageTask = () -> true;

        // Captura o Runnable agendado para simular execução
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);

        // Act
        CompletableFuture<Boolean> future = schedulingService.scheduleMessageSend(
            testContact, delayMs, messageTask);

        // Assert
        assertNotNull(future);
        assertFalse(future.isDone());

        verify(taskScheduler).schedule(runnableCaptor.capture(), instantCaptor.capture());

        // Simular execução da tarefa agendada
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Verificar que o Future foi completado
        assertTrue(future.isDone());
        assertTrue(future.get());

        // Verificar que o delay foi aplicado corretamente
        Instant scheduledTime = instantCaptor.getValue();
        long actualDelay = scheduledTime.toEpochMilli() - Instant.now().toEpochMilli();
        assertTrue(actualDelay >= delayMs - 100 && actualDelay <= delayMs + 100); // Tolerância de 100ms
    }

    @Test
    void scheduleMessageSend_WhenTaskThrowsException_ShouldCompleteExceptionally() {
        // Arrange
        RuntimeException testException = new RuntimeException("Test exception");
        CampaignDelaySchedulingService.MessageSendTask messageTask = () -> {
            throw testException;
        };

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        // Act
        CompletableFuture<Boolean> future = schedulingService.scheduleMessageSend(
            testContact, 1000, messageTask);

        // Assert
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));

        // Simular execução da tarefa agendada
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Verificar que o Future foi completado com exceção
        assertTrue(future.isCompletedExceptionally());
        
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertEquals(testException, exception.getCause());
    }

    @Test
    void scheduleTask_WithRunnable_ShouldScheduleAndComplete() throws InterruptedException, ExecutionException {
        // Arrange
        int delayMs = 500;
        boolean[] taskExecuted = {false};
        Runnable task = () -> taskExecuted[0] = true;

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        // Act
        CompletableFuture<Void> future = schedulingService.scheduleTask(testContact, delayMs, task);

        // Assert
        assertNotNull(future);
        assertFalse(future.isDone());

        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));

        // Simular execução da tarefa agendada
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Verificar que o Future foi completado e a tarefa executada
        assertTrue(future.isDone());
        assertTrue(taskExecuted[0]);
        assertNull(future.get()); // CompletableFuture<Void> retorna null
    }

    @Test
    void scheduleTask_WhenRunnableThrowsException_ShouldCompleteExceptionally() {
        // Arrange
        RuntimeException testException = new RuntimeException("Runnable exception");
        Runnable task = () -> {
            throw testException;
        };

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        // Act
        CompletableFuture<Void> future = schedulingService.scheduleTask(testContact, 1000, task);

        // Assert
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));

        // Simular execução da tarefa agendada
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Verificar que o Future foi completado com exceção
        assertTrue(future.isCompletedExceptionally());
        
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertEquals(testException, exception.getCause());
    }

    @Test
    void scheduleMessageSend_WithZeroDelay_ShouldScheduleImmediately() {
        // Arrange
        CampaignDelaySchedulingService.MessageSendTask messageTask = () -> true;

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);

        // Act
        schedulingService.scheduleMessageSend(testContact, 0, messageTask);

        // Assert
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());

        Instant scheduledTime = instantCaptor.getValue();
        long delay = scheduledTime.toEpochMilli() - Instant.now().toEpochMilli();
        assertTrue(delay <= 10); // Deve ser praticamente imediato (tolerância de 10ms)
    }
}