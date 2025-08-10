package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.event.CampaignRetryEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignMessagingService {

    private final MessagingService messagingService;
    private final CampaignDelaySchedulingService delaySchedulingService;
    private final CampaignMessagingProperties properties;
    private final ChatLidMappingService chatLidMappingService;
    private final ConversationService conversationService;
    private final ApplicationEventPublisher eventPublisher;
    
    @Qualifier("scheduledExecutor")
    private final ScheduledExecutorService scheduledExecutor;
    private final MeterRegistry meterRegistry;

    /**
     * Envia uma única mensagem para um contato da campanha de forma assíncrona
     * Usado pelo sistema de filas para enviar mensagens de forma controlada
     * Implementa retry com exponential backoff e jitter
     */
    @Async("campaignExecutor")
    public CompletableFuture<Boolean> sendSingleMessageAsync(CampaignContact campaignContact) {
        // Validações iniciais (síncronas)
        if (!validateContact(campaignContact)) {
            recordMetrics(campaignContact, false, new IllegalArgumentException("Invalid contact"));
            return CompletableFuture.completedFuture(false);
        }

        // Calcular delay inicial
        int initialDelay = calculateRandomDelay();
        
        log.debug("📅 Agendando envio para {} com delay de {}ms", 
                campaignContact.getCustomer().getPhone(), initialDelay);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        ScheduledFuture<?> scheduledTask = scheduledExecutor.schedule(() -> {
            sendWithRetry(campaignContact, 1)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        recordMetrics(campaignContact, false, throwable);
                    } else {
                        future.complete(result);
                        recordMetrics(campaignContact, result, null);
                    }
                });
        }, initialDelay, TimeUnit.MILLISECONDS);
        
        // Permite cancelamento se necessário
        future.whenComplete((r, t) -> {
            if (future.isCancelled()) {
                scheduledTask.cancel(false);
            }
        });
        
        return future;
    }


    /**
     * Valida o contato da campanha
     */
    private boolean validateContact(CampaignContact campaignContact) {
        if (campaignContact == null) {
            log.error("CampaignContact é null");
            return false;
        }

        if (campaignContact.getCustomer() == null) {
            log.error("Customer é null para CampaignContact {}", campaignContact.getId());
            return false;
        }

        if (campaignContact.getCampaign() == null) {
            log.error("Campaign é null para CampaignContact {}", campaignContact.getId());
            return false;
        }

        String customerPhone = campaignContact.getCustomer().getPhone();
        if (customerPhone == null || customerPhone.trim().isEmpty()) {
            log.error("Telefone do customer está vazio para CampaignContact {}", campaignContact.getId());
            return false;
        }

        MessageTemplate template = campaignContact.getCampaign().getInitialMessageTemplate();
        if (template == null) {
            log.error("Nenhum template de mensagem encontrado para a campanha {}", 
                    campaignContact.getCampaign().getId());
            return false;
        }
        
        String templateContent = template.getContent();
        if (templateContent == null || templateContent.trim().isEmpty()) {
            log.error("Conteúdo do template está vazio para o template {}", template.getId());
            return false;
        }

        return true;
    }

    /**
     * Implementa retry com exponential backoff e jitter
     */
    private CompletableFuture<Boolean> sendWithRetry(CampaignContact contact, int attempt) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                log.info("📤 Tentativa {}/{} - Enviando para {}", 
                        attempt, properties.getMaxRetries(), contact.getCustomer().getPhone());
                        
                boolean result = performActualSend(contact);
                sample.stop(meterRegistry.timer("campaign.send.duration", 
                        "attempt", String.valueOf(attempt), "success", String.valueOf(result)));
                return result;
            } catch (Exception e) {
                sample.stop(meterRegistry.timer("campaign.send.duration", 
                        "attempt", String.valueOf(attempt), "error", "true"));
                throw new CompletionException(e);
            }
        }, scheduledExecutor).handle((result, throwable) -> {
            if (throwable != null) {
                // Converte exceção em resultado de falha
                log.error("❌ Exceção na tentativa {}: {}", attempt, throwable.getMessage());
                return false;
            }
            return result;
        }).thenCompose(result -> {
            if (result) {
                log.info("✅ Mensagem enviada com sucesso para {} na tentativa {}", 
                        contact.getCustomer().getPhone(), attempt);
                return CompletableFuture.completedFuture(true);
            }
            
            if (attempt >= properties.getMaxRetries()) {
                log.error("❌ Falha após {} tentativas para {}", 
                        properties.getMaxRetries(), contact.getCustomer().getPhone());
                return CompletableFuture.completedFuture(false);
            }
            
            long retryDelay = calculateRetryDelayWithJitter(attempt);
            log.warn("⚠️ Falha na tentativa {}. Retry {} em {}ms para {}", 
                    attempt, attempt + 1, retryDelay, contact.getCustomer().getPhone());
            
            CompletableFuture<Boolean> retryFuture = new CompletableFuture<>();
            
            scheduledExecutor.schedule(() -> {
                sendWithRetry(contact, attempt + 1)
                    .whenComplete((retryResult, retryThrowable) -> {
                        if (retryThrowable != null) {
                            retryFuture.completeExceptionally(retryThrowable);
                        } else {
                            retryFuture.complete(retryResult);
                        }
                    });
            }, retryDelay, TimeUnit.MILLISECONDS);
            
            return retryFuture;
        });
    }

    /**
     * Calcula delay aleatório dentro do range WHAPI configurável
     */
    private int calculateRandomDelay() {
        int minDelay = properties.getMinDelayMs();
        int maxDelay = properties.getMaxDelayMs();
        return ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
    }
    
    /**
     * Calcula delay de retry com exponential backoff e jitter
     */
    private long calculateRetryDelayWithJitter(int attempt) {
        // Exponential backoff com jitter
        long baseDelay = Math.min(properties.getRetryDelayMs() * (1L << (attempt - 1)), 30000L);
        long jitter = ThreadLocalRandom.current().nextLong(0, baseDelay / 4);
        return baseDelay + jitter;
    }

    /**
     * Executa o envio com retry automático
     */
    private boolean performActualSendWithRetry(CampaignContact campaignContact) {
        int maxRetries = properties.getMaxRetries();
        int retryDelay = properties.getRetryDelayMs();
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            boolean success = performActualSend(campaignContact);
            
            if (success) {
                if (attempt > 1) {
                    log.info("Mensagem enviada com sucesso na tentativa {} para contato {}", 
                            attempt, campaignContact.getId());
                }
                return true;
            }
            
            if (attempt < maxRetries) {
                log.warn("Tentativa {} falhou para contato {}. Re-adicionando à fila Redis para retry em {}ms", 
                        attempt, campaignContact.getId(), retryDelay);
                
                // Re-adicionar à fila Redis com delay para retry não-bloqueante
                // Isso é melhor que Thread.sleep() pois não bloqueia threads
                reAddToRedisForRetry(campaignContact, retryDelay);
                return false; // Falha atual, mas será retentado via Redis
            }
        }
        
        log.error("Falha definitiva após {} tentativas para contato {}", 
                maxRetries, campaignContact.getId());
        return false;
    }

    /**
     * Executa o envio real da mensagem (sem Thread.sleep)
     */
    private boolean performActualSend(CampaignContact campaignContact) {
        
        try {
            String customerPhone = campaignContact.getCustomer().getPhone();
            
            
            MessageTemplate template = campaignContact.getCampaign().getInitialMessageTemplate();
            String templateContent = template.getContent();
            
            
            // Personalizar mensagem substituindo variáveis
            String personalizedMessage = personalizeMessage(templateContent, campaignContact);
            log.info("🚀 Mensagem personalizada: {}", personalizedMessage.substring(0, Math.min(50, personalizedMessage.length())) + "...");

            // Enviar via MessagingService com contexto da empresa
            log.info("🚀 CHAMANDO MessagingService.sendMessage para telefone: {}", customerPhone);
            MessageResult result = messagingService.sendMessage(
                customerPhone, 
                personalizedMessage,
                campaignContact.getCustomer().getCompany()
            );
            log.info("🚀 RESULTADO do MessagingService: success={}, messageId={}, error={}", 
                    result.isSuccess(), result.getMessageId(), result.getError());

            boolean success = result.isSuccess();

            if (success) {
                log.debug("Mensagem enviada com sucesso para contato {}: {} - MessageId: {}", 
                        campaignContact.getId(), customerPhone, result.getMessageId());
                
                // Criar mapping de campanha para facilitar resposta do cliente
                createCampaignMapping(campaignContact, customerPhone);
            } else {
                log.warn("Falha ao enviar mensagem para contato {}: {} - Erro: {}", 
                        campaignContact.getId(), customerPhone, result.getError());
            }

            return success;

        } catch (Exception e) {
            log.error("Erro no envio real da mensagem para contato {}: {}", 
                    campaignContact.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Personaliza o conteúdo da mensagem substituindo variáveis
     */
    private String personalizeMessage(String template, CampaignContact campaignContact) {
        String message = template;
        
        // Substituir variáveis do cliente
        if (campaignContact.getCustomer() != null) {
            String customerName = campaignContact.getCustomer().getName();
            if (customerName != null) {
                message = message.replace("{{nome}}", customerName);
            } else {
                message = message.replace("{{nome}}", "");
            }
        }
        
        // Aqui podem ser adicionadas outras substituições de variáveis no futuro
        // message = message.replace("{{empresa}}", company.getName());
        // message = message.replace("{{data}}", LocalDate.now().format(formatter));
        
        return message;
    }

    /**
     * Cria mapping de campanha para conversa quando mensagem é enviada
     * Permite que resposta do cliente seja associada à conversa correta
     */
    private void createCampaignMapping(CampaignContact campaignContact, String customerPhone) {
        try {
            log.debug("🔗 Criando mapping de campanha para contato: {}", campaignContact.getId());

            // Buscar conversa associada ao customer
            List<ConversationDTO> conversations = conversationService
                .findByCustomerAndCompany(campaignContact.getCustomer().getId(), campaignContact.getCustomer().getCompany().getId());
            
            Optional<ConversationDTO> conversation = conversations.stream()
                .findFirst(); // Pegar a primeira conversa (mais recente seria ideal, mas usar primeira)

            if (conversation.isPresent()) {
                // Verificar se já existe mapping para esta conversa
                if (chatLidMappingService.findMappingByConversationId(conversation.get().getId()).isEmpty()) {
                    chatLidMappingService.createMappingForCampaign(
                        conversation.get().getId(),
                        customerPhone,
                        campaignContact.getCustomer().getCompany().getId(),
                        null, // instanceId será preenchido quando cliente responder
                        campaignContact.getCampaign().getId() // ID da campanha
                    );
                    log.info("🔗 Mapping de campanha criado para conversa: {}", conversation.get().getId());
                } else {
                    log.debug("Mapping já existe para conversa: {}", conversation.get().getId());
                }
            } else {
                log.warn("Nenhuma conversa encontrada para criar mapping de campanha: {}", customerPhone);
            }

        } catch (Exception e) {
            // Não falhar o envio da campanha se mapping falhar
            log.warn("Erro ao criar mapping de campanha para contato {}: {}", 
                    campaignContact.getId(), e.getMessage());
        }
    }

    /**
     * Re-adiciona contato à fila Redis para retry com delay
     * Solução não-bloqueante para retry sem usar Thread.sleep()
     */
    private void reAddToRedisForRetry(CampaignContact campaignContact, int retryDelayMs) {
        try {
            // Re-adicionar à fila Redis com delay
            // O CampaignQueueProcessor processará novamente após o delay
            log.info("🔄 Re-adicionando contato {} à fila Redis para retry em {}ms", 
                    campaignContact.getId(), retryDelayMs);
            
            // Adicionar com delay usando agendamento
            delaySchedulingService.scheduleTask(
                campaignContact, 
                retryDelayMs, 
                () -> {
                    try {
                        // Publish retry event instead of direct service call
                        CampaignRetryEvent retryEvent = new CampaignRetryEvent(
                            this,
                            campaignContact.getCampaign().getId(),
                            campaignContact.getId(),
                            campaignContact.getCustomer().getCompany().getId().toString()
                        );
                        eventPublisher.publishEvent(retryEvent);
                        log.info("✅ Contato {} evento de retry publicado", 
                                campaignContact.getId());
                    } catch (Exception e) {
                        log.error("❌ Erro ao publicar evento de retry para contato {}: {}", 
                                campaignContact.getId(), e.getMessage(), e);
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("❌ Erro no agendamento de retry para contato {}: {}", 
                    campaignContact.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Registra métricas de processamento
     */
    private void recordMetrics(CampaignContact contact, boolean success, Throwable error) {
        String campaignId = contact.getCampaign() != null ? 
            contact.getCampaign().getId().toString() : "unknown";
        String companyId = contact.getCustomer() != null && contact.getCustomer().getCompany() != null ?
            contact.getCustomer().getCompany().getId().toString() : "unknown";
            
        meterRegistry.counter("campaign.messages.total",
            "campaign_id", campaignId,
            "company_id", companyId,
            "status", success ? "success" : "failed",
            "error", error != null ? error.getClass().getSimpleName() : "none"
        ).increment();
        
        // Registra duração total do processamento se houver timestamp
        if (contact.getCreatedAt() != null) {
            Duration processingTime = Duration.between(
                contact.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(), 
                Instant.now()
            );
            meterRegistry.timer("campaign.processing.duration",
                "campaign_id", campaignId,
                "company_id", companyId
            ).record(processingTime);
        }
    }
}