package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.AIModelRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AIAgentService {

    private final AIAgentRepository aiAgentRepository;
    private final CompanyRepository companyRepository;
    private final AIModelRepository aiModelRepository;
    private final UserRepository userRepository;
    private final OpenAIService openAIService;
    private final MessageEnhancementAuditService auditService;

    public AIAgent createAIAgent(CreateAIAgentDTO createDTO) {
        log.info("Creating AI agent with name: {} for company: {}", createDTO.getName(), createDTO.getCompanyId());

        // Validate company exists
        Company company = companyRepository.findById(createDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + createDTO.getCompanyId()));

        // Verificar limite de agentes para a empresa
        long currentAgentCount = countAIAgentsByCompanyId(createDTO.getCompanyId());
        if (currentAgentCount >= company.getMaxAiAgents()) {
            throw new RuntimeException(String.format(
                "Limite de agentes IA atingido. Plano atual permite %d agente(s), empresa já possui %d.",
                company.getMaxAiAgents(), currentAgentCount
            ));
        }

        // Validate AI model exists
        AIModel aiModel = aiModelRepository.findById(createDTO.getAiModelId())
                .orElseThrow(() -> new RuntimeException("AI Model not found with ID: " + createDTO.getAiModelId()));

        // Create AI agent
        AIAgent aiAgent = AIAgent.builder()
                .company(company)
                .aiModel(aiModel)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .avatarBase64(createDTO.getAvatarBase64())
                .temperament(createDTO.getTemperament())
                .maxResponseLength(createDTO.getMaxResponseLength())
                .temperature(createDTO.getTemperature())
                .isActive(createDTO.getIsActive())
                .build();

        aiAgent = aiAgentRepository.save(aiAgent);
        log.info("AI agent created successfully with id: {}", aiAgent.getId());
        
        // Força refresh para garantir que timestamps do banco sejam carregados
        aiAgentRepository.flush();
        return aiAgentRepository.findById(aiAgent.getId()).orElse(aiAgent);
    }

    @Transactional(readOnly = true)
    public Optional<AIAgent> getAIAgentById(UUID id) {
        log.debug("Fetching AI agent with id: {}", id);
        return aiAgentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<AIAgent> getAllAIAgents(Pageable pageable) {
        log.debug("Fetching all AI agents with pagination: {}", pageable);
        return aiAgentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByCompanyId(UUID companyId) {
        log.debug("Fetching AI agents for company: {}", companyId);
        return aiAgentRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getActiveAIAgentsByCompanyId(UUID companyId) {
        log.debug("Fetching active AI agents for company: {}", companyId);
        return aiAgentRepository.findActiveByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByCompanyIdOrderByName(UUID companyId) {
        log.debug("Fetching AI agents for company ordered by name: {}", companyId);
        return aiAgentRepository.findByCompanyIdOrderByName(companyId);
    }

    public Optional<AIAgent> updateAIAgent(UUID id, UpdateAIAgentDTO updateDTO) {
        log.info("Updating AI agent with id: {}", id);

        Optional<AIAgent> aiAgentOpt = aiAgentRepository.findById(id);
        if (aiAgentOpt.isEmpty()) {
            log.warn("AI agent not found with id: {}", id);
            return Optional.empty();
        }

        AIAgent aiAgent = aiAgentOpt.get();

        // Update fields if provided
        if (updateDTO.getName() != null) {
            aiAgent.setName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            aiAgent.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getAvatarBase64() != null) {
            aiAgent.setAvatarBase64(updateDTO.getAvatarBase64());
        }
        if (updateDTO.getAiModelId() != null) {
            AIModel aiModel = aiModelRepository.findById(updateDTO.getAiModelId())
                    .orElseThrow(() -> new RuntimeException("AI Model not found with ID: " + updateDTO.getAiModelId()));
            aiAgent.setAiModel(aiModel);
        }
        if (updateDTO.getTemperament() != null) {
            aiAgent.setTemperament(updateDTO.getTemperament());
        }
        if (updateDTO.getMaxResponseLength() != null) {
            aiAgent.setMaxResponseLength(updateDTO.getMaxResponseLength());
        }
        if (updateDTO.getTemperature() != null) {
            aiAgent.setTemperature(updateDTO.getTemperature());
        }
        if (updateDTO.getIsActive() != null) {
            aiAgent.setIsActive(updateDTO.getIsActive());
        }

        aiAgent = aiAgentRepository.save(aiAgent);
        log.info("AI agent updated successfully with id: {}", aiAgent.getId());
        
        return Optional.of(aiAgent);
    }

    public boolean deleteAIAgent(UUID id) {
        log.info("Deleting AI agent with id: {}", id);

        if (!aiAgentRepository.existsById(id)) {
            log.warn("AI agent not found with id: {}", id);
            return false;
        }

        aiAgentRepository.deleteById(id);
        log.info("AI agent deleted successfully");
        return true;
    }

    @Transactional(readOnly = true)
    public long countAIAgentsByCompanyId(UUID companyId) {
        log.debug("Counting AI agents for company: {}", companyId);
        return aiAgentRepository.countByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long countActiveAIAgentsByCompanyId(UUID companyId) {
        log.debug("Counting active AI agents for company: {}", companyId);
        return aiAgentRepository.countActiveByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndCompanyId(String name, UUID companyId) {
        log.debug("Checking if AI agent exists with name: {} for company: {}", name, companyId);
        return aiAgentRepository.existsByNameAndCompanyId(name, companyId);
    }

    @Transactional(readOnly = true)
    public boolean canCreateAgent(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyId));
        
        long currentCount = countAIAgentsByCompanyId(companyId);
        return currentCount < company.getMaxAiAgents();
    }

    @Transactional(readOnly = true)
    public int getRemainingAgentSlots(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyId));
        
        long currentCount = countAIAgentsByCompanyId(companyId);
        return Math.max(0, company.getMaxAiAgents() - (int) currentCount);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByModelId(UUID modelId) {
        log.debug("Fetching AI agents by model id: {}", modelId);
        return aiAgentRepository.findByAiModelId(modelId);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByModelName(String modelName) {
        log.debug("Fetching AI agents by model name: {}", modelName);
        return aiAgentRepository.findByAiModelName(modelName);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByTemperament(String temperament) {
        log.debug("Fetching AI agents by temperament: {}", temperament);
        return aiAgentRepository.findByTemperament(temperament);
    }

    public String enhanceMessage(UUID companyId, String originalMessage, UUID userId, UUID conversationId, String userAgent, String ipAddress) {
        log.debug("Enhancing message for company: {} with text: {}", companyId, originalMessage);

        long startTime = System.currentTimeMillis();

        // Get company entity
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyId));

        // Get user entity (only if userId is provided)
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            // Validate user belongs to company
            if (!user.getCompany().getId().equals(companyId)) {
                throw new RuntimeException("User does not belong to the specified company");
            }
        }

        // Get the first active AI agent for the company
        List<AIAgent> activeAgents = getActiveAIAgentsByCompanyId(companyId);
        if (activeAgents.isEmpty()) {
            String errorMessage = "Nenhum agente IA ativo encontrado para esta empresa. Configure um agente primeiro.";
            
            // Record failed enhancement (without agent, so we'll handle this differently)
            log.error("No active AI agent found for company: {}", companyId);
            throw new RuntimeException(errorMessage);
        }

        AIAgent agent = activeAgents.get(0); // Use the first active agent

        // Create enhancement prompt based on the agent's temperament and characteristics
        String enhancementPrompt = buildEnhancementPrompt(agent, originalMessage);

        try {
            // Use OpenAI service to enhance the message
            String enhancedMessage = openAIService.enhanceTemplate(
                enhancementPrompt,
                agent.getAiModel().getName(),
                agent.getTemperature().doubleValue(),
                agent.getMaxResponseLength()
            );

            long responseTime = System.currentTimeMillis() - startTime;

            // Record successful enhancement
            auditService.recordSuccessfulEnhancement(
                company,
                user,
                agent,
                originalMessage,
                enhancedMessage,
                conversationId,
                estimateTokensUsed(originalMessage, enhancedMessage), // Estimate tokens used
                responseTime,
                userAgent,
                ipAddress
            );

            log.info("Message enhanced successfully for company: {} in {}ms", companyId, responseTime);
            return enhancedMessage;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            String errorMessage = "Erro ao melhorar mensagem: " + e.getMessage();

            // Record failed enhancement
            auditService.recordFailedEnhancement(
                company,
                user,
                agent,
                originalMessage,
                errorMessage,
                conversationId,
                responseTime,
                userAgent,
                ipAddress
            );

            log.error("Error enhancing message for company: {} - {}", companyId, e.getMessage(), e);
            throw new RuntimeException(errorMessage);
        }
    }

    // Overload method for backward compatibility
    public String enhanceMessage(UUID companyId, String originalMessage) {
        return enhanceMessage(companyId, originalMessage, null, null, null, null);
    }

    private String buildEnhancementPrompt(AIAgent agent, String originalMessage) {
        String basePersonality = "Você é " + agent.getName();
        if (agent.getDescription() != null && !agent.getDescription().trim().isEmpty()) {
            basePersonality += ", " + agent.getDescription();
        }

        String temperamentGuidance = getTemperamentGuidance(agent.getTemperament());

        return basePersonality + "\n\n" +
               temperamentGuidance + "\n\n" +
               "Por favor, melhore a seguinte mensagem mantendo sua essência, mas tornando-a mais clara, envolvente e adequada ao contexto de saúde e doação de sangue:\n\n" +
               "Mensagem original: \"" + originalMessage + "\"\n\n" +
               "Responda apenas com a mensagem melhorada, sem explicações adicionais.";
    }

    private String getTemperamentGuidance(String temperament) {
        switch (temperament.toLowerCase()) {
            case "formal":
                return "Mantenha um tom profissional, respeitoso e formal. Use linguagem técnica quando apropriado e seja direto e objetivo.";
            case "amigavel":
                return "Use um tom caloroso, acolhedor e amigável. Seja empático e próximo, criando conexão emocional com o destinatário.";
            case "motivacional":
                return "Seja inspirador e motivador. Use linguagem que encoraje ação e desperte o senso de propósito e importância da doação.";
            case "educativo":
                return "Foque em informar e educar. Inclua dados relevantes, explique processos e tire dúvidas de forma clara e didática.";
            case "urgente":
                return "Transmita urgência de forma respeitosa. Comunique a necessidade imediata sem causar ansiedade desnecessária.";
            case "emotivo":
                return "Conecte-se emocionalmente através de histórias, depoimentos ou situações que toquem o coração das pessoas.";
            default:
                return "Mantenha um tom equilibrado, claro e respeitoso, adequado ao contexto de saúde e bem-estar social.";
        }
    }

    /**
     * Estima o número de tokens usados baseado no comprimento das mensagens
     * Esta é uma estimativa aproximada, idealmente seria obtida da resposta da OpenAI
     */
    private Integer estimateTokensUsed(String originalMessage, String enhancedMessage) {
        // Estimativa básica: 1 token ≈ 4 caracteres (para português)
        int inputTokens = originalMessage.length() / 4;
        int outputTokens = enhancedMessage.length() / 4;
        return inputTokens + outputTokens;
    }
}