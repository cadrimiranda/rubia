package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.CampaignContact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignDelaySchedulingService {

    private final TaskScheduler taskScheduler;
    private final CampaignMessagingProperties properties;

    /**
     * Agenda o envio de uma mensagem respeitando horário comercial e delay específico
     * 
     * @param contact Contato da campanha
     * @param delayMs Delay em milissegundos
     * @param messageTask Tarefa de envio da mensagem
     * @return CompletableFuture que será completado quando a mensagem for enviada
     */
    public CompletableFuture<Boolean> scheduleMessageSend(CampaignContact contact,
                                                         int delayMs,
                                                         MessageSendTask messageTask) {
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Instant scheduledTime = calculateScheduledTime(delayMs);

        log.debug("Agendando envio de mensagem para contato {} em {}", 
                contact.getId(), scheduledTime);

        taskScheduler.schedule(() -> {
            try {
                log.debug("Executando envio agendado para contato {}", contact.getId());
                boolean result = messageTask.execute();
                future.complete(result);
                
                log.debug("Envio agendado concluído para contato {} com resultado: {}", 
                        contact.getId(), result);
                        
            } catch (Exception e) {
                log.error("Erro no envio agendado para contato {}: {}", 
                        contact.getId(), e.getMessage(), e);
                future.completeExceptionally(e);
            }
        }, scheduledTime);

        return future;
    }

    /**
     * Calcula o horário de agendamento respeitando horário comercial
     */
    private Instant calculateScheduledTime(int delayMs) {
        Instant proposedTime = Instant.now().plusMillis(delayMs);
        
        log.debug("🕐 calculateScheduledTime: delayMs={}, proposedTime={}, businessHoursOnly={}", 
                delayMs, proposedTime, properties.isBusinessHoursOnly());
        
        if (!properties.isBusinessHoursOnly()) {
            log.debug("🕐 Horário comercial desabilitado, usando horário proposto: {}", proposedTime);
            return proposedTime;
        }

        LocalTime proposedLocalTime = LocalTime.ofInstant(proposedTime, ZoneId.systemDefault());
        LocalTime businessStart = LocalTime.of(properties.getBusinessStartHour(), 0);
        LocalTime businessEnd = LocalTime.of(properties.getBusinessEndHour(), 0);

        log.debug("🕐 Horários: proposto={}, início={}, fim={}, dentro={}", 
                proposedLocalTime, businessStart, businessEnd, 
                proposedLocalTime.isAfter(businessStart) && proposedLocalTime.isBefore(businessEnd));

        // Se está dentro do horário comercial, mantém o horário proposto
        if (proposedLocalTime.isAfter(businessStart) && proposedLocalTime.isBefore(businessEnd)) {
            log.debug("🕐 Dentro do horário comercial, mantendo: {}", proposedTime);
            return proposedTime;
        }

        // Se é antes do horário comercial, agenda para o início do horário
        if (proposedLocalTime.isBefore(businessStart)) {
            return proposedTime.atZone(ZoneId.systemDefault())
                    .with(businessStart)
                    .toInstant();
        }

        // Se é depois do horário comercial, agenda para o próximo dia útil
        return proposedTime.atZone(ZoneId.systemDefault())
                .plusDays(1)
                .with(businessStart)
                .toInstant();
    }

    /**
     * Interface funcional para tarefas de envio de mensagem
     */
    @FunctionalInterface
    public interface MessageSendTask {
        boolean execute() throws Exception;
    }

    /**
     * Versão simplificada que aceita um Runnable
     */
    public CompletableFuture<Void> scheduleTask(CampaignContact contact,
                                               int delayMs,
                                               Runnable task) {
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        Instant scheduledTime = Instant.now().plusMillis(delayMs);

        log.debug("Agendando tarefa para contato {} em {} ms", contact.getId(), delayMs);

        taskScheduler.schedule(() -> {
            try {
                log.debug("Executando tarefa agendada para contato {}", contact.getId());
                task.run();
                future.complete(null);
                
            } catch (Exception e) {
                log.error("Erro na tarefa agendada para contato {}: {}", 
                        contact.getId(), e.getMessage(), e);
                future.completeExceptionally(e);
            }
        }, scheduledTime);

        return future;
    }
}