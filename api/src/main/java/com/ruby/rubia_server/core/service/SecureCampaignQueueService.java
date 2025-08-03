package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.dto.UpdateCampaignContactDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Versão segura do serviço de filas usando Redis
 * Habilitado apenas quando Redis estiver configurado
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "campaign.queue.provider", havingValue = "redis")
public class SecureCampaignQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CampaignService campaignService;
    private final CampaignContactService campaignContactService;
    private final CampaignMessagingService campaignMessagingService;

    // Chaves Redis com namespace seguro
    private static final String QUEUE_KEY = "rubia:campaign:queue";
    private static final String STATE_KEY_PREFIX = "rubia:campaign:state:";
    private static final String PROCESSING_LOCK_KEY = "rubia:campaign:processing:lock";
    
    // Configurações WHAPI
    private static final int BATCH_SIZE = 20;
    private static final int BATCH_PAUSE_MINUTES = 60;
    private static final int CONSERVATIVE_MIN_DELAY = 30000;
    private static final int CONSERVATIVE_MAX_DELAY = 60000;

    /**
     * Item da fila seguro
     */
    public static class SecureCampaignQueueItem {
        private UUID campaignId;
        private UUID campaignContactId;
        private LocalDateTime scheduledTime;
        private int batchNumber;
        private String companyId; // Segurança adicional
        private String createdBy; // Auditoria

        // Constructors, getters e setters
        public SecureCampaignQueueItem() {}

        public SecureCampaignQueueItem(UUID campaignId, UUID campaignContactId, 
                                     LocalDateTime scheduledTime, int batchNumber,
                                     String companyId, String createdBy) {
            this.campaignId = campaignId;
            this.campaignContactId = campaignContactId;
            this.scheduledTime = scheduledTime;
            this.batchNumber = batchNumber;
            this.companyId = companyId;
            this.createdBy = createdBy;
        }

        // Getters e Setters
        public UUID getCampaignId() { return campaignId; }
        public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }
        public UUID getCampaignContactId() { return campaignContactId; }
        public void setCampaignContactId(UUID campaignContactId) { this.campaignContactId = campaignContactId; }
        public LocalDateTime getScheduledTime() { return scheduledTime; }
        public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
        public int getBatchNumber() { return batchNumber; }
        public void setBatchNumber(int batchNumber) { this.batchNumber = batchNumber; }
        public String getCompanyId() { return companyId; }
        public void setCompanyId(String companyId) { this.companyId = companyId; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    }

    /**
     * Adiciona campanha à fila Redis com validação de segurança
     */
    @Transactional
    public void enqueueCampaign(UUID campaignId, String companyId, String userId) {
        log.info("🔒 Adicionando campanha {} à fila Redis (empresa={}, usuário={})", 
                campaignId, companyId, userId);
        
        try {
            // Validar permissões
            if (!validateCampaignAccess(campaignId, companyId)) {
                throw new SecurityException("Acesso negado à campanha " + campaignId);
            }

            // Verificar se já existe na fila
            if (isAlreadyInQueue(campaignId)) {
                log.warn("Campanha {} já está na fila", campaignId);
                return;
            }

            // Buscar campanha e contatos
            Campaign campaign = campaignService.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campanha não encontrada: " + campaignId));
            
            List<CampaignContact> pendingContacts = campaignContactService
                .findByCampaignIdAndStatus(campaignId, CampaignContactStatus.PENDING);
            
            if (pendingContacts.isEmpty()) {
                log.info("Nenhum contato pendente para campanha {}", campaignId);
                return;
            }

            // Criar estado da campanha no Redis
            CampaignState state = new CampaignState();
            state.setCampaignId(campaignId);
            state.setCompanyId(companyId);
            state.setCreatedBy(userId);
            state.setStatus(CampaignStatus.ACTIVE);
            state.setTotalContacts(pendingContacts.size());
            state.setCreatedAt(LocalDateTime.now());
            
            // Salvar estado no Redis com TTL
            String stateKey = STATE_KEY_PREFIX + campaignId;
            redisTemplate.opsForValue().set(stateKey, state, 7, TimeUnit.DAYS);

            // Agendar mensagens na fila Redis
            scheduleMessagesInRedis(campaignId, pendingContacts, companyId, userId);
            
            log.info("✅ Campanha {} adicionada à fila Redis com {} contatos", 
                    campaignId, pendingContacts.size());
            
        } catch (Exception e) {
            log.error("❌ Erro ao adicionar campanha {} à fila Redis: {}", campaignId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Processador principal com lock distribuído
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processMessageQueue() {
        // Tentar adquirir lock distribuído
        String lockValue = UUID.randomUUID().toString();
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(PROCESSING_LOCK_KEY, lockValue, 25, TimeUnit.SECONDS);
        
        if (!lockAcquired) {
            log.debug("Outra instância está processando a fila");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Buscar itens prontos para processamento
            Set<Object> queueItems = redisTemplate.opsForZSet()
                .rangeByScore(QUEUE_KEY, 0, now.atZone(java.time.ZoneOffset.UTC).toEpochSecond());
            
            if (queueItems.isEmpty()) {
                return;
            }

            log.debug("🔄 Processando {} itens da fila Redis", queueItems.size());
            
            int processedCount = 0;
            for (Object itemObj : queueItems) {
                try {
                    SecureCampaignQueueItem item = objectMapper.readValue(
                        itemObj.toString(), SecureCampaignQueueItem.class);
                    
                    if (processSecureQueueItem(item)) {
                        // Remove da fila após processamento bem-sucedido
                        redisTemplate.opsForZSet().remove(QUEUE_KEY, itemObj);
                        processedCount++;
                    }
                    
                    // Limitar processamento por ciclo
                    if (processedCount >= 10) {
                        break;
                    }
                    
                } catch (Exception e) {
                    log.error("Erro ao processar item da fila: {}", e.getMessage());
                    // Remove item com erro para não ficar travado
                    redisTemplate.opsForZSet().remove(QUEUE_KEY, itemObj);
                }
            }
            
            if (processedCount > 0) {
                log.info("📤 Processados {} itens da fila Redis", processedCount);
            }
            
        } finally {
            // Liberar lock apenas se foi esta instância que adquiriu
            String currentLockValue = (String) redisTemplate.opsForValue().get(PROCESSING_LOCK_KEY);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(PROCESSING_LOCK_KEY);
            }
        }
    }

    /**
     * Processa item com validações de segurança
     */
    private boolean processSecureQueueItem(SecureCampaignQueueItem item) {
        try {
            // Validar permissões novamente
            if (!validateCampaignAccess(item.getCampaignId(), item.getCompanyId())) {
                log.warn("🚨 Tentativa de processamento não autorizado: campanha={}, empresa={}", 
                        item.getCampaignId(), item.getCompanyId());
                return false;
            }

            // Verificar se campanha ainda está ativa
            String stateKey = STATE_KEY_PREFIX + item.getCampaignId();
            CampaignState state = (CampaignState) redisTemplate.opsForValue().get(stateKey);
            
            if (state == null || state.getStatus() != CampaignStatus.ACTIVE) {
                log.debug("Campanha {} não está mais ativa", item.getCampaignId());
                return false;
            }

            // Buscar e processar contato
            Optional<CampaignContact> optionalContact = campaignContactService
                .findById(item.getCampaignContactId());
            
            if (optionalContact.isEmpty()) {
                log.warn("CampaignContact {} não encontrado", item.getCampaignContactId());
                return false;
            }

            CampaignContact contact = optionalContact.get();
            
            // Verificar se ainda está pendente
            if (contact.getStatus() != CampaignContactStatus.PENDING) {
                log.debug("CampaignContact {} não está mais pendente", contact.getId());
                return false;
            }

            // Enviar mensagem
            boolean success = campaignMessagingService.sendSingleMessage(contact);
            
            // Atualizar status
            updateContactStatus(contact, success);
            
            // Atualizar estado no Redis
            updateCampaignStateInRedis(item.getCampaignId(), success);
            
            log.debug("✅ Mensagem processada: campanha={}, contato={}, sucesso={}", 
                    item.getCampaignId(), item.getCampaignContactId(), success);
            
            return true;
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar item seguro: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Valida se usuário/empresa tem acesso à campanha
     */
    private boolean validateCampaignAccess(UUID campaignId, String companyId) {
        try {
            Optional<Campaign> optionalCampaign = campaignService.findById(campaignId);
            if (optionalCampaign.isEmpty()) {
                return false;
            }
            
            Campaign campaign = optionalCampaign.get();
            return campaign.getCompany().getId().toString().equals(companyId);
            
        } catch (Exception e) {
            log.error("Erro na validação de acesso: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se campanha já está na fila
     */
    private boolean isAlreadyInQueue(UUID campaignId) {
        String stateKey = STATE_KEY_PREFIX + campaignId;
        return redisTemplate.hasKey(stateKey);
    }

    /**
     * Agenda mensagens no Redis com score baseado no tempo
     */
    private void scheduleMessagesInRedis(UUID campaignId, List<CampaignContact> contacts, 
                                       String companyId, String userId) {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            int currentBatch = 1;
            
            for (int i = 0; i < contacts.size(); i++) {
                CampaignContact contact = contacts.get(i);
                
                // Calcular tempo agendado
                LocalDateTime scheduledTime = calculateScheduledTime(currentTime, i, currentBatch);
                
                // Criar item da fila
                SecureCampaignQueueItem item = new SecureCampaignQueueItem(
                    campaignId, contact.getId(), scheduledTime, currentBatch, companyId, userId);
                
                // Adicionar ao Redis Sorted Set (score = timestamp)
                long score = scheduledTime.atZone(java.time.ZoneOffset.UTC).toEpochSecond();
                String itemJson = objectMapper.writeValueAsString(item);
                
                redisTemplate.opsForZSet().add(QUEUE_KEY, itemJson, score);
                
                // Verificar mudança de lote
                if ((i + 1) % BATCH_SIZE == 0) {
                    currentBatch++;
                    currentTime = scheduledTime.plusMinutes(BATCH_PAUSE_MINUTES);
                }
            }
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar itens da fila", e);
        }
    }

    /**
     * Calcula tempo agendado (mesmo algoritmo do serviço original)
     */
    private LocalDateTime calculateScheduledTime(LocalDateTime baseTime, int messageIndex, int batchNumber) {
        int indexInBatch = messageIndex % BATCH_SIZE;
        LocalDateTime batchStartTime = baseTime.plusMinutes((long) (batchNumber - 1) * BATCH_PAUSE_MINUTES);
        
        int totalDelaySeconds = 0;
        for (int i = 0; i < indexInBatch; i++) {
            int randomDelay = CONSERVATIVE_MIN_DELAY + 
                (int)(Math.random() * (CONSERVATIVE_MAX_DELAY - CONSERVATIVE_MIN_DELAY));
            totalDelaySeconds += randomDelay / 1000;
        }
        
        return batchStartTime.plusSeconds(totalDelaySeconds);
    }

    /**
     * Atualiza status do contato
     */
    private void updateContactStatus(CampaignContact contact, boolean success) {
        try {
            UpdateCampaignContactDTO updateDTO = UpdateCampaignContactDTO.builder()
                .status(success ? CampaignContactStatus.SENT : CampaignContactStatus.FAILED)
                .messageSentAt(success ? LocalDateTime.now() : null)
                .build();
            
            campaignContactService.update(contact.getId(), updateDTO);
            
        } catch (Exception e) {
            log.error("Erro ao atualizar status do contato {}: {}", contact.getId(), e.getMessage());
        }
    }

    /**
     * Atualiza estado da campanha no Redis
     */
    private void updateCampaignStateInRedis(UUID campaignId, boolean success) {
        try {
            String stateKey = STATE_KEY_PREFIX + campaignId;
            CampaignState state = (CampaignState) redisTemplate.opsForValue().get(stateKey);
            
            if (state != null) {
                state.setProcessedContacts(state.getProcessedContacts() + 1);
                state.setLastProcessedTime(LocalDateTime.now());
                
                if (state.getProcessedContacts() >= state.getTotalContacts()) {
                    state.setStatus(CampaignStatus.COMPLETED);
                    log.info("🎉 Campanha {} concluída no Redis", campaignId);
                }
                
                redisTemplate.opsForValue().set(stateKey, state, 7, TimeUnit.DAYS);
            }
            
        } catch (Exception e) {
            log.error("Erro ao atualizar estado da campanha no Redis: {}", e.getMessage());
        }
    }

    /**
     * Estado da campanha para Redis
     */
    public static class CampaignState {
        private UUID campaignId;
        private String companyId;
        private String createdBy;
        private CampaignStatus status;
        private int totalContacts;
        private int processedContacts;
        private LocalDateTime createdAt;
        private LocalDateTime lastProcessedTime;

        // Getters e Setters
        public UUID getCampaignId() { return campaignId; }
        public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }
        public String getCompanyId() { return companyId; }
        public void setCompanyId(String companyId) { this.companyId = companyId; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        public CampaignStatus getStatus() { return status; }
        public void setStatus(CampaignStatus status) { this.status = status; }
        public int getTotalContacts() { return totalContacts; }
        public void setTotalContacts(int totalContacts) { this.totalContacts = totalContacts; }
        public int getProcessedContacts() { return processedContacts; }
        public void setProcessedContacts(int processedContacts) { this.processedContacts = processedContacts; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastProcessedTime() { return lastProcessedTime; }
        public void setLastProcessedTime(LocalDateTime lastProcessedTime) { this.lastProcessedTime = lastProcessedTime; }
    }
}