package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateConversationParticipantDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationParticipantDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ConversationParticipantService {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AIAgentRepository aiAgentRepository;
    private final CompanyRepository companyRepository;

    public ConversationParticipantService(ConversationParticipantRepository conversationParticipantRepository,
                                         ConversationRepository conversationRepository,
                                         CustomerRepository customerRepository,
                                         UserRepository userRepository,
                                         AIAgentRepository aiAgentRepository,
                                         CompanyRepository companyRepository) {
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.conversationRepository = conversationRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.aiAgentRepository = aiAgentRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public ConversationParticipant create(CreateConversationParticipantDTO createDTO) {
        log.debug("Creating ConversationParticipant with data: {}", createDTO);
        
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

        // Validate and set required company
        Company company = companyRepository.findById(createDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + createDTO.getCompanyId()));
        builder.company(company);

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

        ConversationParticipant entity = builder.build();
        ConversationParticipant savedEntity = conversationParticipantRepository.save(entity);
        log.debug("ConversationParticipant created successfully with id: {}", savedEntity.getId());
        return savedEntity;
    }

    @Transactional
    public Optional<ConversationParticipant> update(UUID id, UpdateConversationParticipantDTO updateDTO) {
        log.debug("Updating ConversationParticipant with id: {} and data: {}", id, updateDTO);
        
        Optional<ConversationParticipant> optionalEntity = conversationParticipantRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            log.warn("ConversationParticipant not found with id: {}", id);
            return Optional.empty();
        }

        ConversationParticipant entity = optionalEntity.get();
        
        if (updateDTO.getIsActive() != null) {
            entity.setIsActive(updateDTO.getIsActive());
            
            // If marking as inactive, set leftAt timestamp
            if (!updateDTO.getIsActive()) {
                entity.setLeftAt(updateDTO.getLeftAt() != null ? updateDTO.getLeftAt() : LocalDateTime.now());
            }
            // If marking as active again, clear leftAt timestamp
            else {
                entity.setLeftAt(null);
            }
        }
        
        if (updateDTO.getLeftAt() != null) {
            entity.setLeftAt(updateDTO.getLeftAt());
        }

        ConversationParticipant updatedEntity = conversationParticipantRepository.save(entity);
        log.debug("ConversationParticipant updated successfully with id: {}", updatedEntity.getId());
        return Optional.of(updatedEntity);
    }

    @Transactional(readOnly = true)
    public Optional<ConversationParticipant> findById(UUID id) {
        log.debug("Finding ConversationParticipant by id: {}", id);
        return conversationParticipantRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<ConversationParticipant> findAll(Pageable pageable) {
        log.debug("Finding all ConversationParticipant with pageable: {}", pageable);
        return conversationParticipantRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipant> findByCompanyId(UUID companyId) {
        log.debug("Finding ConversationParticipant by company id: {}", companyId);
        return conversationParticipantRepository.findByCompanyId(companyId);
    }

    @Transactional
    public boolean deleteById(UUID id) {
        log.debug("Deleting ConversationParticipant with id: {}", id);
        if (conversationParticipantRepository.existsById(id)) {
            conversationParticipantRepository.deleteById(id);
            log.debug("ConversationParticipant deleted successfully with id: {}", id);
            return true;
        } else {
            log.warn("ConversationParticipant not found with id: {}", id);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public long countByCompanyId(UUID companyId) {
        log.debug("Counting ConversationParticipant by company id: {}", companyId);
        return conversationParticipantRepository.countByCompanyId(companyId);
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