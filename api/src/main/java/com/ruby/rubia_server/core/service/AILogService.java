package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateAILogDTO;
import com.ruby.rubia_server.core.dto.UpdateAILogDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.AILogStatus;
import com.ruby.rubia_server.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AILogService {

    private final AILogRepository aiLogRepository;
    private final CompanyRepository companyRepository;
    private final AIAgentRepository aiAgentRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageTemplateRepository messageTemplateRepository;

    public AILog createAILog(CreateAILogDTO createDTO) {
        log.debug("Creating AI log for company: {}", createDTO.getCompanyId());

        Company company = companyRepository.findById(createDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + createDTO.getCompanyId()));

        AIAgent aiAgent = aiAgentRepository.findById(createDTO.getAiAgentId())
                .orElseThrow(() -> new RuntimeException("AI Agent not found with ID: " + createDTO.getAiAgentId()));

        User user = null;
        if (createDTO.getUserId() != null) {
            user = userRepository.findById(createDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + createDTO.getUserId()));
        }

        Conversation conversation = null;
        if (createDTO.getConversationId() != null) {
            conversation = conversationRepository.findById(createDTO.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + createDTO.getConversationId()));
        }

        Message message = null;
        if (createDTO.getMessageId() != null) {
            message = messageRepository.findById(createDTO.getMessageId())
                    .orElseThrow(() -> new RuntimeException("Message not found with ID: " + createDTO.getMessageId()));
        }

        MessageTemplate messageTemplate = null;
        if (createDTO.getMessageTemplateId() != null) {
            messageTemplate = messageTemplateRepository.findById(createDTO.getMessageTemplateId())
                    .orElseThrow(() -> new RuntimeException("Message template not found with ID: " + createDTO.getMessageTemplateId()));
        }

        AILog aiLog = AILog.builder()
                .company(company)
                .aiAgent(aiAgent)
                .user(user)
                .conversation(conversation)
                .message(message)
                .messageTemplate(messageTemplate)
                .requestPrompt(createDTO.getRequestPrompt())
                .rawResponse(createDTO.getRawResponse())
                .processedResponse(createDTO.getProcessedResponse())
                .tokensUsedInput(createDTO.getTokensUsedInput())
                .tokensUsedOutput(createDTO.getTokensUsedOutput())
                .estimatedCost(createDTO.getEstimatedCost())
                .status(createDTO.getStatus())
                .errorMessage(createDTO.getErrorMessage())
                .build();

        return aiLogRepository.save(aiLog);
    }

    @Transactional(readOnly = true)
    public Optional<AILog> getAILogById(UUID id) {
        log.debug("Fetching AI log with ID: {}", id);
        return aiLogRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<AILog> getAllAILogs(Pageable pageable) {
        log.debug("Fetching all AI logs with pagination");
        return aiLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<AILog> getAILogsByCompanyId(UUID companyId) {
        log.debug("Fetching AI logs for company: {}", companyId);
        return aiLogRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<AILog> getAILogsByStatus(AILogStatus status) {
        log.debug("Fetching AI logs with status: {}", status);
        return aiLogRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<AILog> getAILogsByAIAgentId(UUID aiAgentId) {
        log.debug("Fetching AI logs for AI agent: {}", aiAgentId);
        return aiLogRepository.findByAiAgentId(aiAgentId);
    }

    public Optional<AILog> updateAILog(UUID id, UpdateAILogDTO updateDTO) {
        log.debug("Updating AI log with ID: {}", id);

        Optional<AILog> existingAILog = aiLogRepository.findById(id);
        if (existingAILog.isEmpty()) {
            return Optional.empty();
        }

        AILog aiLog = existingAILog.get();

        if (updateDTO.getRawResponse() != null) {
            aiLog.setRawResponse(updateDTO.getRawResponse());
        }
        if (updateDTO.getProcessedResponse() != null) {
            aiLog.setProcessedResponse(updateDTO.getProcessedResponse());
        }
        if (updateDTO.getTokensUsedOutput() != null) {
            aiLog.setTokensUsedOutput(updateDTO.getTokensUsedOutput());
        }
        if (updateDTO.getEstimatedCost() != null) {
            aiLog.setEstimatedCost(updateDTO.getEstimatedCost());
        }
        if (updateDTO.getStatus() != null) {
            aiLog.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getErrorMessage() != null) {
            aiLog.setErrorMessage(updateDTO.getErrorMessage());
        }

        return Optional.of(aiLogRepository.save(aiLog));
    }

    public boolean deleteAILog(UUID id) {
        log.debug("Deleting AI log with ID: {}", id);
        if (aiLogRepository.existsById(id)) {
            aiLogRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalCostByCompanyId(UUID companyId) {
        log.debug("Calculating total cost for company: {}", companyId);
        return aiLogRepository.sumEstimatedCostByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public Long getTotalTokensUsedByCompanyId(UUID companyId) {
        log.debug("Calculating total tokens used for company: {}", companyId);
        return aiLogRepository.sumTokensUsedByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long countAILogsByCompanyIdAndStatus(UUID companyId, AILogStatus status) {
        log.debug("Counting AI logs for company: {} with status: {}", companyId, status);
        return aiLogRepository.countByCompanyIdAndStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public List<AILog> getAILogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching AI logs between {} and {}", startDate, endDate);
        return aiLogRepository.findByCreatedAtBetween(startDate, endDate);
    }
}