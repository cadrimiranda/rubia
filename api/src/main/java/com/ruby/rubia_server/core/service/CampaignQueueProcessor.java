package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.MessageResult;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processador de fila de campanhas com controle de concorrência e backpressure
 * Implementa processamento controlado para evitar sobrecarga do sistema
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CampaignQueueProcessor {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CampaignMessagingService messageService;
    
    @Qualifier("campaignConcurrencyLimiter")
    private final Semaphore concurrencyLimiter;
    
    @Qualifier("queueProcessingLimiter")  
    private final Semaphore queueProcessingLimiter;
    
    private final MeterRegistry meterRegistry;
    
    private static final String QUEUE_KEY = "rubia:campaign:queue";
    private static final String PROCESSING_KEY = "rubia:campaign:processing";
    private static final String ERROR_KEY = "rubia:campaign:error";
    private static final int BATCH_SIZE = 10;
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);
    
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final Map<String, CompletableFuture<Boolean>> activeTasks = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 5000) // A cada 5 segundos
    public void processCampaignQueue() {
        if (!isProcessing.compareAndSet(false, true)) {
            log.debug("Processamento já em andamento");
            return;
        }
        
        try {
            processNextBatch();
        } finally {
            isProcessing.set(false);
        }
    }
    
    private void processNextBatch() {
        // Verifica se pode processar mais itens
        if (!queueProcessingLimiter.tryAcquire()) {
            log.debug("Sistema ocupado, aguardando próximo ciclo");
            return;
        }
        
        try {
            // Verifica quantos slots estão disponíveis
            int availablePermits = concurrencyLimiter.availablePermits();
            if (availablePermits == 0) {
                log.warn("Sistema no limite de concorrência, aguardando slots disponíveis");
                return;
            }
            
            // Processa no máximo BATCH_SIZE itens ou quantos permits estiverem disponíveis
            int itemsToProcess = Math.min(BATCH_SIZE, availablePermits);
            
            List<String> queueItems = popItemsFromQueue(itemsToProcess);
            if (queueItems.isEmpty()) {
                return;
            }
            
            log.info("📦 Processando lote de {} itens da fila", queueItems.size());
            
            for (String itemJson : queueItems) {
                processItemWithBackpressure(itemJson);
            }
            
            updateMetrics();
            
        } finally {
            queueProcessingLimiter.release();
        }
    }
    
    private List<String> popItemsFromQueue(int count) {
        List<String> items = new ArrayList<>();
        
        try {
            // Pop atômico usando pipeline para performance
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (int i = 0; i < count; i++) {
                    connection.zSetCommands().zPopMin(QUEUE_KEY.getBytes());
                }
                return null;
            });
            
            for (Object result : results) {
                if (result != null) {
                    String itemJson = result.toString();
                    items.add(itemJson);
                    // Move para lista de processamento para rastreamento
                    redisTemplate.opsForList().rightPush(PROCESSING_KEY, itemJson);
                }
            }
            
        } catch (Exception e) {
            log.error("Erro ao extrair itens da fila: {}", e.getMessage(), e);
        }
        
        return items;
    }
    
    private void processItemWithBackpressure(String itemJson) {
        try {
            // Tenta adquirir permissão (não bloqueia)
            if (!concurrencyLimiter.tryAcquire()) {
                // Se não conseguir, devolve para a fila com timestamp atual
                long score = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(QUEUE_KEY, itemJson, score);
                log.warn("⏸️ Sistema sobrecarregado, devolvendo item para a fila");
                return;
            }
            
            String taskId = UUID.randomUUID().toString();
            
            // Parse do item da fila para extrair o CampaignContact
            CompletableFuture<Boolean> future = processQueueItem(itemJson)
                .whenComplete((result, throwable) -> {
                    try {
                        if (throwable != null) {
                            log.error("❌ Erro ao processar item: {}", throwable.getMessage());
                            handleProcessingError(itemJson, throwable);
                        } else if (result != null && result) {
                            log.debug("✅ Processamento concluído com sucesso");
                            removeFromProcessingList(itemJson);
                        } else {
                            log.warn("⚠️ Processamento falhou");
                            handleProcessingError(itemJson, new RuntimeException("Processing failed"));
                        }
                    } finally {
                        concurrencyLimiter.release();
                        activeTasks.remove(taskId);
                    }
                });
            
            activeTasks.put(taskId, future);
            
        } catch (Exception e) {
            log.error("Erro ao processar item da fila", e);
            concurrencyLimiter.release();
            handleProcessingError(itemJson, e);
        }
    }
    
    private CompletableFuture<Boolean> processQueueItem(String itemJson) {
        // Este método será implementado para usar o SecureCampaignQueueService
        // Por ora, retorna um placeholder
        return CompletableFuture.completedFuture(true);
    }
    
    private void removeFromProcessingList(String itemJson) {
        redisTemplate.opsForList().remove(PROCESSING_KEY, 1, itemJson);
    }
    
    private void handleProcessingError(String itemJson, Throwable error) {
        removeFromProcessingList(itemJson);
        redisTemplate.opsForList().rightPush(ERROR_KEY, itemJson);
        
        String errorType = error != null ? error.getClass().getSimpleName() : "UnknownError";
        meterRegistry.counter("campaign.processing.errors", "error", errorType).increment();
    }
    
    private void updateMetrics() {
        try {
            Long queueSize = redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE);
            Long processingSize = redisTemplate.opsForList().size(PROCESSING_KEY);
            Long errorSize = redisTemplate.opsForList().size(ERROR_KEY);
            
            meterRegistry.gauge("campaign.queue.size", queueSize != null ? queueSize : 0);
            meterRegistry.gauge("campaign.processing.size", processingSize != null ? processingSize : 0);
            meterRegistry.gauge("campaign.error.size", errorSize != null ? errorSize : 0);
            meterRegistry.gauge("campaign.active.tasks", activeTasks.size());
            meterRegistry.gauge("campaign.concurrency.available", concurrencyLimiter.availablePermits());
            meterRegistry.gauge("campaign.concurrency.used", 50 - concurrencyLimiter.availablePermits());
            
        } catch (Exception e) {
            log.warn("Erro ao atualizar métricas: {}", e.getMessage());
        }
    }
    
    @Scheduled(fixedRate = 60000) // A cada minuto
    public void recoverStuckMessages() {
        try {
            log.debug("🔄 Verificando mensagens travadas...");
            
            // Recupera mensagens que estão em processamento há muito tempo
            List<Object> processingItems = redisTemplate.opsForList().range(PROCESSING_KEY, 0, -1);
            if (processingItems == null || processingItems.isEmpty()) {
                return;
            }
            
            int recoveredCount = 0;
            long currentTime = System.currentTimeMillis();
            
            for (Object item : processingItems) {
                if (isStuckWithTimeout(item, currentTime)) {
                    log.warn("🔄 Recuperando mensagem travada: {}", item.toString().substring(0, Math.min(100, item.toString().length())));
                    removeFromProcessingList(item.toString());
                    
                    // Re-adiciona à fila com timestamp atual para reprocessamento imediato
                    redisTemplate.opsForZSet().add(QUEUE_KEY, item.toString(), currentTime);
                    recoveredCount++;
                }
            }
            
            if (recoveredCount > 0) {
                log.info("🔄 Recuperadas {} mensagens travadas", recoveredCount);
                meterRegistry.counter("campaign.recovery.messages").increment(recoveredCount);
            }
            
            // Limpa tarefas ativas órfãs (sem correspondência na fila de processamento)
            cleanOrphanedActiveTasks();
            
        } catch (Exception e) {
            log.error("Erro na recuperação de mensagens travadas: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Verifica se um item está travado com base em timeout
     */
    private boolean isStuckWithTimeout(Object item, long currentTime) {
        try {
            String itemJson = item.toString();
            
            // Heurística 1: Verificar por timestamp no JSON (se implementado)
            if (itemJson.contains("\"scheduledTime\"")) {
                // Parse do timestamp seria implementado aqui
                // Por ora, usar timeout fixo baseado na idade do processamento
                return isProcessingTooLong();
            }
            
            // Heurística 2: Baseado na discrepância entre itens em processamento e tarefas ativas
            Long processingCount = redisTemplate.opsForList().size(PROCESSING_KEY);
            int activeTaskCount = activeTasks.size();
            
            // Se há muito mais itens em processamento que tarefas ativas, há problema
            if (processingCount != null && processingCount > activeTaskCount + 5) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.warn("Erro ao verificar timeout do item: {}", e.getMessage());
            return true; // Em caso de erro, considera travado para recuperar
        }
    }
    
    /**
     * Verifica se o processamento está demorando muito tempo
     */
    private boolean isProcessingTooLong() {
        // Se não há tarefas ativas mas há itens em processamento, algo está errado
        Long processingSize = redisTemplate.opsForList().size(PROCESSING_KEY);
        return activeTasks.isEmpty() && processingSize != null && processingSize > 0;
    }
    
    /**
     * Remove tarefas ativas órfãs que perderam conexão com a fila
     */
    private void cleanOrphanedActiveTasks() {
        try {
            if (activeTasks.isEmpty()) {
                return;
            }
            
            int initialSize = activeTasks.size();
            
            // Remove tarefas que estão completadas ou canceladas
            activeTasks.entrySet().removeIf(entry -> {
                CompletableFuture<Boolean> future = entry.getValue();
                return future.isDone() || future.isCancelled();
            });
            
            int cleanedTasks = initialSize - activeTasks.size();
            if (cleanedTasks > 0) {
                log.debug("🧹 Limpas {} tarefas ativas órfãs", cleanedTasks);
                meterRegistry.counter("campaign.cleanup.orphaned_tasks").increment(cleanedTasks);
            }
            
        } catch (Exception e) {
            log.error("Erro ao limpar tarefas órfãs: {}", e.getMessage());
        }
    }
    
    private boolean isStuck(Object item) {
        try {
            // Parse do item para verificar se há timestamp
            String itemJson = item.toString();
            
            // Verifica se o item está em processamento há mais tempo que o timeout
            // Para uma implementação completa, você precisaria adicionar timestamp aos itens
            // Por ora, usa uma heurística baseada no número de itens em processamento
            
            // Se há muitos itens em processamento e poucos active tasks, 
            // provavelmente há itens travados
            Long processingCount = redisTemplate.opsForList().size(PROCESSING_KEY);
            int activeTaskCount = activeTasks.size();
            
            // Se há mais de 10 itens em processamento mas menos de 5 tarefas ativas,
            // considera que há itens travados
            if (processingCount != null && processingCount > 10 && activeTaskCount < 5) {
                return true;
            }
            
            // Implementação futura: parse do JSON para verificar timestamp
            // if (itemJson.contains("processingStartTime")) {
            //     // Parse e verificação de timeout
            // }
            
            return false;
            
        } catch (Exception e) {
            log.warn("Erro ao verificar se item está travado: {}", e.getMessage());
            return false;
        }
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("🔄 Iniciando shutdown graceful do processador de campanhas...");
        
        // Para o processamento de novos itens
        isProcessing.set(true);
        
        try {
            // Libera todos os permits do queue processing para evitar novos processamentos
            queueProcessingLimiter.drainPermits();
            
            if (!activeTasks.isEmpty()) {
                log.info("⏳ Aguardando {} tarefas ativas finalizarem...", activeTasks.size());
                
                // Aguarda tarefas ativas finalizarem com timeout
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                    activeTasks.values().toArray(new CompletableFuture[0])
                );
                
                try {
                    allTasks.get(45, TimeUnit.SECONDS); // Timeout maior que o normal
                    log.info("✅ Todas as tarefas finalizaram com sucesso");
                } catch (Exception e) {
                    log.warn("⚠️ Timeout/erro ao aguardar tarefas: {}. Forçando cancelamento...", e.getMessage());
                    
                    // Cancela tarefas pendentes
                    int cancelledCount = 0;
                    for (CompletableFuture<Boolean> task : activeTasks.values()) {
                        if (task.cancel(true)) {
                            cancelledCount++;
                        }
                    }
                    
                    if (cancelledCount > 0) {
                        log.info("🛑 {} tarefas foram canceladas", cancelledCount);
                    }
                }
                
                // Move itens em processamento de volta para a fila principal
                moveProcessingItemsBackToQueue();
                
            } else {
                log.info("📋 Nenhuma tarefa ativa para aguardar");
            }
            
            // Logs finais de estatísticas
            logFinalStatistics();
            
            log.info("🏁 Processador de campanhas encerrado com sucesso");
            
        } catch (Exception e) {
            log.error("❌ Erro durante shutdown: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Move itens que estavam sendo processados de volta para a fila principal
     */
    private void moveProcessingItemsBackToQueue() {
        try {
            List<Object> processingItems = redisTemplate.opsForList().range(PROCESSING_KEY, 0, -1);
            if (processingItems != null && !processingItems.isEmpty()) {
                log.info("🔄 Movendo {} itens de volta para a fila principal", processingItems.size());
                
                long currentTime = System.currentTimeMillis();
                for (Object item : processingItems) {
                    // Remove da fila de processamento
                    redisTemplate.opsForList().remove(PROCESSING_KEY, 1, item);
                    
                    // Adiciona de volta à fila principal com prioridade (timestamp atual)
                    redisTemplate.opsForZSet().add(QUEUE_KEY, item.toString(), currentTime);
                }
                
                log.info("✅ {} itens movidos de volta para processamento futuro", processingItems.size());
                meterRegistry.counter("campaign.shutdown.moved_back").increment(processingItems.size());
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao mover itens de volta para a fila: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Registra estatísticas finais do sistema
     */
    private void logFinalStatistics() {
        try {
            Long queueSize = redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE);
            Long errorSize = redisTemplate.opsForList().size(ERROR_KEY);
            int availableConcurrency = concurrencyLimiter.availablePermits();
            
            log.info("📊 Estatísticas finais do processador:");
            log.info("   • Itens na fila principal: {}", queueSize != null ? queueSize : 0);
            log.info("   • Itens com erro: {}", errorSize != null ? errorSize : 0);
            log.info("   • Slots de concorrência disponíveis: {}/50", availableConcurrency);
            log.info("   • Tarefas ativas restantes: {}", activeTasks.size());
            
        } catch (Exception e) {
            log.warn("Erro ao obter estatísticas finais: {}", e.getMessage());
        }
    }
    
    /**
     * Método para monitoramento manual do status do processador
     */
    public Map<String, Object> getProcessorStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        
        try {
            Long queueSize = redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE);
            Long processingSize = redisTemplate.opsForList().size(PROCESSING_KEY);
            Long errorSize = redisTemplate.opsForList().size(ERROR_KEY);
            
            status.put("isProcessing", isProcessing.get());
            status.put("activeTasks", activeTasks.size());
            status.put("queueSize", queueSize != null ? queueSize : 0);
            status.put("processingSize", processingSize != null ? processingSize : 0);
            status.put("errorSize", errorSize != null ? errorSize : 0);
            status.put("availableConcurrency", concurrencyLimiter.availablePermits());
            status.put("maxConcurrency", 50);
            status.put("canProcessQueue", queueProcessingLimiter.availablePermits() > 0);
            
        } catch (Exception e) {
            log.error("Erro ao obter status do processador: {}", e.getMessage());
            status.put("error", e.getMessage());
        }
        
        return status;
    }
}