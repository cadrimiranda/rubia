package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private final CampaignMessagingProperties properties;

    // Chaves Redis com namespace seguro
    private static final String QUEUE_KEY = "rubia:campaign:queue";
    private static final String STATE_KEY_PREFIX = "rubia:campaign:state:";
    private static final String PROCESSING_LOCK_KEY = "rubia:campaign:processing:lock";
    

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

            // Enviar mensagem de forma assíncrona
            CompletableFuture<Boolean> sendFuture = campaignMessagingService.sendSingleMessageAsync(contact);
            
            // Para manter compatibilidade, aguarda com timeout
            try {
                boolean success = sendFuture.get(properties.getMessageTimeout().toSeconds(), TimeUnit.SECONDS);
                
                // Atualizar status
                updateContactStatus(contact, success);
                
                // Atualizar estado no Redis
                updateCampaignStateInRedis(item.getCampaignId(), success);
                
                log.debug("✅ Mensagem processada: campanha={}, contato={}, sucesso={}", 
                        item.getCampaignId(), item.getCampaignContactId(), success);
                
                return true;
                
            } catch (TimeoutException e) {
                log.warn("⏰ Timeout no envio da mensagem para contato {}: {}", 
                        contact.getId(), e.getMessage());
                
                // Atualizar status como falha por timeout
                updateContactStatus(contact, false);
                updateCampaignStateInRedis(item.getCampaignId(), false);
                
                return false;
            }
            
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
            // Randomizar ordem dos contatos se configurado
            List<CampaignContact> processedContacts = new ArrayList<>(contacts);
            if (properties.isRandomizeOrder()) {
                Collections.shuffle(processedContacts);
                log.debug("🔀 Ordem dos {} contatos randomizada para campanha {}", 
                        processedContacts.size(), campaignId);
            }
            
            LocalDateTime currentTime = LocalDateTime.now();
            int currentBatch = 1;
            
            for (int i = 0; i < processedContacts.size(); i++) {
                CampaignContact contact = processedContacts.get(i);
                
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
                if ((i + 1) % properties.getBatchSize() == 0) {
                    currentBatch++;
                    currentTime = scheduledTime.plusMinutes(properties.getBatchPauseMinutes());
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
        int indexInBatch = messageIndex % properties.getBatchSize();
        LocalDateTime batchStartTime = baseTime.plusMinutes((long) (batchNumber - 1) * properties.getBatchPauseMinutes());
        
        int totalDelaySeconds = 0;
        for (int i = 0; i < indexInBatch; i++) {
            int randomDelay = properties.getMinDelayMs() + 
                (int)(Math.random() * (properties.getMaxDelayMs() - properties.getMinDelayMs()));
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

    /**
     * Pausa uma campanha ativa
     */
    @Transactional
    public void pauseCampaign(UUID campaignId, String companyId, String userId) {
        log.info("🔒 Pausando campanha {} (empresa={}, usuário={})", campaignId, companyId, userId);
        
        try {
            // Validar permissões
            Campaign campaign = campaignService.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Campanha não encontrada: " + campaignId));
            
            if (!campaign.getCompany().getId().toString().equals(companyId)) {
                throw new RuntimeException("Acesso negado: campanha não pertence à empresa");
            }
            
            // Verificar se campanha está ativa
            if (campaign.getStatus() != CampaignStatus.ACTIVE) {
                throw new RuntimeException("Campanha não está ativa: " + campaign.getStatus());
            }
            
            // Atualizar status da campanha no banco
            campaign.setStatus(CampaignStatus.PAUSED);
            // Note: Implementar método save no CampaignService se necessário
            
            // Marcar estado como pausado no Redis (implementação simplificada)
            String stateKey = STATE_KEY_PREFIX + campaignId;
            redisTemplate.opsForValue().set(stateKey + ":paused", "true", 24, TimeUnit.HOURS);
            
            log.info("✅ Campanha {} pausada com sucesso", campaignId);
            
        } catch (Exception e) {
            log.error("❌ Erro ao pausar campanha {}: {}", campaignId, e.getMessage(), e);
            throw new RuntimeException("Erro ao pausar campanha: " + e.getMessage(), e);
        }
    }

    /**
     * Retoma uma campanha pausada
     */
    @Transactional
    public void resumeCampaign(UUID campaignId, String companyId, String userId) {
        log.info("🔒 Retomando campanha {} (empresa={}, usuário={})", campaignId, companyId, userId);
        
        try {
            // Validar permissões
            Campaign campaign = campaignService.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Campanha não encontrada: " + campaignId));
            
            if (!campaign.getCompany().getId().toString().equals(companyId)) {
                throw new RuntimeException("Acesso negado: campanha não pertence à empresa");
            }
            
            // Verificar se campanha está pausada
            if (campaign.getStatus() != CampaignStatus.PAUSED) {
                throw new RuntimeException("Campanha não está pausada: " + campaign.getStatus());
            }
            
            // Atualizar status da campanha no banco
            campaign.setStatus(CampaignStatus.ACTIVE);
            // Note: Implementar método save no CampaignService se necessário
            
            // Remover estado pausado do Redis
            String stateKey = STATE_KEY_PREFIX + campaignId;
            redisTemplate.delete(stateKey + ":paused");
            
            log.info("✅ Campanha {} retomada com sucesso", campaignId);
            
        } catch (Exception e) {
            log.error("❌ Erro ao retomar campanha {}: {}", campaignId, e.getMessage(), e);
            throw new RuntimeException("Erro ao retomar campanha: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém estatísticas detalhadas de uma campanha
     */
    public Map<String, Object> getCampaignStats(UUID campaignId, String companyId) {
        log.debug("📊 Obtendo estatísticas da campanha {} (empresa={})", campaignId, companyId);
        
        try {
            // Validar permissões
            Campaign campaign = campaignService.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Campanha não encontrada: " + campaignId));
            
            if (!campaign.getCompany().getId().toString().equals(companyId)) {
                throw new RuntimeException("Acesso negado: campanha não pertence à empresa");
            }
            
            // Estado simplificado - verificar se está pausada
            boolean isPaused = Boolean.TRUE.equals(redisTemplate.hasKey(STATE_KEY_PREFIX + campaignId + ":paused"));
            
            // Contar contatos por status
            List<CampaignContact> allContacts = campaignContactService.findByCampaignId(campaignId);
            long totalContacts = allContacts.size();
            long pendingContacts = allContacts.stream()
                    .filter(c -> c.getStatus() == CampaignContactStatus.PENDING)
                    .count();
            long sentContacts = allContacts.stream()
                    .filter(c -> c.getStatus() == CampaignContactStatus.SENT)
                    .count();
            long failedContacts = allContacts.stream()
                    .filter(c -> c.getStatus() == CampaignContactStatus.FAILED)
                    .count();
            
            // Contar itens na fila Redis
            long queuedItems = redisTemplate.opsForZSet().count(QUEUE_KEY, 0, System.currentTimeMillis());
            
            // Calcular progresso
            double progressPercentage = totalContacts > 0 ? 
                    ((double) (sentContacts + failedContacts) / totalContacts) * 100 : 0;
            
            // Estimar tempo restante (baseado no histórico)
            String estimatedCompletion = calculateEstimatedCompletion(pendingContacts);
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("campaignId", campaignId);
            stats.put("status", campaign.getStatus().toString());
            stats.put("totalContacts", totalContacts);
            stats.put("processedContacts", sentContacts + failedContacts);
            stats.put("pendingContacts", pendingContacts);
            stats.put("sentContacts", sentContacts);
            stats.put("failedContacts", failedContacts);
            stats.put("queuedItems", queuedItems);
            stats.put("progressPercentage", Math.round(progressPercentage * 100.0) / 100.0);
            stats.put("estimatedCompletion", estimatedCompletion);
            stats.put("isPaused", isPaused);
            stats.put("createdAt", campaign.getCreatedAt());
            
            log.debug("📊 Estatísticas da campanha {}: {}", campaignId, stats);
            return stats;
            
        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas da campanha {}: {}", campaignId, e.getMessage(), e);
            throw new RuntimeException("Erro ao obter estatísticas: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém estatísticas globais do sistema de filas
     */
    public Map<String, Object> getGlobalQueueStats() {
        log.debug("📊 Obtendo estatísticas globais do sistema");
        
        try {
            // Contar total de itens na fila
            long totalQueuedItems = redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE);
            long readyToProcessItems = redisTemplate.opsForZSet().count(QUEUE_KEY, 0, System.currentTimeMillis());
            
            // Contar campanhas pausadas (implementação simplificada)
            Set<String> pausedKeys = redisTemplate.keys(STATE_KEY_PREFIX + "*:paused");
            long pausedCampaigns = pausedKeys != null ? pausedKeys.size() : 0;
            
            // Verificar se há processamento ativo
            Boolean hasProcessingLock = redisTemplate.hasKey(PROCESSING_LOCK_KEY);
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalQueuedItems", totalQueuedItems);
            stats.put("readyToProcessItems", readyToProcessItems);
            stats.put("scheduledItems", totalQueuedItems - readyToProcessItems);
            stats.put("pausedCampaigns", pausedCampaigns);
            stats.put("isProcessing", hasProcessingLock != null && hasProcessingLock);
            stats.put("batchSize", properties.getBatchSize());
            stats.put("batchPauseMinutes", properties.getBatchPauseMinutes());
            stats.put("timestamp", LocalDateTime.now());
            
            log.debug("📊 Estatísticas globais: {}", stats);
            return stats;
            
        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas globais: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao obter estatísticas globais: " + e.getMessage(), e);
        }
    }

    /**
     * Calcula estimativa de conclusão baseada na configuração
     */
    private String calculateEstimatedCompletion(long remainingContacts) {
        if (remainingContacts == 0) {
            return "Concluída";
        }
        
        // Estimativa baseada na configuração padrão
        long batchSize = properties.getBatchSize();
        long batchPauseMinutes = properties.getBatchPauseMinutes();
        long remainingBatches = (remainingContacts + batchSize - 1) / batchSize; // Round up
        long estimatedMinutes = remainingBatches * batchPauseMinutes;
        
        if (estimatedMinutes < 60) {
            return estimatedMinutes + " minutos";
        } else if (estimatedMinutes < 1440) {
            return (estimatedMinutes / 60) + " horas";
        } else {
            return (estimatedMinutes / 1440) + " dias";
        }
    }
}