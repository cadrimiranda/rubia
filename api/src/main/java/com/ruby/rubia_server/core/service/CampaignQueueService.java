package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.dto.UpdateCampaignContactDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Serviço de fila para processamento de mensagens de campanha
 * Implementação simples usando Java Collections para evitar dependência de Redis/RabbitMQ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignQueueService {

    private final CampaignService campaignService;
    private final CampaignContactService campaignContactService;
    private final CampaignMessagingService campaignMessagingService;

    // Fila principal de contatos para envio
    private final Queue<CampaignQueueItem> messageQueue = new LinkedBlockingQueue<>();
    
    // Controle de estado das campanhas
    private final Map<UUID, CampaignState> campaignStates = new ConcurrentHashMap<>();
    
    // Configurações baseadas nas boas práticas WHAPI
    private static final int BATCH_SIZE = 20;
    private static final int BATCH_PAUSE_MINUTES = 60;
    private static final int CONSERVATIVE_MIN_DELAY = 30000; // 30s
    private static final int CONSERVATIVE_MAX_DELAY = 60000; // 60s

    /**
     * Item da fila de mensagens
     */
    public static class CampaignQueueItem {
        private final UUID campaignId;
        private final UUID campaignContactId;
        private final LocalDateTime scheduledTime;
        private final int batchNumber;

        public CampaignQueueItem(UUID campaignId, UUID campaignContactId, LocalDateTime scheduledTime, int batchNumber) {
            this.campaignId = campaignId;
            this.campaignContactId = campaignContactId;
            this.scheduledTime = scheduledTime;
            this.batchNumber = batchNumber;
        }

        public UUID getCampaignId() { return campaignId; }
        public UUID getCampaignContactId() { return campaignContactId; }
        public LocalDateTime getScheduledTime() { return scheduledTime; }
        public int getBatchNumber() { return batchNumber; }
    }

    /**
     * Estado de uma campanha em processamento
     */
    public static class CampaignState {
        private UUID campaignId;
        private CampaignStatus status;
        private int totalContacts;
        private int processedContacts;
        private int currentBatch;
        private LocalDateTime lastProcessedTime;
        private LocalDateTime nextBatchTime;

        public CampaignState(UUID campaignId) {
            this.campaignId = campaignId;
            this.status = CampaignStatus.ACTIVE;
            this.totalContacts = 0;
            this.processedContacts = 0;
            this.currentBatch = 1;
            this.lastProcessedTime = LocalDateTime.now();
        }

        // Getters e Setters
        public UUID getCampaignId() { return campaignId; }
        public CampaignStatus getStatus() { return status; }
        public void setStatus(CampaignStatus status) { this.status = status; }
        public int getTotalContacts() { return totalContacts; }
        public void setTotalContacts(int totalContacts) { this.totalContacts = totalContacts; }
        public int getProcessedContacts() { return processedContacts; }
        public void setProcessedContacts(int processedContacts) { this.processedContacts = processedContacts; }
        public int getCurrentBatch() { return currentBatch; }
        public void setCurrentBatch(int currentBatch) { this.currentBatch = currentBatch; }
        public LocalDateTime getLastProcessedTime() { return lastProcessedTime; }
        public void setLastProcessedTime(LocalDateTime lastProcessedTime) { this.lastProcessedTime = lastProcessedTime; }
        public LocalDateTime getNextBatchTime() { return nextBatchTime; }
        public void setNextBatchTime(LocalDateTime nextBatchTime) { this.nextBatchTime = nextBatchTime; }
    }

    /**
     * Adiciona campanha à fila de processamento
     */
    @Transactional
    public void enqueueCampaign(UUID campaignId) {
        log.info("Adicionando campanha {} à fila de processamento", campaignId);
        
        try {
            // Verificar se campanha existe e está ativa
            Campaign campaign = campaignService.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campanha não encontrada: " + campaignId));
            
            if (campaign.getStatus() != CampaignStatus.ACTIVE) {
                log.warn("Campanha {} não está ativa. Status: {}", campaignId, campaign.getStatus());
                return;
            }

            // Buscar contatos pendentes
            List<CampaignContact> pendingContacts = campaignContactService
                .findByCampaignIdAndStatus(campaignId, CampaignContactStatus.PENDING);
            
            if (pendingContacts.isEmpty()) {
                log.info("Nenhum contato pendente para campanha {}", campaignId);
                return;
            }

            // Criar estado da campanha
            CampaignState state = new CampaignState(campaignId);
            state.setTotalContacts(pendingContacts.size());
            campaignStates.put(campaignId, state);

            // Agendar mensagens em lotes
            int totalBatches = (int) Math.ceil((double) pendingContacts.size() / BATCH_SIZE);
            scheduleMessagesInBatches(campaignId, pendingContacts);
            
            // Estimar tempo total
            LocalDateTime estimatedCompletion = calculateEstimatedCompletion(pendingContacts.size());
            
            log.info("📋 Campanha {} adicionada à fila:", campaignId);
            log.info("   • {} contatos em {} lotes", pendingContacts.size(), totalBatches);
            log.info("   • Primeira mensagem: {}", LocalDateTime.now().plusMinutes(1));
            log.info("   • Conclusão estimada: {}", estimatedCompletion);
            log.info("   • Duração estimada: ~{} horas", 
                    java.time.Duration.between(LocalDateTime.now(), estimatedCompletion).toHours());
            
        } catch (Exception e) {
            log.error("Erro ao adicionar campanha {} à fila: {}", campaignId, e.getMessage(), e);
        }
    }

    /**
     * Agenda mensagens em lotes respeitando as boas práticas WHAPI
     */
    private void scheduleMessagesInBatches(UUID campaignId, List<CampaignContact> contacts) {
        LocalDateTime currentTime = LocalDateTime.now();
        int currentBatch = 1;
        
        for (int i = 0; i < contacts.size(); i++) {
            CampaignContact contact = contacts.get(i);
            
            // Calcular tempo agendado para esta mensagem
            LocalDateTime scheduledTime = calculateScheduledTime(currentTime, i, currentBatch);
            
            // Criar item da fila
            CampaignQueueItem item = new CampaignQueueItem(
                campaignId, 
                contact.getId(), 
                scheduledTime,
                currentBatch
            );
            
            messageQueue.offer(item);
            
            // Verificar se precisa mudar de lote
            if ((i + 1) % BATCH_SIZE == 0) {
                currentBatch++;
                // Próximo lote começa após pausa de 1 hora
                currentTime = scheduledTime.plusMinutes(BATCH_PAUSE_MINUTES);
            }
        }
    }

    /**
     * Calcula o tempo agendado para uma mensagem baseado nas boas práticas WHAPI
     */
    private LocalDateTime calculateScheduledTime(LocalDateTime baseTime, int messageIndex, int batchNumber) {
        // Dentro do lote atual
        int indexInBatch = messageIndex % BATCH_SIZE;
        
        // Tempo base: início do lote
        LocalDateTime batchStartTime = baseTime.plusMinutes((long) (batchNumber - 1) * BATCH_PAUSE_MINUTES);
        
        // Adicionar delay randomizado para cada mensagem no lote
        int totalDelaySeconds = 0;
        for (int i = 0; i < indexInBatch; i++) {
            int randomDelay = CONSERVATIVE_MIN_DELAY + 
                (int)(Math.random() * (CONSERVATIVE_MAX_DELAY - CONSERVATIVE_MIN_DELAY));
            totalDelaySeconds += randomDelay / 1000;
        }
        
        return batchStartTime.plusSeconds(totalDelaySeconds);
    }

    /**
     * Calcula o tempo estimado de conclusão de uma campanha
     */
    private LocalDateTime calculateEstimatedCompletion(int totalContacts) {
        int totalBatches = (int) Math.ceil((double) totalContacts / BATCH_SIZE);
        
        // Tempo médio por mensagem (45s = média entre 30-60s)
        int avgDelayPerMessage = (CONSERVATIVE_MIN_DELAY + CONSERVATIVE_MAX_DELAY) / 2 / 1000;
        
        // Tempo por lote completo
        int timePerBatch = BATCH_SIZE * avgDelayPerMessage;
        
        // Tempo total = tempo de todos os lotes + pausas entre lotes
        int totalBatchTime = totalBatches * timePerBatch;
        int totalPauseTime = (totalBatches - 1) * BATCH_PAUSE_MINUTES * 60;
        
        return LocalDateTime.now().plusSeconds(totalBatchTime + totalPauseTime);
    }

    /**
     * Processador principal executado a cada 30 segundos
     */
    @Scheduled(fixedDelay = 30000) // A cada 30 segundos
    @Transactional
    public void processMessageQueue() {
        LocalDateTime now = LocalDateTime.now();
        
        if (messageQueue.isEmpty() && campaignStates.isEmpty()) {
            return; // Nada para processar
        }
        
        log.debug("🔄 Processando fila de mensagens - {} itens, {} campanhas ativas", 
                messageQueue.size(), campaignStates.size());
        
        int processedCount = 0;
        
        // Processar mensagens cujo tempo chegou
        CampaignQueueItem item;
        while ((item = getNextReadyItem(now)) != null) {
            processQueueItem(item);
            processedCount++;
            
            // Limitar processamento por ciclo para não sobrecarregar
            if (processedCount >= 10) {
                log.debug("Limite de 10 mensagens por ciclo atingido, pausando até próximo ciclo");
                break;
            }
        }
        
        if (processedCount > 0) {
            log.info("📤 Processadas {} mensagens da fila", processedCount);
        }
        
        // Limpar campanhas finalizadas
        cleanupFinishedCampaigns();
    }

    /**
     * Busca próximo item pronto para processamento
     */
    private CampaignQueueItem getNextReadyItem(LocalDateTime now) {
        // Iterar pela fila para encontrar item pronto
        Iterator<CampaignQueueItem> iterator = messageQueue.iterator();
        while (iterator.hasNext()) {
            CampaignQueueItem item = iterator.next();
            
            // Verificar se é hora de processar e se campanha está ativa
            if (item.getScheduledTime().isBefore(now) || item.getScheduledTime().isEqual(now)) {
                CampaignState state = campaignStates.get(item.getCampaignId());
                if (state != null && state.getStatus() == CampaignStatus.ACTIVE) {
                    iterator.remove(); // Remove da fila
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Processa um item individual da fila
     */
    private void processQueueItem(CampaignQueueItem item) {
        try {
            log.debug("Processando item da fila: campanha={}, contato={}, lote={}", 
                    item.getCampaignId(), item.getCampaignContactId(), item.getBatchNumber());
            
            // Buscar o contato
            Optional<CampaignContact> optionalContact = campaignContactService.findById(item.getCampaignContactId());
            if (optionalContact.isEmpty()) {
                log.warn("CampaignContact {} não encontrado", item.getCampaignContactId());
                return;
            }
            
            CampaignContact contact = optionalContact.get();
            
            // Verificar se ainda está pendente
            if (contact.getStatus() != CampaignContactStatus.PENDING) {
                log.debug("CampaignContact {} não está mais pendente. Status: {}", 
                        contact.getId(), contact.getStatus());
                return;
            }
            
            // Enviar mensagem usando o serviço existente
            boolean success = campaignMessagingService.sendSingleMessage(contact);
            
            // Atualizar status do contato baseado no resultado
            updateContactStatus(contact, success);
            
            // Atualizar estado da campanha
            updateCampaignState(item.getCampaignId(), success);
            
        } catch (Exception e) {
            log.error("Erro ao processar item da fila - campanha={}, contato={}, lote={}: {}", 
                    item.getCampaignId(), item.getCampaignContactId(), item.getBatchNumber(), e.getMessage(), e);
            
            // Tentar marcar contato como FAILED mesmo em caso de erro
            try {
                if (optionalContact.isPresent()) {
                    updateContactStatus(optionalContact.get(), false);
                }
            } catch (Exception updateError) {
                log.error("Erro adicional ao marcar contato {} como FAILED: {}", 
                        item.getCampaignContactId(), updateError.getMessage());
            }
            
            updateCampaignState(item.getCampaignId(), false);
        }
    }

    /**
     * Atualiza estado da campanha após processar uma mensagem
     */
    private void updateCampaignState(UUID campaignId, boolean success) {
        CampaignState state = campaignStates.get(campaignId);
        if (state != null) {
            state.setProcessedContacts(state.getProcessedContacts() + 1);
            state.setLastProcessedTime(LocalDateTime.now());
            
            // Verificar se campanha foi concluída
            if (state.getProcessedContacts() >= state.getTotalContacts()) {
                state.setStatus(CampaignStatus.COMPLETED);
                log.info("Campanha {} concluída. Total processado: {}", campaignId, state.getProcessedContacts());
            }
        }
    }

    /**
     * Remove campanhas finalizadas do controle de estado
     */
    private void cleanupFinishedCampaigns() {
        campaignStates.entrySet().removeIf(entry -> {
            CampaignState state = entry.getValue();
            boolean isFinished = state.getStatus() == CampaignStatus.COMPLETED || 
                               state.getStatus() == CampaignStatus.CANCELED;
            
            if (isFinished) {
                log.debug("Removendo campanha finalizada {} do controle de estado", entry.getKey());
            }
            
            return isFinished;
        });
    }

    /**
     * Pausa uma campanha (remove itens da fila)
     */
    @Transactional
    public void pauseCampaign(UUID campaignId) {
        log.info("Pausando campanha {}", campaignId);
        
        CampaignState state = campaignStates.get(campaignId);
        if (state != null) {
            state.setStatus(CampaignStatus.PAUSED);
        }
        
        // Remove itens da campanha da fila
        messageQueue.removeIf(item -> item.getCampaignId().equals(campaignId));
        
        log.info("Campanha {} pausada", campaignId);
    }

    /**
     * Retoma uma campanha pausada
     */
    @Transactional
    public void resumeCampaign(UUID campaignId) {
        log.info("Retomando campanha {}", campaignId);
        
        CampaignState state = campaignStates.get(campaignId);
        if (state != null) {
            state.setStatus(CampaignStatus.ACTIVE);
        }
        
        // Re-adicionar à fila
        enqueueCampaign(campaignId);
        
        log.info("Campanha {} retomada", campaignId);
    }

    /**
     * Obtém estatísticas de uma campanha
     */
    public Map<String, Object> getCampaignQueueStats(UUID campaignId) {
        CampaignState state = campaignStates.get(campaignId);
        Map<String, Object> stats = new HashMap<>();
        
        if (state != null) {
            int remaining = state.getTotalContacts() - state.getProcessedContacts();
            int totalBatches = (int) Math.ceil((double) state.getTotalContacts() / BATCH_SIZE);
            
            stats.put("campaignId", campaignId);
            stats.put("status", state.getStatus());
            stats.put("totalContacts", state.getTotalContacts());
            stats.put("processedContacts", state.getProcessedContacts());
            stats.put("remainingContacts", remaining);
            stats.put("currentBatch", state.getCurrentBatch());
            stats.put("totalBatches", totalBatches);
            stats.put("lastProcessedTime", state.getLastProcessedTime());
            stats.put("nextBatchTime", state.getNextBatchTime());
            
            // Calcular progresso
            if (state.getTotalContacts() > 0) {
                double progress = (double) state.getProcessedContacts() / state.getTotalContacts() * 100;
                stats.put("progressPercentage", Math.round(progress * 100.0) / 100.0);
            }
            
            // Estimar tempo restante
            if (remaining > 0 && state.getStatus() == CampaignStatus.ACTIVE) {
                LocalDateTime estimatedCompletion = calculateEstimatedCompletion(remaining);
                stats.put("estimatedCompletion", estimatedCompletion);
                stats.put("estimatedRemainingHours", 
                    java.time.Duration.between(LocalDateTime.now(), estimatedCompletion).toHours());
            }
            
            // Contar itens na fila para esta campanha
            long queuedItems = messageQueue.stream()
                .filter(item -> item.getCampaignId().equals(campaignId))
                .count();
            stats.put("queuedMessages", queuedItems);
            
        } else {
            // Verificar se campanha existe no sistema mas não está na fila
            try {
                Optional<Campaign> campaign = campaignService.findById(campaignId);
                if (campaign.isPresent()) {
                    stats.put("campaignId", campaignId);
                    stats.put("status", campaign.get().getStatus());
                    stats.put("message", "Campanha não está na fila de processamento");
                    stats.put("inQueue", false);
                } else {
                    stats.put("error", "Campanha não encontrada");
                }
            } catch (Exception e) {
                stats.put("error", "Erro ao buscar campanha: " + e.getMessage());
            }
        }
        
        return stats;
    }

    /**
     * Obtém estatísticas globais da fila
     */
    public Map<String, Object> getGlobalQueueStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("queueSize", messageQueue.size());
        stats.put("activeCampaigns", campaignStates.size());
        stats.put("totalCampaignStates", campaignStates.values().stream()
            .mapToInt(CampaignState::getTotalContacts).sum());
        stats.put("totalProcessedContacts", campaignStates.values().stream()
            .mapToInt(CampaignState::getProcessedContacts).sum());
        
        return stats;
    }

    /**
     * Atualiza o status do contato após tentativa de envio
     */
    private void updateContactStatus(CampaignContact contact, boolean success) {
        try {
            UpdateCampaignContactDTO updateDTO;
            
            if (success) {
                updateDTO = UpdateCampaignContactDTO.builder()
                    .status(CampaignContactStatus.SENT)
                    .messageSentAt(LocalDateTime.now())
                    .build();
                
                log.debug("Marcando contato {} como SENT", contact.getId());
            } else {
                updateDTO = UpdateCampaignContactDTO.builder()
                    .status(CampaignContactStatus.FAILED)
                    .build();
                
                log.debug("Marcando contato {} como FAILED", contact.getId());
            }
            
            Optional<CampaignContact> updated = campaignContactService.update(contact.getId(), updateDTO);
            
            if (updated.isPresent()) {
                log.debug("Status do contato {} atualizado para {}", 
                        contact.getId(), updated.get().getStatus());
            } else {
                log.warn("Falha ao atualizar status do contato {}", contact.getId());
            }
            
        } catch (Exception e) {
            log.error("Erro ao atualizar status do contato {}: {}", 
                    contact.getId(), e.getMessage(), e);
        }
    }
}