package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.MessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignMessagingService {

    private final MessagingService messagingService;
    private final CampaignDelaySchedulingService delaySchedulingService;
    private final CampaignMessagingProperties properties;

    /**
     * Envia uma única mensagem para um contato da campanha de forma assíncrona
     * Usado pelo sistema de filas para enviar mensagens de forma controlada
     */
    @Async
    public CompletableFuture<Boolean> sendSingleMessageAsync(CampaignContact campaignContact) {
        // Validações iniciais (síncronas)
        if (!validateContact(campaignContact)) {
            return CompletableFuture.completedFuture(false);
        }

        // Calcular delay
        int delay = calculateRandomDelay();

        // Retornar Future que será resolvido após o delay com retry automático
        return delaySchedulingService.scheduleMessageSend(
            campaignContact,
            delay,
            () -> performActualSendWithRetry(campaignContact)
        );
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
     * Calcula delay aleatório dentro do range WHAPI configurável
     */
    private int calculateRandomDelay() {
        int minDelay = properties.getMinDelayMs();
        int maxDelay = properties.getMaxDelayMs();
        return minDelay + (int)(Math.random() * (maxDelay - minDelay));
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
                log.warn("Tentativa {} falhou para contato {}. Tentando novamente em {}ms", 
                        attempt, campaignContact.getId(), retryDelay);
                
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrompido para contato {}", campaignContact.getId());
                    return false;
                }
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

            // Enviar via MessagingService
            MessageResult result = messagingService.sendMessage(
                customerPhone, 
                personalizedMessage
            );

            boolean success = result.isSuccess();

            if (success) {
                log.debug("Mensagem enviada com sucesso para contato {}: {} - MessageId: {}", 
                        campaignContact.getId(), customerPhone, result.getMessageId());
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
}