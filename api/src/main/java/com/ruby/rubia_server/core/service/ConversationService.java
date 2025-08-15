package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.ConversationSummaryDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.ConversationParticipant;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.ConversationParticipantRepository;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.repository.CampaignRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.dto.MessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final CampaignRepository campaignRepository;
    private final MessageRepository messageRepository;
    private final AIAgentService aiAgentService;
    
    @Autowired
    private UnreadMessageCountService unreadCountService;
    
    public Optional<ConversationDTO> findByCustomerIdAndCampaignId(UUID customerId, UUID campaignId) {
        log.debug("Finding conversation by customer id: {} and campaign id: {}", customerId, campaignId);
        
        // Buscar conversas do customer que tenham a campanha específica
        List<Conversation> conversations = conversationRepository.findAll().stream()
            .filter(conv -> conv.getCampaign() != null && conv.getCampaign().getId().equals(campaignId))
            .filter(conv -> conv.getParticipants().stream()
                .anyMatch(p -> p.getCustomer() != null && p.getCustomer().getId().equals(customerId)))
            .toList();
        
        if (conversations.isEmpty()) {
            return Optional.empty();
        }
        
        // Retornar a primeira conversa encontrada
        Conversation conversation = conversations.get(0);
        return Optional.of(toDTO(conversation));
    }

    public ConversationDTO create(CreateConversationDTO createDTO, UUID companyId) {
        log.info("Creating conversation for customer: {} in company: {}", createDTO.getCustomerId(), companyId);
        
        Customer customer = customerRepository.findById(createDTO.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        // Validate customer belongs to company
        if (!customer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cliente não pertence a esta empresa");
        }
        
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
        
        User assignedUser = null;
        if (createDTO.getAssignedUserId() != null) {
            assignedUser = userRepository.findById(createDTO.getAssignedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            // Validate user belongs to company
            if (!assignedUser.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("Usuário não pertence a esta empresa");
            }
        }
        
        Department department = null;
        if (createDTO.getDepartmentId() != null) {
            department = departmentRepository.findById(createDTO.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
            // Validate department belongs to company
            if (!department.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("Departamento não pertence a esta empresa");
            }
        }
        
        Campaign campaign = null;
        if (createDTO.getCampaignId() != null) {
            campaign = campaignRepository.findById(createDTO.getCampaignId())
                    .orElseThrow(() -> new IllegalArgumentException("Campanha não encontrada"));
            // Validate campaign belongs to company
            if (!campaign.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("Campanha não pertence a esta empresa");
            }
        }
        
        Conversation conversation = Conversation.builder()
                .assignedUser(assignedUser)
                .company(company)
                .status(createDTO.getStatus())
                .channel(createDTO.getChannel())
                .priority(createDTO.getPriority())
                .campaign(campaign)
                .conversationType(createDTO.getConversationType())
                .chatLid(createDTO.getChatLid())
                .build();
        
        Conversation saved = conversationRepository.save(conversation);
        log.info("Conversation created successfully with id: {}", saved.getId());
        
        // Criar ConversationParticipant para o customer
        ConversationParticipant customerParticipant = ConversationParticipant.builder()
                .conversation(saved)
                .customer(customer)
                .company(company)
                .isActive(true)
                .build();
        
        participantRepository.save(customerParticipant);
        log.info("Customer participant created for conversation: {}", saved.getId());
        
        // Alternative approach: manually build the DTO with the known customer
        return ConversationDTO.builder()
                .id(saved.getId())
                .companyId(saved.getCompany() != null ? saved.getCompany().getId() : null)
                .customerId(customer.getId())
                .customerName(customer.getName())
                .customerPhone(customer.getPhone())
                .assignedUserId(saved.getAssignedUser() != null ? saved.getAssignedUser().getId() : null)
                .assignedUserName(saved.getAssignedUser() != null ? saved.getAssignedUser().getName() : null)
                .campaignId(saved.getCampaign() != null ? saved.getCampaign().getId() : null)
                .campaignName(saved.getCampaign() != null ? saved.getCampaign().getName() : null)
                .status(saved.getStatus())
                .channel(saved.getChannel())
                .priority(saved.getPriority())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .unreadCount(0L)
                .chatLid(saved.getChatLid())
                .aiAutoResponseEnabled(saved.getAiAutoResponseEnabled())
                .aiMessageLimit(aiAgentService.getAiMessageLimitForCompany(companyId))
                .aiMessagesUsed(saved.getAiMessagesUsed())
                .aiLimitReachedAt(saved.getAiLimitReachedAt())
                .build();
    }
    
    @Transactional(readOnly = true)
    public ConversationDTO findById(UUID id, UUID companyId) {
        log.debug("Finding conversation by id: {} for company: {}", id, companyId);
        
        List<Object[]> results = conversationRepository.findConversationWithCustomer(id);
        
        if (results.isEmpty()) {
            // Fallback to regular query
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
            
            // Validate conversation belongs to company
            if (!conversation.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("Conversa não pertence a esta empresa");
            }
            
            return toDTO(conversation);
        }
        
        Object[] result = results.get(0);
        Conversation conversation = (Conversation) result[0];
        Customer customer = (Customer) result[1];
        
        // Validate conversation belongs to company
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversa não pertence a esta empresa");
        }
        
        return toDTO(conversation);
    }
    
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> findSummariesByStatus(ConversationStatus status) {
        log.debug("Finding conversation summaries by status: {}", status);
        
        // This method should not exist in a multi-tenant system
        throw new UnsupportedOperationException("Use findSummariesByStatusAndCompany instead");
    }
    
    public ConversationDTO assignToUser(UUID conversationId, UUID userId, UUID companyId) {
        log.info("Assigning conversation {} to user {} in company {}", conversationId, userId, companyId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        // Validate conversation belongs to company
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversa não pertence a esta empresa");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        // Validate user belongs to company
        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Usuário não pertence a esta empresa");
        }
        
        conversation.setAssignedUser(user);
        conversation.setStatus(ConversationStatus.ESPERANDO);
        
        Conversation updated = conversationRepository.save(conversation);
        log.info("Conversation assigned successfully");
        
        return toDTO(updated);
    }
    
    public ConversationDTO changeStatus(UUID conversationId, ConversationStatus newStatus, UUID companyId) {
        log.info("Changing conversation {} status to {} in company {}", conversationId, newStatus, companyId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        // Validate conversation belongs to company
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversa não pertence a esta empresa");
        }
        
        conversation.setStatus(newStatus);
        
        Conversation updated = conversationRepository.save(conversation);
        log.info("Conversation status changed successfully");
        
        return toDTO(updated);
    }
    
    public ConversationDTO update(UUID id, UpdateConversationDTO updateDTO, UUID companyId) {
        log.info("Updating conversation with id: {} for company: {}", id, companyId);
        
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        // Validate conversation belongs to company
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversa não pertence a esta empresa");
        }
        
        if (updateDTO.getAssignedUserId() != null) {
            User user = userRepository.findById(updateDTO.getAssignedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            // Validate user belongs to company
            if (!user.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("Usuário não pertence a esta empresa");
            }
            conversation.setAssignedUser(user);
        }
        
        if (updateDTO.getStatus() != null) {
            conversation.setStatus(updateDTO.getStatus());
        }
        
        Conversation updated = conversationRepository.save(conversation);
        log.info("Conversation updated successfully");
        
        return toDTO(updated);
    }
    
    public void delete(UUID id, UUID companyId) {
        log.info("Deleting conversation with id: {} for company: {}", id, companyId);
        
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        // Validate conversation belongs to company
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversa não pertence a esta empresa");
        }
        
        conversationRepository.deleteById(id);
        log.info("Conversation deleted successfully");
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findByStatusAndCompany(ConversationStatus status, UUID companyId) {
        log.debug("Finding conversations by status: {} for company: {}", status, companyId);
        
        return conversationRepository.findByStatusAndCompanyOrderedByPriorityAndUpdatedAt(status, companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Page<ConversationDTO> findByStatusAndCompanyWithPagination(ConversationStatus status, UUID companyId, Pageable pageable) {
        return findByStatusAndCompanyWithPagination(status, companyId, pageable, null);
    }
    
    @Transactional(readOnly = true)
    public Page<ConversationDTO> findByStatusAndCompanyWithPagination(ConversationStatus status, UUID companyId, Pageable pageable, UUID userId) {
        log.debug("Finding conversations by status: {} for company: {} with pagination (userId: {})", status, companyId, userId);
        
        Page<Conversation> conversationPage = conversationRepository.findByStatusAndCompanyOrderedByPriorityAndUpdatedAt(status, companyId, pageable);
        
        // Como a query já faz LEFT JOIN FETCH, podemos mapear diretamente
        return conversationPage.map(conversation -> toDTO(conversation, userId));
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findConversationsOrderByLastMessageDate(UUID companyId) {
        log.debug("Finding conversations ordered by last message date for company: {}", companyId);
        
        return conversationRepository.findConversationsOrderByLastMessageDateOptimized(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Page<ConversationDTO> findConversationsOrderByLastMessageDateWithPagination(UUID companyId, Pageable pageable) {
        log.debug("Finding conversations ordered by last message date for company: {} with pagination", companyId);
        
        Page<Conversation> conversationPage = conversationRepository.findConversationsOrderByLastMessageDateOptimized(companyId, pageable);
        
        return conversationPage.map(this::toDTO);
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findConversationsOrderByLastMessageDateByStatus(UUID companyId, ConversationStatus status) {
        log.debug("Finding conversations ordered by last message date for company: {} with status: {}", companyId, status);
        
        return conversationRepository.findConversationsOrderByLastMessageDateOptimizedByStatus(companyId, status)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Page<ConversationDTO> findConversationsOrderByLastMessageDateByStatusWithPagination(UUID companyId, ConversationStatus status, Pageable pageable) {
        log.debug("Finding conversations ordered by last message date for company: {} with status: {} with pagination", companyId, status);
        
        Page<Conversation> conversationPage = conversationRepository.findConversationsOrderByLastMessageDateOptimizedByStatus(companyId, status, pageable);
        
        return conversationPage.map(this::toDTO);
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findByCustomerAndCompany(UUID customerId, UUID companyId) {
        log.debug("Finding conversations by customer: {} for company: {}", customerId, companyId);
        
        return conversationRepository.findByCustomerIdAndCompanyId(customerId, companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findByAssignedUserAndCompany(UUID userId, UUID companyId) {
        log.debug("Finding conversations by assigned user: {} for company: {}", userId, companyId);
        
        return conversationRepository.findByAssignedUserIdAndCompanyId(userId, companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findUnassignedByCompany(UUID companyId) {
        log.debug("Finding unassigned conversations for company: {}", companyId);
        
        return conversationRepository.findUnassignedEntranceConversationsByCompany(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public long countByStatusAndCompany(ConversationStatus status, UUID companyId) {
        return conversationRepository.countByStatusAndCompany(status, companyId);
    }
    
    public void deleteAllByCompany(UUID companyId) {
        log.info("Deleting all conversations for company: {}", companyId);
        
        List<Conversation> conversations = conversationRepository.findByCompanyId(companyId);
        conversationRepository.deleteAll(conversations);
        
        log.info("Deleted {} conversations for company: {}", conversations.size(), companyId);
    }

    private ConversationDTO toDTO(Conversation conversation) {
        return toDTO(conversation, null);
    }
    
    private ConversationDTO toDTO(Conversation conversation, UUID userId) {
        // Proteção defensiva para participants null
        Customer customer = null;
        if (conversation.getParticipants() != null) {
            customer = conversation.getParticipants().stream()
                    .filter(p -> p.getCustomer() != null)
                    .map(p -> p.getCustomer())
                    .findFirst()
                    .orElse(null);
        }
        
        // Buscar a última mensagem da conversa
        MessageDTO lastMessage = null;
        try {
            Optional<Message> lastMessageEntity = messageRepository.findLastMessageByConversation(conversation.getId());
            if (lastMessageEntity.isPresent()) {
                Message msg = lastMessageEntity.get();
                // Determinar messageType e mediaUrl baseado na mídia associada
                String messageType = "TEXT"; // Padrão
                String mediaUrl = null;
                
                if (msg.getMedia() != null) {
                    messageType = msg.getMedia().getMediaType().name();
                    mediaUrl = msg.getMedia().getFileUrl();
                }
                
                lastMessage = MessageDTO.builder()
                        .id(msg.getId())
                        .conversationId(msg.getConversation().getId())
                        .content(msg.getContent())
                        .senderType(msg.getSenderType())
                        .senderId(msg.getSenderId())
                        .messageType(messageType)
                        .mediaUrl(mediaUrl)
                        .externalMessageId(msg.getExternalMessageId())
                        .isAiGenerated(msg.getIsAiGenerated())
                        .aiConfidence(msg.getAiConfidence())
                        .status(msg.getStatus())
                        .createdAt(msg.getCreatedAt())
                        .deliveredAt(msg.getDeliveredAt())
                        .readAt(msg.getReadAt())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Error fetching last message for conversation {}: {}", conversation.getId(), e.getMessage());
        }

        return ConversationDTO.builder()
                .id(conversation.getId())
                .companyId(conversation.getCompany() != null ? conversation.getCompany().getId() : null)
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getName() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .customerBirthDate(customer != null ? customer.getBirthDate() : null)
                .customerBloodType(customer != null ? customer.getBloodType() : null)
                .customerLastDonationDate(customer != null ? customer.getLastDonationDate() : null)
                .customerHeight(customer != null ? customer.getHeight() : null)
                .customerWeight(customer != null ? customer.getWeight() : null)
                .assignedUserId(conversation.getAssignedUser() != null ? conversation.getAssignedUser().getId() : null)
                .assignedUserName(conversation.getAssignedUser() != null ? conversation.getAssignedUser().getName() : null)
                .campaignId(conversation.getCampaign() != null ? conversation.getCampaign().getId() : null)
                .campaignName(conversation.getCampaign() != null ? conversation.getCampaign().getName() : null)
                .status(conversation.getStatus())
                .channel(conversation.getChannel())
                .priority(conversation.getPriority())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessage(lastMessage)
                .unreadCount(userId != null ? 
                    Optional.ofNullable(unreadCountService.getUnreadCount(userId, conversation.getId()))
                        .map(Integer::longValue).orElse(0L) : 0L)
                .chatLid(conversation.getChatLid())
                .aiAutoResponseEnabled(conversation.getAiAutoResponseEnabled())
                .aiMessageLimit(aiAgentService.getAiMessageLimitForCompany(conversation.getCompany().getId()))
                .aiMessagesUsed(conversation.getAiMessagesUsed())
                .aiLimitReachedAt(conversation.getAiLimitReachedAt())
                .build();
    }
    
    private ConversationSummaryDTO toSummaryDTO(Conversation conversation) {
        Customer customer = conversation.getParticipants().stream()
                .filter(p -> p.getCustomer() != null)
                .map(p -> p.getCustomer())
                .findFirst()
                .orElse(null);

        return ConversationSummaryDTO.builder()
                .id(conversation.getId())
                .customerName(customer != null ? customer.getName() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .assignedUserName(conversation.getAssignedUser() != null ? conversation.getAssignedUser().getName() : null)
                .status(conversation.getStatus())
                .channel(conversation.getChannel())
                .priority(conversation.getPriority())
                .updatedAt(conversation.getUpdatedAt())
                .unreadCount(0L) // Summary doesn't include user-specific unread count
                .lastMessageContent(null) // Will be populated by message service
                .lastMessageTime(null) // Will be populated by message service
                .chatLid(conversation.getChatLid())
                .build();
    }

    /**
     * Find conversation by Z-API chatLid
     */
    public Optional<ConversationDTO> findByChatLid(String chatLid) {
        if (chatLid == null || chatLid.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return conversationRepository.findByChatLid(chatLid)
            .map(this::toDTO);
    }

    /**
     * Update conversation with chatLid
     */
    public void updateChatLid(UUID conversationId, String chatLid) {
        log.info("Updating conversation {} with chatLid: {}", conversationId, chatLid);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        
        conversation.setChatLid(chatLid);
        conversationRepository.save(conversation);
    }


    /**
     * Increment AI message usage counter and disable AI if limit reached
     */
    public void incrementAiMessageUsage(UUID conversationId, UUID companyId) {
        log.info("Incrementing AI message usage for conversation: {}", conversationId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        
        // Verificar se a conversa pertence à empresa do usuário
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversation does not belong to user's company");
        }
        
        // Buscar limite do AIAgent da empresa
        Integer aiMessageLimit = aiAgentService.getAiMessageLimitForCompany(companyId);
        
        // Incrementar contador
        conversation.setAiMessagesUsed(conversation.getAiMessagesUsed() + 1);
        
        // Verificar se atingiu o limite
        if (conversation.getAiMessagesUsed() >= aiMessageLimit) {
            conversation.setAiAutoResponseEnabled(false);
            conversation.setAiLimitReachedAt(LocalDateTime.now());
            log.info("AI auto-response disabled for conversation {} - limit reached ({}/{})", 
                    conversationId, conversation.getAiMessagesUsed(), aiMessageLimit);
        }
        
        conversationRepository.save(conversation);
        
        log.debug("AI message usage updated: {}/{} for conversation: {}", 
                conversation.getAiMessagesUsed(), aiMessageLimit, conversationId);
    }

    /**
     * Reset AI message limit counter and re-enable AI auto-response
     */
    public ConversationDTO resetAiMessageLimit(UUID conversationId, UUID companyId) {
        log.info("Resetting AI message limit for conversation: {}", conversationId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        
        // Verificar se a conversa pertence à empresa do usuário
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversation does not belong to user's company");
        }
        
        // Resetar contador e reativar AI
        conversation.setAiMessagesUsed(0);
        conversation.setAiLimitReachedAt(null);
        conversation.setAiAutoResponseEnabled(true);
        
        conversation = conversationRepository.save(conversation);
        
        log.info("AI message limit reset and auto-response re-enabled for conversation: {}", conversationId);
        
        return toDTO(conversation);
    }


    /**
     * Toggle AI auto-response for a specific conversation
     */
    public ConversationDTO toggleAiAutoResponse(UUID conversationId, Boolean enabled, UUID companyId) {
        log.info("Toggling AI auto-response for conversation {} to: {}", conversationId, enabled);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        
        // Verificar se a conversa pertence à empresa do usuário
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversation does not belong to user's company");
        }
        
        // Se está tentando ativar AI, verificar se não atingiu o limite
        if (enabled) {
            Integer aiMessageLimit = aiAgentService.getAiMessageLimitForCompany(companyId);
            if (conversation.getAiMessagesUsed() >= aiMessageLimit) {
                throw new IllegalArgumentException(
                    String.format("Cannot enable AI auto-response: message limit reached (%d/%d). Reset the limit first.", 
                        conversation.getAiMessagesUsed(), aiMessageLimit));
            }
        }
        
        conversation.setAiAutoResponseEnabled(enabled);
        
        // Se está desativando manualmente, limpar timestamp de limite atingido
        if (!enabled && conversation.getAiLimitReachedAt() != null) {
            // Só limpar se não atingiu o limite por contagem
            Integer aiMessageLimit = aiAgentService.getAiMessageLimitForCompany(companyId);
            if (conversation.getAiMessagesUsed() < aiMessageLimit) {
                conversation.setAiLimitReachedAt(null);
            }
        }
        
        conversation = conversationRepository.save(conversation);
        
        log.info("AI auto-response {} for conversation: {}", 
                enabled ? "enabled" : "disabled", conversationId);
        
        return toDTO(conversation);
    }
}