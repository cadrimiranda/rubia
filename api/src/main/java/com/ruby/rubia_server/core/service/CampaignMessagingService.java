package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.dto.UpdateMessageDTO;
import com.ruby.rubia_server.dto.campaign.UpdateCampaignDTO;
import com.ruby.rubia_server.core.dto.UpdateCampaignContactDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignMessagingService {

    // Configurações baseadas nas boas práticas WHAPI
    private static final int BATCH_SIZE = 20; // Lotes de 20 mensagens
    private static final int BATCH_PAUSE_MINUTES = 60; // 1 hora entre lotes
    private static final int MAX_DAILY_HOURS = 6; // Máximo 6 horas de envio por dia
    private static final int CONSERVATIVE_MIN_DELAY = 30000; // 30s (números novos)
    private static final int CONSERVATIVE_MAX_DELAY = 60000; // 60s (números novos)
    private static final int EXPERIENCED_MIN_DELAY = 5000; // 5s (números experientes)
    private static final int EXPERIENCED_MAX_DELAY = 10000; // 10s (números experientes)

    private final CampaignService campaignService;
    private final CampaignContactService campaignContactService;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final MessagingService messagingService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Async
    @Transactional
    public CompletableFuture<Void> startCampaignMessaging(UUID campaignId) {
        log.info("Iniciando envio automático de mensagens para campanha: {}", campaignId);
        
        try {
            // Verificar se a campanha existe e está ativa
            Campaign campaign = campaignService.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campanha não encontrada: " + campaignId));
            
            if (campaign.getStatus() != CampaignStatus.ACTIVE) {
                log.warn("Campanha {} não está ativa. Status atual: {}", campaignId, campaign.getStatus());
                return CompletableFuture.completedFuture(null);
            }

            // Buscar todos os contatos pendentes da campanha
            List<CampaignContact> pendingContacts = campaignContactService.findByCampaignIdAndStatus(
                campaignId, CampaignContactStatus.PENDING);
            
            log.info("Encontrados {} contatos pendentes para campanha {}", pendingContacts.size(), campaignId);
            
            if (pendingContacts.isEmpty()) {
                log.info("Nenhum contato pendente encontrado para campanha {}", campaignId);
                return CompletableFuture.completedFuture(null);
            }

            int sucessCount = 0;
            int errorCount = 0;
            int currentBatchCount = 0;

            // Processar contatos em lotes seguindo boas práticas WHAPI
            for (int i = 0; i < pendingContacts.size(); i++) {
                CampaignContact campaignContact = pendingContacts.get(i);
                try {
                    boolean success = sendMessageToContact(campaignContact);
                    if (success) {
                        sucessCount++;
                        // Atualizar status do contato para SENT
                        campaignContact.setStatus(CampaignContactStatus.SENT);
                        campaignContact.setMessageSentAt(LocalDateTime.now());
                        campaignContactService.update(campaignContact.getId(), 
                            UpdateCampaignContactDTO.builder()
                                .status(CampaignContactStatus.SENT)
                                .messageSentAt(LocalDateTime.now())
                                .build());
                        
                        log.debug("Mensagem enviada com sucesso para contato {} da campanha {}", 
                            campaignContact.getCustomer().getName(), campaignId);
                    } else {
                        errorCount++;
                        // Marcar como falha
                        campaignContactService.update(campaignContact.getId(), 
                            UpdateCampaignContactDTO.builder()
                                .status(CampaignContactStatus.FAILED)
                                .build());
                        
                        log.warn("Falha ao enviar mensagem para contato {} da campanha {}", 
                            campaignContact.getCustomer().getName(), campaignId);
                    }
                    
                    currentBatchCount++;
                    
                    // Verificar se precisa fazer pausa entre lotes
                    if (currentBatchCount >= BATCH_SIZE && i < pendingContacts.size() - 1) {
                        log.info("Lote de {} mensagens enviado. Pausando por {} minutos antes do próximo lote.", 
                                BATCH_SIZE, BATCH_PAUSE_MINUTES);
                        Thread.sleep(BATCH_PAUSE_MINUTES * 60 * 1000); // Pausa entre lotes
                        currentBatchCount = 0;
                    } else if (i < pendingContacts.size() - 1) {
                        // Delay normal entre mensagens individuais
                        int randomDelay = getRandomDelay();
                        log.debug("Aguardando {} ms antes do próximo envio", randomDelay);
                        Thread.sleep(randomDelay);
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    log.error("Erro ao processar contato {} da campanha {}: {}", 
                        campaignContact.getCustomer().getName(), campaignId, e.getMessage());
                    
                    // Marcar como falha
                    campaignContactService.update(campaignContact.getId(), 
                        UpdateCampaignContactDTO.builder()
                            .status(CampaignContactStatus.FAILED)
                            .build());
                }
            }

            // Atualizar estatísticas da campanha
            var updateCampaignDTO = UpdateCampaignDTO.builder()
                .contactsReached(campaign.getContactsReached() + sucessCount)
                .build();
            campaignService.update(campaign.getId(), updateCampaignDTO);

            log.info("Envio de mensagens concluído para campanha {}. Sucessos: {}, Erros: {}", 
                campaignId, sucessCount, errorCount);

            // Notificar via WebSocket sobre o progresso
            webSocketNotificationService.notifyCampaignProgress(campaign.getCompany().getId(), 
                campaignId, sucessCount, errorCount);

        } catch (Exception e) {
            log.error("Erro geral ao processar campanha {}: {}", campaignId, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Envia uma única mensagem para um contato (usado pelo sistema de filas)
     */
    @Transactional
    public boolean sendSingleMessage(CampaignContact campaignContact) {
        return sendMessageToContact(campaignContact);
    }

    private boolean sendMessageToContact(CampaignContact campaignContact) {
        try {
            // Buscar a conversa associada ao contato
            var conversation = conversationService.findByCustomerIdAndCampaignId(
                campaignContact.getCustomer().getId(), 
                campaignContact.getCampaign().getId());
            
            if (conversation.isEmpty()) {
                log.warn("Conversa não encontrada para contato {} da campanha {}", 
                    campaignContact.getCustomer().getName(), campaignContact.getCampaign().getId());
                return false;
            }

            // Buscar mensagens DRAFT na conversa
            var draftMessages = messageService.findByConversationAndStatus(
                conversation.get().getId(), MessageStatus.DRAFT);
            
            if (draftMessages.isEmpty()) {
                log.warn("Nenhuma mensagem DRAFT encontrada para contato {} da campanha {}", 
                    campaignContact.getCustomer().getName(), campaignContact.getCampaign().getId());
                return false;
            }

            // Pegar a primeira mensagem DRAFT e enviar
            var draftMessage = draftMessages.get(0);
            
            // Atualizar status da mensagem para SENT
            UpdateMessageDTO updateDTO = UpdateMessageDTO.builder()
                .status(MessageStatus.SENT)
                .build();
            
            messageService.update(draftMessage.getId(), updateDTO);
            
            // Enviar via MessagingService (Z-API ou outro provedor)
            String phoneNumber = campaignContact.getCustomer().getPhone();
            String messageContent = draftMessage.getContent();
            
            boolean sent = messagingService.sendMessage(phoneNumber, messageContent);
            
            if (sent) {
                log.debug("Mensagem enviada via MessagingService para {}: {}", phoneNumber, messageContent);
                return true;
            } else {
                log.warn("Falha no envio via MessagingService para {}", phoneNumber);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para contato {}: {}", 
                campaignContact.getCustomer().getName(), e.getMessage());
            return false;
        }
    }

    @Transactional
    public void pauseCampaignMessaging(UUID campaignId) {
        log.info("Pausando envio de mensagens da campanha: {}", campaignId);
        
        Campaign campaign = campaignService.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campanha não encontrada: " + campaignId));
        
        campaignService.pauseCampaign(campaignId);
        
        log.info("Campanha {} pausada com sucesso", campaignId);
    }

    @Transactional
    public void resumeCampaignMessaging(UUID campaignId) {
        log.info("Retomando envio de mensagens da campanha: {}", campaignId);
        
        Campaign campaign = campaignService.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campanha não encontrada: " + campaignId));
        
        campaignService.resumeCampaign(campaignId);
        
        // Iniciar envio automático novamente
        startCampaignMessaging(campaignId);
        
        log.info("Campanha {} retomada com sucesso", campaignId);
    }

    @Transactional(readOnly = true)
    public void getCampaignMessagingStats(UUID campaignId) {
        var stats = campaignService.getCampaignStatistics(campaignId);
        log.info("Estatísticas da campanha {}: {}", campaignId, stats);
    }

    /**
     * Calcula delay randomizado entre mensagens baseado nas boas práticas WHAPI
     * Por padrão usa modo conservador (30-60s) para segurança máxima
     */
    private int getRandomDelay() {
        return getRandomDelay(true);
    }

    /**
     * Calcula delay randomizado entre mensagens
     * @param conservative true para números novos (30-60s), false para experientes (5-10s)
     */
    private int getRandomDelay(boolean conservative) {
        int minDelay, maxDelay;
        
        if (conservative) {
            minDelay = CONSERVATIVE_MIN_DELAY; // 30s
            maxDelay = CONSERVATIVE_MAX_DELAY; // 60s
        } else {
            minDelay = EXPERIENCED_MIN_DELAY; // 5s  
            maxDelay = EXPERIENCED_MAX_DELAY; // 10s
        }
        
        return minDelay + (int)(Math.random() * (maxDelay - minDelay));
    }

    /**
     * Verifica se pode continuar enviando baseado no limite de horas diárias
     * TODO: Implementar controle de horas diárias por campanha
     */
    private boolean canContinueSending(UUID campaignId) {
        // Por enquanto sempre permite, mas pode ser estendido para controlar
        // as 6 horas máximas de envio por dia recomendadas pela WHAPI
        return true;
    }
}