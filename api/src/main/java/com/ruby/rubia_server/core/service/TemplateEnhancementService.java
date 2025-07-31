package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.EnhanceTemplateDTO;
import com.ruby.rubia_server.core.dto.EnhancedTemplateResponseDTO;
import com.ruby.rubia_server.core.dto.SaveTemplateWithAIMetadataDTO;
import com.ruby.rubia_server.core.dto.MessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.MessageTemplateRevision;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.AIModelRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRepository;
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
    private final MessageTemplateRepository messageTemplateRepository;
    private final MessageTemplateRevisionService messageTemplateRevisionService;

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
            "friendly", "Crie uma abordagem amigável e calorosa que faça o doador se sentir bem-vindo e valorizado",
            "professional", "Desenvolva uma comunicação profissional que transmita confiança e credibilidade institucional",
            "empathetic", "Use linguagem empática que conecte emocionalmente e mostre como a doação impacta vidas reais",
            "urgent", "Comunique necessidade urgente de forma responsável, motivando ação imediata sem causar pânico",
            "motivational", "Transforme em um convite inspirador que faça o doador se sentir herói e parte de algo maior"
        );

        String enhancementInstruction = enhancements.getOrDefault(request.getEnhancementType(), "Melhore esta mensagem");
        
        return String.format(
            "CONTEXTO: Você é um especialista em captação de doadores de sangue para centros de hematologia e hemoterapia.\n" +
            "OBJETIVO: Criar mensagens persuasivas que motivem pessoas a fazer doações de sangue.\n" +
            "CATEGORIA: %s\n" +
            "TÍTULO: %s\n" +
            "INSTRUÇÃO: %s\n" +
            "\n" +
            "DIRETRIZES OBRIGATÓRIAS:\n" +
            "1. Use {{nome}} para personalização (será substituído pelo nome do doador)\n" +
            "2. Foque na importância social e humanitária da doação\n" +
            "3. Seja persuasivo mas ético - nunca prometa benefícios médicos diretos\n" +
            "4. Destaque como a doação salva vidas e fortalece a comunidade\n" +
            "5. Inclua um call-to-action claro e motivador\n" +
            "6. Use linguagem acessível e empática\n" +
            "7. Mantenha tom respeitoso e não invasivo\n" +
            "\n" +
            "MENSAGEM ORIGINAL: \"%s\"\n" +
            "\n" +
            "Forneça apenas a versão melhorada da mensagem com foco em captação efetiva de doadores.",
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
        // Adicionar personalização se não existir
        if (!content.contains("{{nome}}")) {
            content = "Olá {{nome}}! 😊 " + content;
        }
        
        // Tornar mais amigável e focado em captação
        if (!content.contains("😊") && !content.contains("💝")) {
            content = content.replace(".", "! 💝");
        }
        
        // Adicionar call-to-action amigável
        if (!content.toLowerCase().contains("venha") && !content.toLowerCase().contains("participe")) {
            content += " Venha fazer parte dessa corrente do bem!";
        }
        
        return content;
    }

    private String makeProfessional(String content) {
        // Adicionar personalização formal
        if (!content.contains("{{nome}}")) {
            content = "Prezado(a) {{nome}}, " + content.toLowerCase();
        }
        
        // Formalizar linguagem
        content = content.replace("oi", "Prezado(a)");
        content = content.replace("!", ".");
        content = content.replaceAll("😊|😄|😃|💝|✨", "");
        
        // Adicionar call-to-action profissional
        if (!content.toLowerCase().contains("solicitar") && !content.toLowerCase().contains("convid")) {
            content += " Solicitamos sua valiosa colaboração para salvar vidas em nossa comunidade.";
        }
        
        if (!content.contains("Atenciosamente")) {
            content += "\n\nAtenciosamente,\nEquipe do Centro de Hematologia";
        }
        return content;
    }

    private String addEmpathy(String content) {
        // Adicionar personalização empática
        if (!content.contains("{{nome}}")) {
            content = "{{nome}}, entendemos que sua agenda pode estar corrida, mas " + content.toLowerCase();
        }
        
        // Tornar mais empático e conectivo
        content = content.replace("você deve", "seria possível");
        content = content.replace("precisa", "gostaria de");
        content = content.replace("fazer", "nos ajudar com");
        
        // Adicionar conexão emocional
        if (!content.toLowerCase().contains("vida") && !content.toLowerCase().contains("ajud")) {
            content += " Sua generosidade pode transformar e salvar vidas.";
        }
        
        return content + " 💝";
    }

    private String addUrgency(String content) {
        // Adicionar personalização urgente
        if (!content.contains("{{nome}}")) {
            content = "{{nome}}, IMPORTANTE: " + content.toLowerCase();
        }
        
        // Adicionar urgência responsável
        if (!content.toUpperCase().contains("URGENTE") && !content.toUpperCase().contains("IMPORTANTE")) {
            content = "IMPORTANTE: " + content;
        }
        
        content = content.replace(".", "!");
        
        // Call-to-action urgente mas ético
        if (!content.toLowerCase().contains("hoje") && !content.toLowerCase().contains("agora")) {
            content += " Precisamos de sua doação hoje - vidas dependem disso!";
        }
        
        return content;
    }

    private String makeMotivational(String content) {
        // Adicionar personalização motivacional
        if (!content.contains("{{nome}}")) {
            content = "{{nome}}, que tal ser um herói hoje? " + content.toLowerCase();
        }
        
        // Transformar em linguagem heroica
        content = content.replace("doação", "ato heroico de salvar vidas");
        content = content.replace("doar", "ser um herói");
        content = content.replace("sangue", "esperança e vida");
        
        // Adicionar elementos motivacionais
        if (!content.contains("⭐") && !content.contains("🦸")) {
            content += " ⭐ Você tem o poder de fazer a diferença!";
        }
        
        // Call-to-action inspirador
        if (!content.toLowerCase().contains("herói") || !content.toLowerCase().contains("transform")) {
            content += " Venha transformar vidas conosco! 🦸‍♀️";
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
        // Buscar GPT-4o mini: IDEAL para templates de doação (custo 17x menor que GPT-4o premium)
        List<AIModel> activeModels = aiModelRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc();
        
        // Priorizar GPT-4o mini: 90% da qualidade por 6% do custo
        AIModel defaultModel = activeModels.stream()
                .filter(model -> "gpt-4o-mini".equals(model.getName()))
                .findFirst()
                .orElse(activeModels.isEmpty() ? null : activeModels.get(0));
        
        if (defaultModel == null) {
            throw new RuntimeException("Nenhum modelo de IA ativo encontrado no sistema. Configure ao menos um modelo ativo.");
        }
        
        log.info("Using default AI model: {} ({}) - Optimized for blood donation templates", defaultModel.getDisplayName(), defaultModel.getName());
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
                .description("Agente de IA otimizado para campanhas de doação de sangue. Usa GPT-4o mini para máximo custo-benefício.")
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

    /**
     * Salva um template atualizado com metadados de IA e cria uma revisão com histórico completo
     */
    @Transactional
    public MessageTemplateRevisionDTO saveTemplateWithAIMetadata(SaveTemplateWithAIMetadataDTO request) {
        log.info("Saving template {} with AI metadata from enhancement type: {}", 
                request.getTemplateId(), request.getAiEnhancementType());

        // Buscar o template
        MessageTemplate template = messageTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Template not found with ID: " + request.getTemplateId()));

        // Atualizar o conteúdo do template
        template.setContent(request.getContent());
        template.setEditCount(template.getEditCount() + 1);
        template.setIsAiGenerated(true); // Marcar como gerado por IA
        
        // Se aiAgentId for fornecido, definir o agente que gerou
        if (request.getAiAgentId() != null) {
            AIAgent aiAgent = aiAgentRepository.findById(request.getAiAgentId())
                    .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + request.getAiAgentId()));
            template.setAiAgent(aiAgent);
        }

        // Salvar o template atualizado
        MessageTemplate savedTemplate = messageTemplateRepository.save(template);
        log.debug("Template updated with AI content: {}", savedTemplate.getId());

        // Criar revisão com metadados de IA
        MessageTemplateRevision revision = messageTemplateRevisionService.createAIEnhancementRevision(
                request.getTemplateId(),
                request.getContent(),
                request.getUserId(),
                request.getAiAgentId(),
                request.getAiEnhancementType(),
                request.getAiTokensUsed(),
                request.getAiCreditsConsumed(),
                request.getAiModelUsed(),
                request.getAiExplanation()
        );

        // Converter para DTO
        MessageTemplateRevisionDTO dto = MessageTemplateRevisionDTO.builder()
                .id(revision.getId())
                .templateId(revision.getTemplate().getId())
                .templateName(revision.getTemplate().getName())
                .revisionNumber(revision.getRevisionNumber())
                .content(revision.getContent())
                .editedByUserId(revision.getEditedBy() != null ? revision.getEditedBy().getId() : null)
                .editedByUserName(revision.getEditedBy() != null ? revision.getEditedBy().getName() : null)
                .revisionType(revision.getRevisionType())
                .revisionTimestamp(revision.getRevisionTimestamp())
                .createdAt(revision.getCreatedAt())
                .updatedAt(revision.getUpdatedAt())
                // AI metadata
                .aiAgentId(revision.getAiAgent() != null ? revision.getAiAgent().getId() : null)
                .aiAgentName(revision.getAiAgent() != null ? revision.getAiAgent().getName() : null)
                .aiEnhancementType(revision.getAiEnhancementType())
                .aiTokensUsed(revision.getAiTokensUsed())
                .aiCreditsConsumed(revision.getAiCreditsConsumed())
                .aiModelUsed(revision.getAiModelUsed())
                .aiExplanation(revision.getAiExplanation())
                .build();

        log.info("Template {} successfully saved with AI metadata. Revision {} created.", 
                savedTemplate.getId(), revision.getId());

        return dto;
    }
}