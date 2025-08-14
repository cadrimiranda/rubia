package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.*;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.service.OpenAIService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AIDraftService {
    
    private final MessageDraftRepository messageDraftRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final FAQService faqService;
    private final MessageTemplateService messageTemplateService;
    private final OpenAIService openAIService;
    private final AIAgentService aiAgentService;
    private final UserRepository userRepository;
    private final CompanyContextUtil companyContextUtil;
    
    /**
     * Gera draft automaticamente baseado na mensagem do cliente
     */
    public MessageDTO generateDraftResponse(UUID conversationId, String userMessage) {
        log.info("Generating draft response for conversation: {} with message: '{}'", conversationId, userMessage);
        
        try {
            // 1. Verificar se deve gerar draft
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversa não encontrada"));
                
            if (!shouldGenerateDraft(conversation, userMessage)) {
                log.debug("Skipping draft generation for conversation: {}", conversationId);
                return null;
            }
            
            // 2. Buscar FAQs relevantes
            List<FAQMatchDTO> faqMatches = searchRelevantFAQs(conversation.getCompany().getId(), userMessage);
            
            // 3. Buscar templates relevantes  
            List<MessageTemplate> templates = searchRelevantTemplates(conversation.getCompany().getId(), userMessage);
            
            // 4. Determinar melhor resposta baseada em confiança
            DraftResponse bestResponse = selectBestResponse(faqMatches, templates, userMessage, conversation.getCompany().getId());
            
            if (bestResponse == null) {
                log.debug("❌ [DEBUG] No response selected for conversation: {}", conversationId);
                return null;
            }
            
            if (bestResponse.getConfidence() < 0.5) {
                log.debug("❌ [DEBUG] Response confidence too low: {:.2f} (minimum 0.5) for conversation: {}", 
                    bestResponse.getConfidence(), conversationId);
                return null;
            }
            
            log.debug("✅ [DEBUG] Selected response with confidence {:.2f} for conversation: {}", 
                bestResponse.getConfidence(), conversationId);
            
            // 5. Criar Message com status DRAFT (não MessageDraft separada)
            log.debug("✅ [DEBUG] Creating Message with DRAFT status, source: {} and confidence: {:.2f}", 
                bestResponse.getSourceType(), bestResponse.getConfidence());
                
            MessageDTO result = createDraftMessage(conversation, bestResponse, userMessage);
            log.info("✅ [SUCCESS] Draft Message created successfully with ID: {} for conversation: {}", 
                result.getId(), conversationId);
            return result;
            
        } catch (Exception e) {
            log.error("Error generating draft for conversation: {}", conversationId, e);
            return null;
        }
    }
    
    /**
     * Cria um draft manualmente
     */
    public MessageDraftDTO createDraft(CreateMessageDraftDTO createDTO) {
        log.info("Creating draft for conversation: {}", createDTO.getConversationId());
        
        Conversation conversation = conversationRepository.findById(createDTO.getConversationId())
            .orElseThrow(() -> new RuntimeException("Conversa não encontrada"));
            
        // Em contexto assíncrono, não há usuário autenticado
        // Usar usuário sistema ou primeiro usuário ativo da empresa
        User currentUser;
        try {
            currentUser = companyContextUtil.getAuthenticatedUser();
        } catch (IllegalStateException e) {
            log.debug("No authenticated user in async context, using system user for draft creation");
            // Buscar primeiro usuário da empresa como fallback
            List<User> companyUsers = userRepository.findByCompanyId(conversation.getCompany().getId());
            currentUser = companyUsers.isEmpty() ? null : companyUsers.get(0);
            
            if (currentUser == null) {
                log.warn("No users found for company {}, creating draft without user", conversation.getCompany().getId());
            }
        }
        
        MessageDraft draft = new MessageDraft();
        draft.setCompany(conversation.getCompany());
        draft.setConversation(conversation);
        draft.setContent(createDTO.getContent());
        draft.setAiModel(createDTO.getAiModel());
        draft.setConfidence(createDTO.getConfidence());
        draft.setSourceType(createDTO.getSourceType());
        draft.setSourceId(createDTO.getSourceId());
        draft.setOriginalMessage(createDTO.getOriginalMessage());
        draft.setCreatedBy(currentUser);
        draft.setStatus(DraftStatus.PENDING);
        
        draft = messageDraftRepository.save(draft);
        
        log.info("Draft created with ID: {}", draft.getId());
        return convertToDTO(draft);
    }
    
    /**
     * Aprova um draft e envia como mensagem
     */
    public MessageDTO approveDraft(UUID draftId, DraftReviewDTO reviewDTO) {
        log.info("Approving draft: {}", draftId);
        
        MessageDraft draft = messageDraftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft não encontrado"));
            
        if (!draft.canBeReviewed()) {
            throw new RuntimeException("Draft não pode ser revisado");
        }
        
        User reviewer = companyContextUtil.getAuthenticatedUser();
        String finalContent = draft.getContent();
        
        // Se foi editado, usar novo conteúdo
        if (DraftStatus.EDITED.equals(reviewDTO.getAction()) && reviewDTO.getEditedContent() != null) {
            finalContent = reviewDTO.getEditedContent();
            draft.edit(reviewer, finalContent);
        } else {
            draft.approve(reviewer);
        }
        
        messageDraftRepository.save(draft);
        
        // Enviar mensagem se solicitado
        if (reviewDTO.getSendImmediately()) {
            // TODO: Integrar com MessageService para enviar mensagem real
            log.info("Sending approved draft as message for conversation: {}", draft.getConversation().getId());
            
            // Por enquanto, vamos simular o envio retornando um DTO
            return MessageDTO.builder()
                .id(UUID.randomUUID())
                .conversationId(draft.getConversation().getId())
                .content(finalContent)
                .isFromUser(false) // false = from operator
                .senderType(SenderType.AGENT)
                .createdAt(LocalDateTime.now())
                .build();
        }
        
        return null;
    }
    
    /**
     * Rejeita um draft
     */
    public void rejectDraft(UUID draftId, DraftReviewDTO reviewDTO) {
        log.info("Rejecting draft: {}", draftId);
        
        MessageDraft draft = messageDraftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft não encontrado"));
            
        if (!draft.canBeReviewed()) {
            throw new RuntimeException("Draft não pode ser revisado");
        }
        
        User reviewer = companyContextUtil.getAuthenticatedUser();
        draft.reject(reviewer, reviewDTO.getRejectionReason());
        
        messageDraftRepository.save(draft);
        log.info("Draft rejected: {}", draftId);
    }
    
    /**
     * Lista drafts por conversa
     */
    public List<MessageDraftDTO> getDraftsByConversation(UUID conversationId) {
        return messageDraftRepository.findByConversationIdAndDeletedAtIsNull(conversationId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Lista drafts pendentes por empresa
     */
    public Page<MessageDraftDTO> getPendingDrafts(UUID companyId, Pageable pageable) {
        return messageDraftRepository.findPendingDraftsByCompany(companyId, DraftStatus.PENDING, pageable)
            .map(this::convertToDTO);
    }
    
    /**
     * Obtém estatísticas de drafts
     */
    public DraftStatsDTO getDraftStats(UUID companyId) {
        List<Object[]> stats = messageDraftRepository.getDraftStatsByCompany(companyId);
        
        DraftStatsDTO.DraftStatsDTOBuilder builder = DraftStatsDTO.builder()
            .companyId(companyId);
            
        long total = 0;
        long pending = 0;
        long approved = 0;
        long rejected = 0;
        long edited = 0;
        
        for (Object[] stat : stats) {
            DraftStatus status = (DraftStatus) stat[0];
            Long count = (Long) stat[1];
            
            total += count;
            
            switch (status) {
                case PENDING: pending = count; break;
                case APPROVED: approved = count; break;
                case REJECTED: rejected = count; break;
                case EDITED: edited = count; break;
            }
        }
        
        double approvalRate = total > 0 ? (double) (approved + edited) / total * 100 : 0;
        
        return builder
            .totalDrafts(total)
            .pendingDrafts(pending)
            .approvedDrafts(approved)
            .rejectedDrafts(rejected)
            .editedDrafts(edited)
            .approvalRate(approvalRate)
            .avgConfidence(0.75) // TODO: Calcular confiança média real
            .mostUsedAiModel("rubia-ai-v1")
            .mostUsedSourceType("FAQ")
            .build();
    }
    
    /**
     * Cria uma Message com status DRAFT (seguindo o padrão que o frontend espera)
     */
    private MessageDTO createDraftMessage(Conversation conversation, DraftResponse draftResponse, String originalMessage) {
        // Buscar usuário para criar a mensagem
        User currentUser;
        try {
            currentUser = companyContextUtil.getAuthenticatedUser();
        } catch (IllegalStateException e) {
            log.debug("No authenticated user in async context, using system user for draft creation");
            List<User> companyUsers = userRepository.findByCompanyId(conversation.getCompany().getId());
            currentUser = companyUsers.isEmpty() ? null : companyUsers.get(0);
            
            if (currentUser == null) {
                log.warn("No users found for company {}, creating draft without user", conversation.getCompany().getId());
            }
        }
        
        // Criar Message com status DRAFT
        Message draftMessage = new Message();
        draftMessage.setConversation(conversation);
        draftMessage.setContent(draftResponse.getContent());
        draftMessage.setStatus(MessageStatus.DRAFT);
        draftMessage.setSenderType(SenderType.AI_AGENT);
        draftMessage.setIsAiGenerated(true);
        draftMessage.setAiConfidence(draftResponse.getConfidence());
        
        // Adicionar metadados do draft nos campos extras
        if (draftResponse.getSourceType() != null) {
            // Usar campo customizado ou adicionar na description/content
            // Por enquanto, vamos adicionar como metadado no próprio content
            String metadata = String.format("\n[AI_SOURCE: %s, CONFIDENCE: %.2f, ORIGINAL: %s]", 
                draftResponse.getSourceType(), 
                draftResponse.getConfidence(),
                originalMessage);
            // Não adicionar metadata visível, manter apenas o conteúdo limpo
        }
        
        if (currentUser != null) {
            draftMessage.setSenderId(currentUser.getId());
        }
        
        // Salvar a mensagem DRAFT
        Message savedMessage = messageRepository.save(draftMessage);
        
        log.info("Created DRAFT Message with ID: {} for conversation: {}", savedMessage.getId(), conversation.getId());
        
        // Converter para DTO
        return toDraftMessageDTO(savedMessage, currentUser, draftResponse, originalMessage);
    }
    
    /**
     * Converte Message DRAFT para DTO com metadados específicos
     */
    private MessageDTO toDraftMessageDTO(Message message, User sender, DraftResponse draftResponse, String originalMessage) {
        return MessageDTO.builder()
            .id(message.getId())
            .conversationId(message.getConversation().getId())
            .content(message.getContent())
            .status(message.getStatus())
            .senderType(message.getSenderType())
            .senderId(message.getSenderId())
            .senderName(sender != null ? sender.getName() : "AI System")
            .isAiGenerated(message.getIsAiGenerated())
            .aiConfidence(message.getAiConfidence())
            .createdAt(message.getCreatedAt())
            .build();
    }
    
    // Métodos auxiliares
    
    private boolean shouldGenerateDraft(Conversation conversation, String userMessage) {
        log.debug("🔍 [DEBUG] Checking if should generate draft for conversation: {}", conversation.getId());
        log.debug("🔍 [DEBUG] - Company ID: {}", conversation.getCompany().getId());
        log.debug("🔍 [DEBUG] - Conversation status: '{}'", conversation.getStatus());
        log.debug("🔍 [DEBUG] - User message: '{}' (length: {})", userMessage, userMessage != null ? userMessage.length() : 0);
        
        // Regras para gerar draft:
        // 1. Conversa em status "entrada"
        // 2. Não há draft pendente recente
        // 3. Mensagem não é muito curta
        // 4. Empresa tem IA habilitada (TODO: implementar configuração)
        
        // 1. Verificar status da conversa
        if (conversation.getStatus() != ConversationStatus.ENTRADA) {
            log.debug("❌ [DEBUG] Draft generation skipped: conversation status is '{}', expected 'ENTRADA'", conversation.getStatus());
            return false;
        }
        log.debug("✅ [DEBUG] Conversation status check passed: '{}'", conversation.getStatus());
        
        // 2. Verificar se já existe draft pendente recente (últimos 5 minutos)
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(5);
        List<MessageDraft> recentDrafts = messageDraftRepository.findRecentDrafts(
            conversation.getCompany().getId(), recentTime);
        log.debug("🔍 [DEBUG] Found {} recent drafts in last 5 minutes for company {}", recentDrafts.size(), conversation.getCompany().getId());
            
        boolean hasRecentPendingDraft = recentDrafts.stream()
            .anyMatch(d -> d.getConversation().getId().equals(conversation.getId()) && d.isPending());
        
        if (hasRecentPendingDraft) {
            log.debug("❌ [DEBUG] Draft generation skipped: recent pending draft found for conversation {}", conversation.getId());
            return false;
        }
        log.debug("✅ [DEBUG] No recent pending drafts found for conversation {}", conversation.getId());
        
        // 3. Verificar comprimento da mensagem
        if (userMessage == null || userMessage.trim().length() < 10) {
            log.debug("❌ [DEBUG] Draft generation skipped: message too short ({} chars, minimum 10)", 
                userMessage != null ? userMessage.trim().length() : 0);
            return false;
        }
        log.debug("✅ [DEBUG] Message length check passed: {} chars", userMessage.trim().length());
        
        log.debug("✅ [DEBUG] All checks passed - should generate draft for conversation {}", conversation.getId());
        return true;
    }
    
    private List<FAQMatchDTO> searchRelevantFAQs(UUID companyId, String userMessage) {
        log.debug("🔍 [DEBUG] Searching relevant FAQs for company: {}, message: '{}'", companyId, userMessage);
        try {
            FAQSearchDTO searchDTO = FAQSearchDTO.builder()
                .companyId(companyId)
                .userMessage(userMessage)
                .limit(3)
                .minConfidenceScore(0.5)
                .build();
                
            List<FAQMatchDTO> faqs = faqService.searchRelevantFAQs(searchDTO);
            log.debug("🔍 [DEBUG] Found {} relevant FAQs for message '{}'", faqs.size(), userMessage);
            
            for (int i = 0; i < faqs.size(); i++) {
                FAQMatchDTO faq = faqs.get(i);
                log.debug("🔍 [DEBUG] FAQ {}: '{}' (confidence: {:.2f})", 
                    i + 1, faq.getFaq().getQuestion(), faq.getConfidenceScore());
            }
            
            return faqs;
        } catch (Exception e) {
            log.warn("❌ [DEBUG] Error searching FAQs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private List<MessageTemplate> searchRelevantTemplates(UUID companyId, String userMessage) {
        // TODO: Implementar busca inteligente de templates
        // Por enquanto, retorna lista vazia
        return Collections.emptyList();
    }
    
    private DraftResponse selectBestResponse(List<FAQMatchDTO> faqMatches, List<MessageTemplate> templates, String userMessage, UUID companyId) {
        log.debug("🔍 [DEBUG] Selecting best response from {} FAQs and {} templates", faqMatches.size(), templates.size());
        
        // 1. Se tem FAQs relevantes, usar IA para contextualizar
        if (!faqMatches.isEmpty()) {
            log.debug("🔍 [DEBUG] Attempting to generate AI contextualized response with {} FAQs", faqMatches.size());
            DraftResponse aiResponse = generateContextualizedResponse(faqMatches, userMessage, companyId);
            if (aiResponse != null) {
                log.debug("✅ [DEBUG] Using AI contextualized response (confidence: {:.2f})", aiResponse.getConfidence());
                return aiResponse;
            }
            log.debug("❌ [DEBUG] AI contextualized response failed, falling back to direct FAQ");
        } else {
            log.debug("❌ [DEBUG] No FAQs found for contextualization");
        }
        
        // 2. Fallback: Retornar FAQ com maior confiança sem IA
        Optional<FAQMatchDTO> bestFaq = faqMatches.stream()
            .filter(faq -> faq.getConfidenceScore() >= 0.7)
            .findFirst();
            
        if (bestFaq.isPresent()) {
            FAQMatchDTO faq = bestFaq.get();
            log.debug("✅ [DEBUG] Using direct FAQ response as fallback (confidence: {:.2f}): '{}'", 
                faq.getConfidenceScore(), faq.getFaq().getQuestion());
            return DraftResponse.builder()
                .content(faq.getFaq().getAnswer())
                .confidence(faq.getConfidenceScore())
                .sourceType("FAQ")
                .sourceId(faq.getFaq().getId())
                .build();
        } else {
            if (!faqMatches.isEmpty()) {
                log.debug("❌ [DEBUG] Best FAQ confidence too low: {:.2f} (minimum 0.7)", 
                    faqMatches.get(0).getConfidenceScore());
            }
        }
        
        // 3. TODO: Implementar lógica para templates
        log.debug("❌ [DEBUG] No suitable response found from FAQs or templates");
        
        return null;
    }
    
    /**
     * Gera resposta contextualizada usando IA com FAQs como base de conhecimento
     */
    private DraftResponse generateContextualizedResponse(List<FAQMatchDTO> faqMatches, String userMessage, UUID companyId) {
        log.debug("🔍 [DEBUG] Generating AI contextualized response for company: {}", companyId);
        try {
            // Buscar agente de IA da empresa (usar o primeiro ativo)
            List<AIAgent> agents = aiAgentService.getActiveAIAgentsByCompanyId(companyId);
            log.debug("🔍 [DEBUG] Found {} active AI agents for company {}", agents.size(), companyId);
            
            if (agents.isEmpty()) {
                log.debug("❌ [DEBUG] No AI agent found for company {}, using direct FAQ", companyId);
                return null;
            }
            
            AIAgent agent = agents.get(0);
            log.debug("✅ [DEBUG] Using AI agent '{}' (model: {}, temp: {}) to generate contextual response", 
                agent.getName(), agent.getAiModel().getName(), agent.getTemperature());
            
            // Construir contexto com FAQs relevantes
            StringBuilder context = new StringBuilder();
            context.append("Base de conhecimento disponível:\n\n");
            
            for (int i = 0; i < Math.min(3, faqMatches.size()); i++) {
                FAQMatchDTO match = faqMatches.get(i);
                context.append(String.format("FAQ %d (confiança %.0f%%):\n", 
                    i + 1, match.getConfidenceScore() * 100));
                context.append("P: ").append(match.getFaq().getQuestion()).append("\n");
                context.append("R: ").append(match.getFaq().getAnswer()).append("\n\n");
            }
            
            // Construir prompt contextualizado
            String prompt = String.format(
                """
                Você é %s, um assistente especializado em atendimento ao cliente.
                Temperamento: %s
                
                Sua tarefa é responder à pergunta do cliente de forma natural e personalizada, 
                usando as informações da base de conhecimento como referência.
                
                %s
                
                Pergunta do cliente: "%s"
                
                Instruções:
                - Use as informações da base de conhecimento como fonte principal
                - Personalize a resposta para ser natural e conversacional
                - Mantenha o temperamento %s
                - Limite a resposta a %d caracteres
                - Seja direto e útil
                
                Resposta:
                """,
                agent.getName(),
                agent.getTemperament().toLowerCase(),
                context.toString(),
                userMessage,
                agent.getTemperament().toLowerCase(),
                agent.getMaxResponseLength()
            );
            
            // Chamar IA para gerar resposta
            String aiResponse = openAIService.enhanceTemplate(
                prompt,
                agent.getAiModel().getName(),
                agent.getTemperature().doubleValue(),
                agent.getMaxResponseLength()
            );
            
            if (aiResponse != null && !aiResponse.startsWith("Erro")) {
                // Calcular confiança baseada na melhor FAQ match
                double confidence = faqMatches.get(0).getConfidenceScore();
                // Aumentar ligeiramente a confiança por usar IA contextualizada
                confidence = Math.min(0.95, confidence + 0.1);
                
                log.info("AI generated contextual response with confidence: {:.2f}", confidence);
                
                return DraftResponse.builder()
                    .content(aiResponse)
                    .confidence(confidence)
                    .sourceType("AI_CONTEXTUAL")
                    .sourceId(faqMatches.get(0).getFaq().getId()) // FAQ base usada
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error generating AI contextual response: {}", e.getMessage(), e);
        }
        
        return null; // Fallback para método tradicional
    }
    
    private MessageDraftDTO convertToDTO(MessageDraft draft) {
        return MessageDraftDTO.builder()
            .id(draft.getId())
            .companyId(draft.getCompany().getId())
            .conversationId(draft.getConversation().getId())
            .content(draft.getContent())
            .aiModel(draft.getAiModel())
            .confidence(draft.getConfidence())
            .status(draft.getStatus())
            .sourceType(draft.getSourceType())
            .sourceId(draft.getSourceId())
            .createdById(draft.getCreatedBy() != null ? draft.getCreatedBy().getId() : null)
            .createdByName(draft.getCreatedBy() != null ? draft.getCreatedBy().getName() : null)
            .reviewedById(draft.getReviewedBy() != null ? draft.getReviewedBy().getId() : null)
            .reviewedByName(draft.getReviewedBy() != null ? draft.getReviewedBy().getName() : null)
            .reviewedAt(draft.getReviewedAt())
            .originalMessage(draft.getOriginalMessage())
            .rejectionReason(draft.getRejectionReason())
            .createdAt(draft.getCreatedAt())
            .updatedAt(draft.getUpdatedAt())
            .build();
    }
    
    // Classe auxiliar para resposta do draft
    @lombok.Data
    @lombok.Builder
    private static class DraftResponse {
        private String content;
        private Double confidence;
        private String sourceType;
        private UUID sourceId;
    }
}