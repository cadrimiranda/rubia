package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateConversationParticipantDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationParticipantDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ConversationParticipantService extends BaseCompanyEntityService<ConversationParticipant, CreateConversationParticipantDTO, UpdateConversationParticipantDTO> {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AIAgentRepository aiAgentRepository;

    public ConversationParticipantService(ConversationParticipantRepository conversationParticipantRepository,
                                         CompanyRepository companyRepository,
                                         ConversationRepository conversationRepository,
                                         CustomerRepository customerRepository,
                                         UserRepository userRepository,
                                         AIAgentRepository aiAgentRepository,
                                         EntityRelationshipValidator relationshipValidator) {
        super(conversationParticipantRepository, companyRepository, relationshipValidator);
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.conversationRepository = conversationRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    protected String getEntityName() {
        return "ConversationParticipant";
    }

    @Override
    protected ConversationParticipant buildEntityFromDTO(CreateConversationParticipantDTO createDTO) {
        // Validate business rule: exactly one participant type must be provided
        int participantCount = 0;
        if (createDTO.getCustomerId() != null) participantCount++;
        if (createDTO.getUserId() != null) participantCount++;
        if (createDTO.getAiAgentId() != null) participantCount++;
        
        if (participantCount != 1) {
            throw new RuntimeException("Exactly one participant (customer, user, or AI agent) must be provided");
        }

        ConversationParticipant.ConversationParticipantBuilder builder = ConversationParticipant.builder()
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true);

        // Validate and set required conversation
        Conversation conversation = conversationRepository.findById(createDTO.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + createDTO.getConversationId()));
        builder.conversation(conversation);

        // Handle participant relationships (exactly one should be set)
        if (createDTO.getCustomerId() != null) {
            Customer customer = customerRepository.findById(createDTO.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + createDTO.getCustomerId()));
            builder.customer(customer);
        }

        if (createDTO.getUserId() != null) {
            User user = userRepository.findById(createDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + createDTO.getUserId()));
            builder.user(user);
        }

        if (createDTO.getAiAgentId() != null) {
            AIAgent aiAgent = aiAgentRepository.findById(createDTO.getAiAgentId())
                    .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + createDTO.getAiAgentId()));
            builder.aiAgent(aiAgent);
        }

        return builder.build();
    }

    @Override
    protected void updateEntityFromDTO(ConversationParticipant conversationParticipant, UpdateConversationParticipantDTO updateDTO) {
        if (updateDTO.getIsActive() != null) {
            conversationParticipant.setIsActive(updateDTO.getIsActive());
            
            // If marking as inactive, set leftAt timestamp
            if (!updateDTO.getIsActive()) {
                conversationParticipant.setLeftAt(updateDTO.getLeftAt() != null ? updateDTO.getLeftAt() : LocalDateTime.now());
            }
            // If marking as active again, clear leftAt timestamp
            else {
                conversationParticipant.setLeftAt(null);
            }
        }
        
        if (updateDTO.getLeftAt() != null) {
            conversationParticipant.setLeftAt(updateDTO.getLeftAt());
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateConversationParticipantDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade
    @Transactional(readOnly = true)
    public List<ConversationParticipant> findByConversationId(UUID conversationId) {
        log.debug("Finding ConversationParticipants by conversation id: {}", conversationId);
        return conversationParticipantRepository.findByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findActiveByConversationId(UUID conversationId) {
        log.debug("Finding active ConversationParticipants by conversation id: {}", conversationId);
        return conversationParticipantRepository.findByConversationIdAndIsActive(conversationId, true);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findByCustomerId(UUID customerId) {
        log.debug("Finding ConversationParticipants by customer id: {}", customerId);
        return conversationParticipantRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findByUserId(UUID userId) {
        log.debug("Finding ConversationParticipants by user id: {}", userId);
        return conversationParticipantRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findByAiAgentId(UUID aiAgentId) {
        log.debug("Finding ConversationParticipants by AI agent id: {}", aiAgentId);
        return conversationParticipantRepository.findByAiAgentId(aiAgentId);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findActiveByCustomerId(UUID customerId) {
        log.debug("Finding active ConversationParticipants by customer id: {}", customerId);
        return conversationParticipantRepository.findByCustomerIdAndIsActive(customerId, true);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findActiveByUserId(UUID userId) {
        log.debug("Finding active ConversationParticipants by user id: {}", userId);
        return conversationParticipantRepository.findByUserIdAndIsActive(userId, true);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findActiveByAiAgentId(UUID aiAgentId) {
        log.debug("Finding active ConversationParticipants by AI agent id: {}", aiAgentId);
        return conversationParticipantRepository.findByAiAgentIdAndIsActive(aiAgentId, true);
    }

    @Transactional(readOnly = true)
    public long countByConversationId(UUID conversationId) {
        log.debug("Counting ConversationParticipants by conversation id: {}", conversationId);
        return conversationParticipantRepository.countByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public long countActiveByConversationId(UUID conversationId) {
        log.debug("Counting active ConversationParticipants by conversation id: {}", conversationId);
        return conversationParticipantRepository.countByConversationIdAndIsActive(conversationId, true);
    }

    @Transactional(readOnly = true)
    public boolean existsByConversationIdAndCustomerId(UUID conversationId, UUID customerId) {
        log.debug("Checking if ConversationParticipant exists by conversation id: {} and customer id: {}", conversationId, customerId);
        return conversationParticipantRepository.existsByConversationIdAndCustomerId(conversationId, customerId);
    }

    @Transactional(readOnly = true)
    public boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId) {
        log.debug("Checking if ConversationParticipant exists by conversation id: {} and user id: {}", conversationId, userId);
        return conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    @Transactional(readOnly = true)
    public boolean existsByConversationIdAndAiAgentId(UUID conversationId, UUID aiAgentId) {
        log.debug("Checking if ConversationParticipant exists by conversation id: {} and AI agent id: {}", conversationId, aiAgentId);
        return conversationParticipantRepository.existsByConversationIdAndAiAgentId(conversationId, aiAgentId);
    }

    @Transactional
    public Optional<ConversationParticipant> leaveConversation(UUID participantId) {
        log.debug("Marking ConversationParticipant as left with id: {}", participantId);

        Optional<ConversationParticipant> optionalParticipant = conversationParticipantRepository.findById(participantId);
        if (optionalParticipant.isEmpty()) {
            log.warn("ConversationParticipant not found with id: {}", participantId);
            return Optional.empty();
        }

        ConversationParticipant participant = optionalParticipant.get();
        participant.setIsActive(false);
        participant.setLeftAt(LocalDateTime.now());

        ConversationParticipant updatedParticipant = conversationParticipantRepository.save(participant);
        log.debug("ConversationParticipant marked as left successfully with id: {}", updatedParticipant.getId());

        return Optional.of(updatedParticipant);
    }

    @Transactional
    public Optional<ConversationParticipant> rejoinConversation(UUID participantId) {
        log.debug("Marking ConversationParticipant as rejoined with id: {}", participantId);

        Optional<ConversationParticipant> optionalParticipant = conversationParticipantRepository.findById(participantId);
        if (optionalParticipant.isEmpty()) {
            log.warn("ConversationParticipant not found with id: {}", participantId);
            return Optional.empty();
        }

        ConversationParticipant participant = optionalParticipant.get();
        participant.setIsActive(true);
        participant.setLeftAt(null);

        ConversationParticipant updatedParticipant = conversationParticipantRepository.save(participant);
        log.debug("ConversationParticipant marked as rejoined successfully with id: {}", updatedParticipant.getId());

        return Optional.of(updatedParticipant);
    }

    @Transactional
    public ConversationParticipant addCustomerToConversation(UUID conversationId, UUID customerId, UUID companyId) {
        log.debug("Adding customer: {} to conversation: {}", customerId, conversationId);

        // Check if customer is already a participant
        if (existsByConversationIdAndCustomerId(conversationId, customerId)) {
            throw new RuntimeException("Customer is already a participant in this conversation");
        }

        CreateConversationParticipantDTO createDTO = CreateConversationParticipantDTO.builder()
                .companyId(companyId)
                .conversationId(conversationId)
                .customerId(customerId)
                .isActive(true)
                .build();

        return create(createDTO);
    }

    @Transactional
    public ConversationParticipant addUserToConversation(UUID conversationId, UUID userId, UUID companyId) {
        log.debug("Adding user: {} to conversation: {}", userId, conversationId);

        // Check if user is already a participant
        if (existsByConversationIdAndUserId(conversationId, userId)) {
            throw new RuntimeException("User is already a participant in this conversation");
        }

        CreateConversationParticipantDTO createDTO = CreateConversationParticipantDTO.builder()
                .companyId(companyId)
                .conversationId(conversationId)
                .userId(userId)
                .isActive(true)
                .build();

        return create(createDTO);
    }

    @Transactional
    public ConversationParticipant addAiAgentToConversation(UUID conversationId, UUID aiAgentId, UUID companyId) {
        log.debug("Adding AI agent: {} to conversation: {}", aiAgentId, conversationId);

        // Check if AI agent is already a participant
        if (existsByConversationIdAndAiAgentId(conversationId, aiAgentId)) {
            throw new RuntimeException("AI agent is already a participant in this conversation");
        }

        CreateConversationParticipantDTO createDTO = CreateConversationParticipantDTO.builder()
                .companyId(companyId)
                .conversationId(conversationId)
                .aiAgentId(aiAgentId)
                .isActive(true)
                .build();

        return create(createDTO);
    }
}