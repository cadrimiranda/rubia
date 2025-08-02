package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.MessageEnhancementAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageEnhancementAuditService {

    private final MessageEnhancementAuditRepository auditRepository;

    /**
     * Registra uma melhoria de mensagem bem-sucedida
     */
    public MessageEnhancementAudit recordSuccessfulEnhancement(
            Company company,
            User user,
            AIAgent aiAgent,
            String originalMessage,
            String enhancedMessage,
            UUID conversationId,
            Integer tokensConsumed,
            Long responseTimeMs,
            String userAgent,
            String ipAddress,
            String openaiSystemMessage,
            String openaiUserMessage,
            String openaiFullPayload) {

        log.debug("Recording successful message enhancement for user: {} with agent: {}", 
                 user.getId(), aiAgent.getName());

        MessageEnhancementAudit audit = MessageEnhancementAudit.builder()
                .company(company)
                .user(user)
                .aiAgent(aiAgent)
                .conversationId(conversationId)
                .originalMessage(originalMessage)
                .enhancedMessage(enhancedMessage)
                .temperamentUsed(aiAgent.getTemperament())
                .aiModelUsed(aiAgent.getAiModel().getName())
                .temperatureUsed(aiAgent.getTemperature().doubleValue())
                .maxTokensUsed(aiAgent.getMaxResponseLength())
                .tokensConsumed(tokensConsumed)
                .responseTimeMs(responseTimeMs)
                .success(true)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .openaiSystemMessage(openaiSystemMessage)
                .openaiUserMessage(openaiUserMessage)
                .openaiFullPayload(openaiFullPayload)
                .build();

        MessageEnhancementAudit saved = auditRepository.save(audit);
        log.info("Message enhancement audit recorded successfully with ID: {}", saved.getId());
        
        return saved;
    }

    /**
     * Registra uma melhoria de mensagem que falhou
     */
    public MessageEnhancementAudit recordFailedEnhancement(
            Company company,
            User user,
            AIAgent aiAgent,
            String originalMessage,
            String errorMessage,
            UUID conversationId,
            Long responseTimeMs,
            String userAgent,
            String ipAddress,
            String openaiSystemMessage,
            String openaiUserMessage,
            String openaiFullPayload) {

        log.debug("Recording failed message enhancement for user: {} with agent: {}", 
                 user.getId(), aiAgent.getName());

        MessageEnhancementAudit audit = MessageEnhancementAudit.builder()
                .company(company)
                .user(user)
                .aiAgent(aiAgent)
                .conversationId(conversationId)
                .originalMessage(originalMessage)
                .enhancedMessage(null) // Não há mensagem melhorada em caso de falha
                .temperamentUsed(aiAgent.getTemperament())
                .aiModelUsed(aiAgent.getAiModel().getName())
                .temperatureUsed(aiAgent.getTemperature().doubleValue())
                .maxTokensUsed(aiAgent.getMaxResponseLength())
                .tokensConsumed(null) // Não houve consumo de tokens em falha
                .responseTimeMs(responseTimeMs)
                .success(false)
                .errorMessage(errorMessage)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .openaiSystemMessage(openaiSystemMessage)
                .openaiUserMessage(openaiUserMessage)
                .openaiFullPayload(openaiFullPayload)
                .build();

        MessageEnhancementAudit saved = auditRepository.save(audit);
        log.warn("Message enhancement failure recorded with ID: {} - Error: {}", saved.getId(), errorMessage);
        
        return saved;
    }

    /**
     * Busca auditorias por empresa com paginação
     */
    @Transactional(readOnly = true)
    public Page<MessageEnhancementAudit> getAuditsByCompany(UUID companyId, Pageable pageable) {
        return auditRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * Busca auditorias por usuário com paginação
     */
    @Transactional(readOnly = true)
    public Page<MessageEnhancementAudit> getAuditsByUser(UUID userId, Pageable pageable) {
        return auditRepository.findByUserId(userId, pageable);
    }

    /**
     * Busca auditorias por agente IA com paginação
     */
    @Transactional(readOnly = true)
    public Page<MessageEnhancementAudit> getAuditsByAiAgent(UUID aiAgentId, Pageable pageable) {
        return auditRepository.findByAiAgentId(aiAgentId, pageable);
    }

    /**
     * Busca auditorias por conversa
     */
    @Transactional(readOnly = true)
    public List<MessageEnhancementAudit> getAuditsByConversation(UUID conversationId) {
        return auditRepository.findByConversationId(conversationId);
    }

    /**
     * Estatísticas de melhorias por empresa
     */
    @Transactional(readOnly = true)
    public EnhancementStats getCompanyStats(UUID companyId) {
        long successful = auditRepository.countSuccessfulEnhancementsByCompany(companyId);
        long failed = auditRepository.countFailedEnhancementsByCompany(companyId);
        long totalTokens = auditRepository.sumTokensConsumedByCompany(companyId);
        Double avgResponseTime = auditRepository.averageResponseTimeByCompany(companyId);

        return EnhancementStats.builder()
                .successfulEnhancements(successful)
                .failedEnhancements(failed)
                .totalEnhancements(successful + failed)
                .totalTokensConsumed(totalTokens)
                .averageResponseTimeMs(avgResponseTime != null ? avgResponseTime : 0.0)
                .successRate(successful + failed > 0 ? (double) successful / (successful + failed) * 100 : 0.0)
                .build();
    }

    /**
     * Busca auditorias por período
     */
    @Transactional(readOnly = true)
    public Page<MessageEnhancementAudit> getAuditsByDateRange(
            UUID companyId, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable) {
        return auditRepository.findByCompanyIdAndDateRange(companyId, startDate, endDate, pageable);
    }

    /**
     * Busca auditorias por temperamento
     */
    @Transactional(readOnly = true)
    public Page<MessageEnhancementAudit> getAuditsByTemperament(
            UUID companyId, 
            String temperament, 
            Pageable pageable) {
        return auditRepository.findByCompanyIdAndTemperament(companyId, temperament, pageable);
    }

    /**
     * Busca auditorias por modelo de IA
     */
    @Transactional(readOnly = true)
    public Page<MessageEnhancementAudit> getAuditsByAiModel(
            UUID companyId, 
            String model, 
            Pageable pageable) {
        return auditRepository.findByCompanyIdAndAiModel(companyId, model, pageable);
    }

    /**
     * Classe para estatísticas de enhancement
     */
    @lombok.Data
    @lombok.Builder
    public static class EnhancementStats {
        private long successfulEnhancements;
        private long failedEnhancements;
        private long totalEnhancements;
        private long totalTokensConsumed;
        private double averageResponseTimeMs;
        private double successRate; // Porcentagem
    }
}