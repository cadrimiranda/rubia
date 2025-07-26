package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.EnhanceTemplateDTO;
import com.ruby.rubia_server.core.dto.EnhancedTemplateResponseDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateEnhancementService {

    private final AIAgentRepository aiAgentRepository;

    public EnhancedTemplateResponseDTO enhanceTemplate(EnhanceTemplateDTO request) {
        log.info("Enhancing template for company: {} with type: {}", request.getCompanyId(), request.getEnhancementType());

        // Buscar um AI Agent ativo da empresa (pegamos o primeiro disponível)
        List<AIAgent> activeAgents = aiAgentRepository.findActiveByCompanyId(request.getCompanyId());
        
        if (activeAgents.isEmpty()) {
            throw new RuntimeException("Nenhum agente de IA ativo encontrado para a empresa");
        }

        AIAgent selectedAgent = activeAgents.get(0); // Pega o primeiro agente ativo
        AIModel aiModel = selectedAgent.getAiModel();
        
        log.info("Using AI model: {} for template enhancement", aiModel.getDisplayName());

        // Gerar prompt baseado no tipo de melhoria
        String prompt = generatePrompt(request, aiModel);
        
        // Simular chamada para a IA (aqui você integraria com a API real do modelo)
        String enhancedContent = simulateAIEnhancement(request.getOriginalContent(), request.getEnhancementType(), aiModel);
        
        // Estimar tokens e créditos usados
        int estimatedTokens = estimateTokens(request.getOriginalContent() + enhancedContent);
        int creditsConsumed = calculateCredits(estimatedTokens, aiModel.getCostPer1kTokens());

        return EnhancedTemplateResponseDTO.builder()
                .originalContent(request.getOriginalContent())
                .enhancedContent(enhancedContent)
                .enhancementType(request.getEnhancementType())
                .aiModelUsed(aiModel.getDisplayName())
                .tokensUsed(estimatedTokens)
                .creditsConsumed(creditsConsumed)
                .explanation(generateExplanation(request.getEnhancementType(), aiModel))
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

    private String generateExplanation(String enhancementType, AIModel aiModel) {
        Map<String, String> explanations = Map.of(
            "friendly", "Adicionei saudações calorosas e emojis para criar um tom mais acolhedor",
            "professional", "Formalizei a linguagem e adicionei estrutura profissional à mensagem",
            "empathetic", "Incluí expressões de compreensão e consideração pelos sentimentos do destinatário",
            "urgent", "Destaquei a importância e urgência usando linguagem impactante",
            "motivational", "Transformei a mensagem em um convite inspirador para ação heroica"
        );

        String baseExplanation = explanations.getOrDefault(enhancementType, "Melhorei a mensagem");
        return String.format("%s usando o modelo %s (%s tokens estimados).", 
                           baseExplanation, aiModel.getDisplayName(), aiModel.getName());
    }
}