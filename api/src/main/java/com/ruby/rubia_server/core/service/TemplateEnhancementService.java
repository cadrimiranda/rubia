package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.EnhanceTemplateDTO;
import com.ruby.rubia_server.core.dto.EnhancedTemplateResponseDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.AIModelRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateEnhancementService {

    private final AIAgentRepository aiAgentRepository;
    private final AIModelRepository aiModelRepository;
    private final CompanyRepository companyRepository;

    public EnhancedTemplateResponseDTO enhanceTemplate(EnhanceTemplateDTO request) {
        log.info("Enhancing template for company: {} with type: {}", request.getCompanyId(), request.getEnhancementType());

        // Tentar buscar um AI Agent ativo da empresa
        List<AIAgent> activeAgents = aiAgentRepository.findActiveByCompanyId(request.getCompanyId());
        
        AIModel aiModel;
        String modelSource;
        boolean isUsingCompanyAgent = false;
        
        if (!activeAgents.isEmpty()) {
            // Cenário ideal: empresa tem agente configurado
            AIAgent selectedAgent = activeAgents.get(0);
            aiModel = selectedAgent.getAiModel();
            modelSource = "agente da empresa";
            isUsingCompanyAgent = true;
            log.info("Using company's configured AI model: {} for template enhancement", aiModel.getDisplayName());
        } else {
            // Fallback: usar modelo padrão (mais econômico)
            aiModel = getDefaultAIModel();
            modelSource = "modelo padrão do sistema";
            log.info("No company AI agent found, using default model: {} for template enhancement", aiModel.getDisplayName());
        }

        // Gerar prompt baseado no tipo de melhoria
        String prompt = generatePrompt(request, aiModel);
        
        // Simular chamada para a IA (aqui você integraria com a API real do modelo)
        String enhancedContent = simulateAIEnhancement(request.getOriginalContent(), request.getEnhancementType(), aiModel);
        
        // Estimar tokens e créditos usados
        int estimatedTokens = estimateTokens(request.getOriginalContent() + enhancedContent);
        int creditsConsumed = calculateCredits(estimatedTokens, aiModel.getCostPer1kTokens());

        String fullExplanation = generateExplanation(request.getEnhancementType(), aiModel, modelSource, isUsingCompanyAgent);
        
        return EnhancedTemplateResponseDTO.builder()
                .originalContent(request.getOriginalContent())
                .enhancedContent(enhancedContent)
                .enhancementType(request.getEnhancementType())
                .aiModelUsed(aiModel.getDisplayName() + (isUsingCompanyAgent ? "" : " (Padrão)"))
                .tokensUsed(estimatedTokens)
                .creditsConsumed(creditsConsumed)
                .explanation(fullExplanation)
                .build();
    }

    private String generatePrompt(EnhanceTemplateDTO request, AIModel aiModel) {
        Map<String, String> enhancements = Map.of(
            "friendly", "Torne esta mensagem mais amigável e calorosa, mantendo um tom acolhedor",
            "professional", "Transforme esta mensagem em um formato mais profissional e formal",
            "empathetic", "Adicione empatia e compreensão a esta mensagem, demonstrando cuidado",
            "urgent", "Torne esta mensagem mais urgente, transmitindo importância sem ser agressivo",
            "motivational", "Transforme esta mensagem em algo inspirador e motivacional"
        );

        String enhancementInstruction = enhancements.getOrDefault(request.getEnhancementType(), "Melhore esta mensagem");
        
        return String.format(
            "Contexto: Você é um assistente especializado em comunicação para centros de hematologia e hemoterapia.\n" +
            "Categoria do template: %s\n" +
            "Título: %s\n" +
            "Instrução: %s\n" +
            "Mensagem original: \"%s\"\n" +
            "Forneça apenas a versão melhorada da mensagem, mantendo o contexto médico apropriado.",
            request.getCategory(),
            request.getTitle() != null ? request.getTitle() : "N/A",
            enhancementInstruction,
            request.getOriginalContent()
        );
    }

    private String simulateAIEnhancement(String originalContent, String enhancementType, AIModel aiModel) {
        // Esta é uma simulação. Em um ambiente real, você faria a chamada para a API do modelo AI
        switch (enhancementType) {
            case "friendly":
                return addFriendlyTouch(originalContent);
            case "professional":
                return makeProfessional(originalContent);
            case "empathetic":
                return addEmpathy(originalContent);
            case "urgent":
                return addUrgency(originalContent);
            case "motivational":
                return makeMotivational(originalContent);
            default:
                return originalContent + " (melhorado)";
        }
    }

    private String addFriendlyTouch(String content) {
        if (!content.contains("😊") && !content.contains("!")) {
            content = content.replace(".", "! 😊");
        }
        if (!content.toLowerCase().startsWith("olá")) {
            content = "Olá! " + content;
        }
        return content;
    }

    private String makeProfessional(String content) {
        content = content.replace("oi", "Prezado(a),");
        content = content.replace("!", ".");
        content = content.replaceAll("😊|😄|😃", "");
        if (!content.contains("Atenciosamente")) {
            content += "\n\nAtenciosamente,\nEquipe do Centro de Hematologia";
        }
        return content;
    }

    private String addEmpathy(String content) {
        if (!content.toLowerCase().contains("entend")) {
            content = "Entendemos que sua agenda pode estar corrida, mas " + content.toLowerCase();
        }
        content = content.replace("você deve", "seria possível");
        content = content.replace("precisa", "gostaria de");
        return content + " 💝";
    }

    private String addUrgency(String content) {
        if (!content.toUpperCase().contains("URGENTE") && !content.toUpperCase().contains("IMPORTANTE")) {
            content = "IMPORTANTE: " + content;
        }
        content = content.replace(".", "!");
        content += "\n\nSua doação pode salvar vidas hoje!";
        return content;
    }

    private String makeMotivational(String content) {
        content = content.replace("doação", "ato heroico de salvar vidas");
        content = content.replace("doar", "ser um herói");
        if (!content.contains("⭐") && !content.contains("🦸")) {
            content += " ⭐ Você pode fazer a diferença!";
        }
        return content;
    }

    private int estimateTokens(String text) {
        // Estimativa simples: aproximadamente 4 caracteres por token
        return text.length() / 4;
    }

    private int calculateCredits(int tokens, Integer costPer1kTokens) {
        if (costPer1kTokens == null) return 0;
        return (int) Math.ceil((tokens / 1000.0) * costPer1kTokens);
    }

    private AIModel getDefaultAIModel() {
        // Buscar o modelo mais econômico ativo (GPT-4 Mini)
        List<AIModel> activeModels = aiModelRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc();
        
        // Tentar encontrar o GPT-4 Mini primeiro (mais econômico)
        AIModel defaultModel = activeModels.stream()
                .filter(model -> "gpt-4o-mini".equals(model.getName()))
                .findFirst()
                .orElse(activeModels.isEmpty() ? null : activeModels.get(0));
        
        if (defaultModel == null) {
            throw new RuntimeException("Nenhum modelo de IA ativo encontrado no sistema. Configure ao menos um modelo ativo.");
        }
        
        log.info("Using default AI model: {} ({})", defaultModel.getDisplayName(), defaultModel.getName());
        return defaultModel;
    }

    private String generateExplanation(String enhancementType, AIModel aiModel, String modelSource, boolean isUsingCompanyAgent) {
        Map<String, String> explanations = Map.of(
            "friendly", "Adicionei saudações calorosas e emojis para criar um tom mais acolhedor",
            "professional", "Formalizei a linguagem e adicionei estrutura profissional à mensagem",
            "empathetic", "Incluí expressões de compreensão e consideração pelos sentimentos do destinatário",
            "urgent", "Destaquei a importância e urgência usando linguagem impactante",
            "motivational", "Transformei a mensagem em um convite inspirador para ação heroica"
        );

        String baseExplanation = explanations.getOrDefault(enhancementType, "Melhorei a mensagem");
        String modelInfo = String.format("usando o modelo %s", aiModel.getDisplayName());
        
        if (!isUsingCompanyAgent) {
            modelInfo += " (modelo padrão do sistema)";
            baseExplanation += ". 💡 Dica: Configure um agente de IA na seção 'Configuração de Agente' para usar um modelo personalizado para sua empresa";
        }
        
        return String.format("%s %s.", baseExplanation, modelInfo);
    }

    /**
     * Método opcional para auto-configurar um agente padrão para empresas novas.
     * Chame este método quando uma empresa for criada para facilitar o onboarding.
     */
    @Transactional
    public AIAgent createDefaultAgentForCompany(UUID companyId) {
        log.info("Creating default AI agent for company: {}", companyId);
        
        // Verificar se a empresa existe
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyId));
        
        // Verificar se já existe algum agente para evitar duplicatas
        List<AIAgent> existingAgents = aiAgentRepository.findByCompanyId(companyId);
        if (!existingAgents.isEmpty()) {
            log.info("Company already has AI agents, skipping default creation");
            return existingAgents.get(0);
        }
        
        // Buscar modelo padrão (mais econômico)
        AIModel defaultModel = getDefaultAIModel();
        
        // Criar agente padrão
        AIAgent defaultAgent = AIAgent.builder()
                .company(company)
                .aiModel(defaultModel)
                .name("Assistente " + company.getName())
                .description("Agente de IA padrão para melhoria de templates e comunicação com doadores.")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(java.math.BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        
        defaultAgent = aiAgentRepository.save(defaultAgent);
        log.info("Default AI agent created successfully for company: {} with model: {}", 
                company.getName(), defaultModel.getDisplayName());
        
        return defaultAgent;
    }
}