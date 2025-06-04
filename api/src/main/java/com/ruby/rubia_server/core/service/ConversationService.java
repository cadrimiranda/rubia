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
    
    public ConversationDTO create(CreateConversationDTO createDTO) {
        log.info("Creating conversation for customer: {}", createDTO.getCustomerId());
        
        Customer customer = customerRepository.findById(createDTO.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        User assignedUser = null;
        if (createDTO.getAssignedUserId() != null) {
            assignedUser = userRepository.findById(createDTO.getAssignedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        }
        
        Department department = null;
        if (createDTO.getDepartmentId() != null) {
            department = departmentRepository.findById(createDTO.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
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
    public ConversationDTO findById(UUID id) {
        log.debug("Finding conversation by id: {}", id);
        
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        return toDTO(conversation);
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findByStatus(ConversationStatus status) {
        log.debug("Finding conversations by status: {}", status);
        
        return conversationRepository.findByStatusOrderedByPriorityAndUpdatedAt(status)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Page<ConversationDTO> findByStatusWithPagination(ConversationStatus status, Pageable pageable) {
        log.debug("Finding conversations by status with pagination: {}", status);
        
        return conversationRepository.findByStatusOrderedByPriorityAndUpdatedAt(status, pageable)
                .map(this::toDTO);
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findByCustomer(UUID customerId) {
        log.debug("Finding conversations by customer: {}", customerId);
        
        return conversationRepository.findConversationsByCustomerOrderedByUpdatedAt(customerId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findByAssignedUser(UUID userId) {
        log.debug("Finding conversations by assigned user: {}", userId);
        
        return conversationRepository.findByAssignedUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> findUnassigned() {
        log.debug("Finding unassigned conversations");
        
        return conversationRepository.findUnassignedEntranceConversations()
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> findSummariesByStatus(ConversationStatus status) {
        log.debug("Finding conversation summaries by status: {}", status);
        
        return conversationRepository.findByStatusOrderedByPriorityAndUpdatedAt(status)
                .stream()
                .map(this::toSummaryDTO)
                .toList();
    }
    
    public ConversationDTO assignToUser(UUID conversationId, UUID userId) {
        log.info("Assigning conversation {} to user {}", conversationId, userId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        conversation.setAssignedUser(user);
        conversation.setStatus(ConversationStatus.ESPERANDO);
        
        Conversation updated = conversationRepository.save(conversation);
        log.info("Conversation assigned successfully");
        
        return toDTO(updated);
    }
    
    public ConversationDTO changeStatus(UUID conversationId, ConversationStatus newStatus) {
        log.info("Changing conversation {} status to {}", conversationId, newStatus);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
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
    
    public ConversationDTO pinConversation(UUID conversationId) {
        log.info("Toggling pin status for conversation: {}", conversationId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        conversation.setIsPinned(!conversation.getIsPinned());
        
        Conversation updated = conversationRepository.save(conversation);
        log.info("Conversation pin status toggled successfully");
        
        return toDTO(updated);
    }
    
    public ConversationDTO update(UUID id, UpdateConversationDTO updateDTO) {
        log.info("Updating conversation with id: {}", id);
        
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        if (updateDTO.getAssignedUserId() != null) {
            User user = userRepository.findById(updateDTO.getAssignedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            conversation.setAssignedUser(user);
        }
        
        if (updateDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(updateDTO.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
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
    
    public void delete(UUID id) {
        log.info("Deleting conversation with id: {}", id);
        
        if (!conversationRepository.existsById(id)) {
            throw new IllegalArgumentException("Conversa não encontrada");
        }
        
        conversationRepository.deleteById(id);
        log.info("Conversation deleted successfully");
    }
    
    @Transactional(readOnly = true)
    public long countByStatus(ConversationStatus status) {
        return conversationRepository.countByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public long countActiveByUser(UUID userId) {
        return conversationRepository.countActiveConversationsByUser(userId);
    }
    
    private ConversationDTO toDTO(Conversation conversation) {
        return ConversationDTO.builder()
                .id(conversation.getId())
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