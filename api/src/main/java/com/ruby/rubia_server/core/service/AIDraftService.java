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
import com.ruby.rubia_server.core.service.MessagingService;
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
    private final MessagingService messagingService;
    private final CustomerService customerService;
    private final MessageService messageService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final ConversationService conversationService;
    
    /**
     * Gera draft automaticamente baseado na mensagem do cliente
     */
    public MessageDTO generateDraftResponse(UUID conversationId, String userMessage) {
        // Normalizar mensagem para melhor processamento
        String normalizedMessage = normalizeUserMessage(userMessage);
        log.info("Generating draft response for conversation: {} with message: '{}'", conversationId, normalizedMessage);
        
        try {
            // 1. Verificar se deve gerar draft
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversa não encontrada"));
                
            if (!shouldGenerateDraft(conversation, normalizedMessage)) {
                log.debug("Skipping draft generation for conversation: {}", conversationId);
                return null;
            }
            
            // 2. Gerar resposta do hemocentro usando AIAgent da empresa
            DraftResponse bestResponse = generateBloodCenterResponse(conversation.getCompany().getId(), normalizedMessage);
            
            if (bestResponse == null) {
                log.debug("❌ [DEBUG] No response selected for conversation: {}", conversationId);
                return null;
            }
            
            log.info("✅ Selected response with confidence {:.2f} for conversation: {}", 
                bestResponse.getConfidence(), conversationId);
                
            // Enviar resposta automaticamente para hemocentro
            MessageDTO result = sendBloodCenterResponse(conversation, bestResponse, normalizedMessage);
            if (result != null) {
                log.info("✅ [SUCCESS] Blood center message sent automatically with ID: {} for conversation: {}", 
                    result.getId(), conversationId);
            }
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
        
        // 1. Verificar status da conversa
        if (conversation.getStatus() != ConversationStatus.ENTRADA) {
            log.debug("❌ [DEBUG] Draft generation skipped: conversation status is '{}', expected 'ENTRADA'", conversation.getStatus());
            return false;
        }
        log.debug("✅ [DEBUG] Conversation status check passed: '{}'", conversation.getStatus());
        
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
    
    /**
     * Gera resposta especializada do hemocentro usando AIAgent da empresa
     */
    private DraftResponse generateBloodCenterResponse(UUID companyId, String userMessage) {
        log.debug("🩸 Generating blood center response for company: {}", companyId);
        try {
            // Buscar agente de IA da empresa (similar ao TemplateEnhancementService)
            List<AIAgent> activeAgents = aiAgentService.getActiveAIAgentsByCompanyId(companyId);
            log.debug("🔍 Found {} active AI agents for company {}", activeAgents.size(), companyId);
            
            AIAgent agent;
            String modelSource;
            boolean isUsingCompanyAgent = false;
            
            if (!activeAgents.isEmpty()) {
                // Cenário ideal: empresa tem agente configurado
                agent = activeAgents.get(0);
                modelSource = "agente da empresa";
                isUsingCompanyAgent = true;
                log.info("Using company's configured AI agent: {} for blood center response", agent.getName());
            } else {
                log.debug("❌ No AI agent found for company {}, skipping blood center response", companyId);
                return null; // Empresa precisa ter agente configurado para hemocentro
            }
            
            // Construir prompt especializado do hemocentro
            String prompt = String.format(
                """
                Você é %s, assistente especializada de um hemocentro.
                Temperamento: %s
                
                Responda apenas dúvidas sobre doação de sangue, com tom descontraído, acolhedor e encorajador.
                
                Contexto rápido:
                - Explique critérios de elegibilidade (idade, peso, saúde, tempo entre doações).
                - Oriente pré e pós-doação (hidratação, alimentação, descanso).
                - Esclareça medos comuns e benefícios para a sociedade.
                - Se a pergunta fugir do tema doação de sangue/hemocentro, avise gentilmente que só responde sobre isso.
                
                Use linguagem clara, direta e humana. Sempre seja positiva e encorajadora sobre a doação de sangue.
                Mantenha as respostas concisas mas informativas.
                Limite a resposta a %d caracteres.
                
                Pergunta do cliente: "%s"
                
                Resposta especializada:
                """,
                agent.getName(),
                agent.getTemperament().toLowerCase(),
                agent.getMaxResponseLength(),
                userMessage
            );
            
            // Chamar IA para gerar resposta usando configurações do agente
            String aiResponse = openAIService.enhanceTemplate(
                prompt,
                agent.getAiModel().getName(),
                agent.getTemperature().doubleValue(),
                agent.getMaxResponseLength()
            );
            
            if (aiResponse != null && !aiResponse.startsWith("Erro")) {
                // Calcular confiança alta para resposta especializada
                double confidence = 0.85; // Confiança alta para agente especializado
                
                log.info("🩸 Blood center AI generated response with confidence: {}", aiResponse);
                
                return DraftResponse.builder()
                    .content(aiResponse)
                    .confidence(confidence)
                    .sourceType("BLOOD_CENTER_AI")
                    .sourceId(agent.getId())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error generating blood center AI response: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Envia resposta do hemocentro automaticamente via WhatsApp
     */
    private MessageDTO sendBloodCenterResponse(Conversation conversation, DraftResponse draftResponse, String originalMessage) {
        try {
            log.info("🩸 Sending blood center response automatically for conversation: {}", conversation.getId());
            
            // Buscar usuário para criar a mensagem
            User currentUser;
            try {
                currentUser = companyContextUtil.getAuthenticatedUser();
            } catch (IllegalStateException e) {
                log.debug("No authenticated user in async context, using system user for automatic send");
                List<User> companyUsers = userRepository.findByCompanyId(conversation.getCompany().getId());
                currentUser = companyUsers.isEmpty() ? null : companyUsers.get(0);
                
                if (currentUser == null) {
                    log.warn("No users found for company {}, cannot send automatic message", conversation.getCompany().getId());
                    return null;
                }
            }
            
            // Buscar customer através dos participantes da conversa
            Customer customer = null;
            for (ConversationParticipant participant : conversation.getParticipants()) {
                if (participant.getCustomer() != null) {
                    customer = participant.getCustomer();
                    break;
                }
            }
            
            if (customer == null || customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
                log.warn("Customer phone not found for conversation: {}, cannot send automatic message", conversation.getId());
                return null;
            }
            
            // Criar mensagem no banco primeiro
            CreateMessageDTO createDTO = CreateMessageDTO.builder()
                .conversationId(conversation.getId())
                .companyId(conversation.getCompany().getId())
                .content(draftResponse.getContent())
                .senderType(SenderType.AI_AGENT)
                .senderId(currentUser.getId())
                .build();
            
            MessageDTO message = messageService.create(createDTO);
            log.info("Message created in database with ID: {}", message.getId());
            
            // Enviar via WhatsApp/Z-API (passando Company para evitar problema de contexto)
            MessageResult result = messagingService.sendMessage(
                customer.getPhone(),
                draftResponse.getContent(),
                conversation.getCompany()
            );
            
            if (result.isSuccess()) {
                // Atualizar mensagem com ID externo e status SENT
                UpdateMessageDTO updateDTO = UpdateMessageDTO.builder()
                    .status(MessageStatus.SENT)
                    .externalMessageId(result.getMessageId())
                    .build();
                
                MessageDTO updatedMessage = messageService.update(message.getId(), updateDTO);
                log.info("🩸 Blood center message sent successfully via WhatsApp. External ID: {}", result.getMessageId());
                
                // Notificar frontend via WebSocket
                try {
                    ConversationDTO conversationDTO = conversationService.findById(conversation.getId(), conversation.getCompany().getId());
                    webSocketNotificationService.notifyNewMessage(updatedMessage, conversationDTO);
                    log.debug("WebSocket notification sent for blood center message: {}", updatedMessage.getId());
                } catch (Exception e) {
                    log.warn("Failed to send WebSocket notification for blood center message: {}", e.getMessage());
                }
                
                return updatedMessage;
            } else {
                // Marcar mensagem como falhou
                UpdateMessageDTO failedUpdateDTO = UpdateMessageDTO.builder()
                    .status(MessageStatus.FAILED)
                    .build();
                
                messageService.update(message.getId(), failedUpdateDTO);
                log.error("Failed to send blood center message via WhatsApp: {}", result.getError());
                
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error sending blood center response automatically: {}", e.getMessage(), e);
            return null;
        }
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
    
    /**
     * Normaliza mensagem do usuário para melhor processamento pela IA
     * - Remove quebras de linha desnecessárias 
     * - Preserva quebras importantes para listas e pontos
     * - Remove espaços extras
     * - Limpa formatação do WhatsApp
     */
    private String normalizeUserMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return userMessage;
        }

        String normalized = userMessage
            // Remove espaços no início e fim
            .trim()
            // Converte múltiplas quebras de linha em uma única
            .replaceAll("\\n{3,}", "\n\n")
            // Remove espaços antes e depois de quebras de linha
            .replaceAll("\\s*\\n\\s*", "\n")
            // Remove múltiplos espaços em branco
            .replaceAll("\\s{2,}", " ")
            // Remove formatação do WhatsApp (negrito, itálico)
            .replaceAll("\\*([^*]+)\\*", "$1")  // *texto* -> texto
            .replaceAll("_([^_]+)_", "$1")      // _texto_ -> texto
            .replaceAll("~([^~]+)~", "$1")      // ~texto~ -> texto
            // Preserva quebras importantes (listas, pontos)
            .replaceAll("\\n([0-9]+\\.|[•\\-\\*])\\s*", "\n$1 ")
            // Remove quebras de linha simples que não são listas (junta frases)
            .replaceAll("(?<!\\n)\\n(?![0-9]+\\.|[•\\-\\*\\n])", " ")
            // Limpa espaços extras novamente
            .replaceAll("\\s{2,}", " ")
            .trim();

        log.debug("Normalized message: '{}' -> '{}'", userMessage, normalized);
        return normalized;
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