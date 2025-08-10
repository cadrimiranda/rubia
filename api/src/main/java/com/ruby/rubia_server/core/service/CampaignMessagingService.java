package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignMessagingService {

    private final MessagingService messagingService;
    private final CampaignDelaySchedulingService delaySchedulingService;
    private final CampaignMessagingProperties properties;
    private final ChatLidMappingService chatLidMappingService;
    private final ConversationService conversationService;
    private final SecureCampaignQueueService secureCampaignQueueService;

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
                        null // instanceId será preenchido quando cliente responder
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
            // O SecureCampaignQueueService processará novamente após o delay
            log.info("🔄 Re-adicionando contato {} à fila Redis para retry em {}ms", 
                    campaignContact.getId(), retryDelayMs);
            
            // Adicionar com delay usando agendamento
            delaySchedulingService.scheduleTask(
                campaignContact, 
                retryDelayMs, 
                () -> {
                    try {
                        secureCampaignQueueService.addContactForRetry(
                            campaignContact.getCampaign().getId(),
                            campaignContact.getId(),
                            campaignContact.getCustomer().getCompany().getId().toString()
                        );
                        log.info("✅ Contato {} re-adicionado à fila Redis para retry", 
                                campaignContact.getId());
                    } catch (Exception e) {
                        log.error("❌ Erro ao re-adicionar contato {} à fila Redis: {}", 
                                campaignContact.getId(), e.getMessage(), e);
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("❌ Erro no agendamento de retry para contato {}: {}", 
                    campaignContact.getId(), e.getMessage(), e);
        }
    }
}