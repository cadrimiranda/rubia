package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.*;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
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
    private final UserRepository userRepository;
    private final CompanyContextUtil companyContextUtil;
    
    /**
     * Gera draft automaticamente baseado na mensagem do cliente
     */
    public MessageDraftDTO generateDraftResponse(UUID conversationId, String userMessage) {
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
            DraftResponse bestResponse = selectBestResponse(faqMatches, templates, userMessage);
            
            if (bestResponse == null || bestResponse.getConfidence() < 0.5) {
                log.debug("No suitable response found for conversation: {}", conversationId);
                return null;
            }
            
            // 5. Criar draft
            CreateMessageDraftDTO createDTO = CreateMessageDraftDTO.builder()
                .companyId(conversation.getCompany().getId())
                .conversationId(conversationId)
                .content(bestResponse.getContent())
                .confidence(bestResponse.getConfidence())
                .sourceType(bestResponse.getSourceType())
                .sourceId(bestResponse.getSourceId())
                .aiModel("rubia-ai-v1")
                .originalMessage(userMessage)
                .build();
                
            return createDraft(createDTO);
            
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
            
        User currentUser = companyContextUtil.getAuthenticatedUser();
        
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
                .fromOperator(true)
                .sentAt(LocalDateTime.now())
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
    
    // Métodos auxiliares
    
    private boolean shouldGenerateDraft(Conversation conversation, String userMessage) {
        // Regras para gerar draft:
        // 1. Conversa em status "entrada"
        // 2. Não há draft pendente recente
        // 3. Mensagem não é muito curta
        // 4. Empresa tem IA habilitada (TODO: implementar configuração)
        
        if (!"entrada".equals(conversation.getStatus())) {
            return false;
        }
        
        // Verificar se já existe draft pendente recente (últimos 5 minutos)
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(5);
        List<MessageDraft> recentDrafts = messageDraftRepository.findRecentDrafts(
            conversation.getCompany().getId(), recentTime);
            
        boolean hasRecentPendingDraft = recentDrafts.stream()
            .anyMatch(d -> d.getConversation().getId().equals(conversation.getId()) && d.isPending());
            
        if (hasRecentPendingDraft) {
            return false;
        }
        
        // Mensagem muito curta provavelmente não precisa de draft
        if (userMessage == null || userMessage.trim().length() < 10) {
            return false;
        }
        
        return true;
    }
    
    private List<FAQMatchDTO> searchRelevantFAQs(UUID companyId, String userMessage) {
        try {
            FAQSearchDTO searchDTO = FAQSearchDTO.builder()
                .companyId(companyId)
                .userMessage(userMessage)
                .limit(3)
                .minConfidenceScore(0.5)
                .build();
                
            return faqService.searchRelevantFAQs(searchDTO);
        } catch (Exception e) {
            log.warn("Error searching FAQs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private List<MessageTemplate> searchRelevantTemplates(UUID companyId, String userMessage) {
        // TODO: Implementar busca inteligente de templates
        // Por enquanto, retorna lista vazia
        return Collections.emptyList();
    }
    
    private DraftResponse selectBestResponse(List<FAQMatchDTO> faqMatches, List<MessageTemplate> templates, String userMessage) {
        // Priorizar FAQs com alta confiança
        Optional<FAQMatchDTO> bestFaq = faqMatches.stream()
            .filter(faq -> faq.getConfidenceScore() >= 0.7)
            .findFirst();
            
        if (bestFaq.isPresent()) {
            FAQMatchDTO faq = bestFaq.get();
            return DraftResponse.builder()
                .content(faq.getFaq().getAnswer())
                .confidence(faq.getConfidenceScore())
                .sourceType("FAQ")
                .sourceId(faq.getFaq().getId())
                .build();
        }
        
        // TODO: Implementar lógica para templates
        
        return null;
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