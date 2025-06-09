package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.ConversationSummaryDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationDTO;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    
    public ConversationDTO create(CreateConversationDTO createDTO, UUID companyId) {
        log.info("Creating conversation for customer: {} in company: {}", createDTO.getCustomerId(), companyId);
        
        Customer customer = customerRepository.findById(createDTO.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        // Validate customer belongs to company
        if (!customer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cliente não pertence a esta empresa");
        }
        
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
        
        Conversation conversation = Conversation.builder()
                .customer(customer)
                .assignedUser(assignedUser)
                .department(department)
                .status(createDTO.getStatus())
                .channel(createDTO.getChannel())
                .priority(createDTO.getPriority())
                .isPinned(createDTO.getIsPinned())
                .build();
        
        Conversation saved = conversationRepository.save(conversation);
        log.info("Conversation created successfully with id: {}", saved.getId());
        
        return toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public ConversationDTO findById(UUID id, UUID companyId) {
        log.debug("Finding conversation by id: {} for company: {}", id, companyId);
        
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
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
        
        ConversationStatus oldStatus = conversation.getStatus();
        conversation.setStatus(newStatus);
        
        if (newStatus == ConversationStatus.FINALIZADOS && oldStatus != ConversationStatus.FINALIZADOS) {
            conversation.setClosedAt(LocalDateTime.now());
        } else if (newStatus != ConversationStatus.FINALIZADOS && oldStatus == ConversationStatus.FINALIZADOS) {
            conversation.setClosedAt(null);
        }
        
        Conversation updated = conversationRepository.save(conversation);
        log.info("Conversation status changed successfully");
        
        return toDTO(updated);
    }
    
    public ConversationDTO pinConversation(UUID conversationId, UUID companyId) {
        log.info("Toggling pin status for conversation: {} in company: {}", conversationId, companyId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        // Validate conversation belongs to company
        if (!conversation.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Conversa não pertence a esta empresa");
        }
        
        conversation.setIsPinned(!conversation.getIsPinned());
        
        Conversation updated = conversationRepository.save(conversation);
        log.info("Conversation pin status toggled successfully");
        
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
        
        if (updateDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(updateDTO.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
            // Validate department belongs to company
            if (!department.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("Departamento não pertence a esta empresa");
            }
            conversation.setDepartment(department);
        }
        
        if (updateDTO.getStatus() != null) {
            ConversationStatus oldStatus = conversation.getStatus();
            conversation.setStatus(updateDTO.getStatus());
            
            if (updateDTO.getStatus() == ConversationStatus.FINALIZADOS && oldStatus != ConversationStatus.FINALIZADOS) {
                conversation.setClosedAt(LocalDateTime.now());
            } else if (updateDTO.getStatus() != ConversationStatus.FINALIZADOS && oldStatus == ConversationStatus.FINALIZADOS) {
                conversation.setClosedAt(null);
            }
        }
        
        if (updateDTO.getChannel() != null) {
            conversation.setChannel(updateDTO.getChannel());
        }
        
        if (updateDTO.getPriority() != null) {
            conversation.setPriority(updateDTO.getPriority());
        }
        
        if (updateDTO.getIsPinned() != null) {
            conversation.setIsPinned(updateDTO.getIsPinned());
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
    
    
    // Company-scoped methods
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
        log.debug("Finding conversations by status: {} for company: {} with pagination", status, companyId);
        
        return conversationRepository.findByStatusAndCompanyOrderedByPriorityAndUpdatedAt(status, companyId, pageable)
                .map(this::toDTO);
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

    private ConversationDTO toDTO(Conversation conversation) {
        return ConversationDTO.builder()
                .id(conversation.getId())
                .companyId(conversation.getCompany() != null ? conversation.getCompany().getId() : null)
                .customerId(conversation.getCustomer().getId())
                .customerName(conversation.getCustomer().getName())
                .customerPhone(conversation.getCustomer().getPhone())
                .assignedUserId(conversation.getAssignedUser() != null ? conversation.getAssignedUser().getId() : null)
                .assignedUserName(conversation.getAssignedUser() != null ? conversation.getAssignedUser().getName() : null)
                .departmentId(conversation.getDepartment() != null ? conversation.getDepartment().getId() : null)
                .departmentName(conversation.getDepartment() != null ? conversation.getDepartment().getName() : null)
                .status(conversation.getStatus())
                .channel(conversation.getChannel())
                .priority(conversation.getPriority())
                .isPinned(conversation.getIsPinned())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .closedAt(conversation.getClosedAt())
                .unreadCount(0L) // Will be calculated by message service
                .build();
    }
    
    private ConversationSummaryDTO toSummaryDTO(Conversation conversation) {
        return ConversationSummaryDTO.builder()
                .id(conversation.getId())
                .customerName(conversation.getCustomer().getName())
                .customerPhone(conversation.getCustomer().getPhone())
                .assignedUserName(conversation.getAssignedUser() != null ? conversation.getAssignedUser().getName() : null)
                .status(conversation.getStatus())
                .channel(conversation.getChannel())
                .priority(conversation.getPriority())
                .isPinned(conversation.getIsPinned())
                .updatedAt(conversation.getUpdatedAt())
                .unreadCount(0L) // Will be calculated by message service
                .lastMessageContent(null) // Will be populated by message service
                .lastMessageTime(null) // Will be populated by message service
                .build();
    }
}