package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateMessageDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageDTO;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.messaging.model.IncomingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    
    public MessageDTO create(CreateMessageDTO createDTO) {
        log.info("Creating message for conversation: {}", createDTO.getConversationId());
        
        Conversation conversation = conversationRepository.findById(createDTO.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        User sender = null;
        if (createDTO.getSenderType() == SenderType.AGENT && createDTO.getSenderId() != null) {
            sender = userRepository.findById(createDTO.getSenderId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        }
        
        if (createDTO.getExternalMessageId() != null && 
            messageRepository.existsByExternalMessageId(createDTO.getExternalMessageId())) {
            throw new IllegalArgumentException("Mensagem com ID externo já existe");
        }
        
        Message message = Message.builder()
                .conversation(conversation)
                .content(createDTO.getContent())
                .senderType(createDTO.getSenderType())
                .senderId(createDTO.getSenderId())
                .externalMessageId(createDTO.getExternalMessageId())
                .isAiGenerated(createDTO.getIsAiGenerated())
                .aiConfidence(createDTO.getAiConfidence())
                .build();
        
        Message saved = messageRepository.save(message);
        log.info("Message created successfully with id: {}", saved.getId());
        
        return toDTO(saved, sender);
    }
    
    public MessageDTO createFromIncomingMessage(IncomingMessage incomingMessage, UUID conversationId) {
        log.info("Creating message from incoming message for conversation: {}", conversationId);
        
        CreateMessageDTO createDTO = CreateMessageDTO.builder()
                .conversationId(conversationId)
                .content(incomingMessage.getBody())
                .senderType(SenderType.CUSTOMER)
                .senderId(null)
                .externalMessageId(incomingMessage.getMessageId())
                .isAiGenerated(false)
                .build();
        
        return create(createDTO);
    }
    
    @Transactional(readOnly = true)
    public MessageDTO findById(UUID id) {
        log.debug("Finding message by id: {}", id);
        
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));
        
        User sender = null;
        if (message.getSenderType() == SenderType.AGENT && message.getSenderId() != null) {
            sender = userRepository.findById(message.getSenderId()).orElse(null);
        }
        
        return toDTO(message, sender);
    }
    
    @Transactional(readOnly = true)
    public List<MessageDTO> findByConversation(UUID conversationId) {
        log.debug("Finding messages by conversation: {}", conversationId);
        
        List<Message> messages = messageRepository.findByConversationIdOrderedByCreatedAt(conversationId);
        return messages.stream()
                .map(message -> {
                    User sender = null;
                    if (message.getSenderType() == SenderType.AGENT && message.getSenderId() != null) {
                        sender = userRepository.findById(message.getSenderId()).orElse(null);
                    }
                    return toDTO(message, sender);
                })
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Page<MessageDTO> findByConversationWithPagination(UUID conversationId, Pageable pageable) {
        log.debug("Finding messages by conversation with pagination: {}", conversationId);
        
        return messageRepository.findByConversationIdOrderedByCreatedAtDesc(conversationId, pageable)
                .map(message -> {
                    User sender = null;
                    if (message.getSenderType() == SenderType.AGENT && message.getSenderId() != null) {
                        sender = userRepository.findById(message.getSenderId()).orElse(null);
                    }
                    return toDTO(message, sender);
                });
    }
    
    @Transactional(readOnly = true)
    public MessageDTO findByExternalMessageId(String externalMessageId) {
        log.debug("Finding message by external ID: {}", externalMessageId);
        
        Message message = messageRepository.findByExternalMessageId(externalMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));
        
        User sender = null;
        if (message.getSenderType() == SenderType.AGENT && message.getSenderId() != null) {
            sender = userRepository.findById(message.getSenderId()).orElse(null);
        }
        
        return toDTO(message, sender);
    }
    
    @Transactional(readOnly = true)
    public List<MessageDTO> searchInContent(String searchTerm) {
        log.debug("Searching messages by content: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }
        
        List<Message> messages = messageRepository.searchByContent(searchTerm.trim());
        return messages.stream()
                .map(message -> {
                    User sender = null;
                    if (message.getSenderType() == SenderType.AGENT && message.getSenderId() != null) {
                        sender = userRepository.findById(message.getSenderId()).orElse(null);
                    }
                    return toDTO(message, sender);
                })
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<MessageDTO> searchInConversation(UUID conversationId, String searchTerm) {
        log.debug("Searching messages in conversation: {} with term: {}", conversationId, searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findByConversation(conversationId);
        }
        
        List<Message> messages = messageRepository.searchByContentInConversation(conversationId, searchTerm.trim());
        return messages.stream()
                .map(message -> {
                    User sender = null;
                    if (message.getSenderType() == SenderType.AGENT && message.getSenderId() != null) {
                        sender = userRepository.findById(message.getSenderId()).orElse(null);
                    }
                    return toDTO(message, sender);
                })
                .toList();
    }
    
    public MessageDTO update(UUID id, UpdateMessageDTO updateDTO) {
        log.info("Updating message with id: {}", id);
        
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));
        
        if (updateDTO.getContent() != null) {
            message.setContent(updateDTO.getContent());
        }
        
        if (updateDTO.getStatus() != null) {
            MessageStatus oldStatus = message.getStatus();
            message.setStatus(updateDTO.getStatus());
            
            // Update delivery/read timestamps
            if (updateDTO.getStatus() == MessageStatus.DELIVERED && oldStatus != MessageStatus.DELIVERED) {
                message.setDeliveredAt(LocalDateTime.now());
            } else if (updateDTO.getStatus() == MessageStatus.READ && oldStatus != MessageStatus.READ) {
                message.setReadAt(LocalDateTime.now());
                if (message.getDeliveredAt() == null) {
                    message.setDeliveredAt(LocalDateTime.now());
                }
            }
        }
        
        if (updateDTO.getIsAiGenerated() != null) {
            message.setIsAiGenerated(updateDTO.getIsAiGenerated());
        }
        
        if (updateDTO.getAiConfidence() != null) {
            message.setAiConfidence(updateDTO.getAiConfidence());
        }
        
        Message updated = messageRepository.save(message);
        log.info("Message updated successfully");
        
        User sender = null;
        if (updated.getSenderType() == SenderType.AGENT && updated.getSenderId() != null) {
            sender = userRepository.findById(updated.getSenderId()).orElse(null);
        }
        
        return toDTO(updated, sender);
    }
    
    public MessageDTO markAsDelivered(UUID id) {
        log.info("Marking message as delivered: {}", id);
        
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));
        
        message.setStatus(MessageStatus.DELIVERED);
        message.setDeliveredAt(LocalDateTime.now());
        
        Message updated = messageRepository.save(message);
        log.info("Message marked as delivered successfully");
        
        User sender = null;
        if (updated.getSenderType() == SenderType.AGENT && updated.getSenderId() != null) {
            sender = userRepository.findById(updated.getSenderId()).orElse(null);
        }
        
        return toDTO(updated, sender);
    }
    
    public MessageDTO markAsRead(UUID id) {
        log.info("Marking message as read: {}", id);
        
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));
        
        message.setStatus(MessageStatus.READ);
        message.setReadAt(LocalDateTime.now());
        
        if (message.getDeliveredAt() == null) {
            message.setDeliveredAt(LocalDateTime.now());
        }
        
        Message updated = messageRepository.save(message);
        log.info("Message marked as read successfully");
        
        User sender = null;
        if (updated.getSenderType() == SenderType.AGENT && updated.getSenderId() != null) {
            sender = userRepository.findById(updated.getSenderId()).orElse(null);
        }
        
        return toDTO(updated, sender);
    }
    
    public void markConversationMessagesAsRead(UUID conversationId) {
        log.info("Marking all unread customer messages as read for conversation: {}", conversationId);
        
        List<Message> unreadMessages = messageRepository.findUnreadCustomerMessages(conversationId);
        
        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(LocalDateTime.now());
            if (message.getDeliveredAt() == null) {
                message.setDeliveredAt(LocalDateTime.now());
            }
        }
        
        messageRepository.saveAll(unreadMessages);
        log.info("Marked {} messages as read", unreadMessages.size());
    }
    
    public void delete(UUID id) {
        log.info("Deleting message with id: {}", id);
        
        if (!messageRepository.existsById(id)) {
            throw new IllegalArgumentException("Mensagem não encontrada");
        }
        
        messageRepository.deleteById(id);
        log.info("Message deleted successfully");
    }
    
    @Transactional(readOnly = true)
    public long countUnreadByConversation(UUID conversationId) {
        return messageRepository.countUnreadCustomerMessages(conversationId);
    }
    
    @Transactional(readOnly = true)
    public long countByConversation(UUID conversationId) {
        return messageRepository.countByConversationId(conversationId);
    }
    
    public void deleteAllByCompany(UUID companyId) {
        log.info("Deleting all messages for company: {}", companyId);
        
        List<Message> messages = messageRepository.findByConversationCompanyId(companyId);
        messageRepository.deleteAll(messages);
        
        log.info("Deleted {} messages for company: {}", messages.size(), companyId);
    }
    
    
    
    private MessageDTO toDTO(Message message, User sender) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .content(message.getContent())
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getName() : null)
                .externalMessageId(message.getExternalMessageId())
                .isAiGenerated(message.getIsAiGenerated())
                .aiConfidence(message.getAiConfidence())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                // Frontend compatibility
                .timestamp(message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isFromUser(message.getSenderType() != SenderType.CUSTOMER)
                .build();
    }
}