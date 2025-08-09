package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateConversationMediaDTO;
import com.ruby.rubia_server.core.dto.CreateMessageDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageDTO;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.ConversationMedia;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.MediaType;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.repository.CampaignContactRepository;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
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
    private final MessageTemplateRepository messageTemplateRepository;
    private final CampaignContactRepository campaignContactRepository;
    private final ConversationMediaService conversationMediaService;
    
    public boolean hasDraftMessage(UUID conversationId) {
        log.debug("Checking if conversation {} has draft messages", conversationId);
        return messageRepository.existsByConversationIdAndStatus(conversationId, MessageStatus.DRAFT);
    }

    public MessageDTO create(CreateMessageDTO createDTO) {
        log.info("Creating message for conversation: {}", createDTO.getConversationId());
        
        Conversation conversation = conversationRepository.findById(createDTO.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada"));
        
        User sender = null;
        if (createDTO.getSenderType() == SenderType.AGENT && createDTO.getSenderId() != null) {
            sender = userRepository.findById(createDTO.getSenderId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        }
        
        // Check if message with external ID already exists (avoid duplicates from webhooks)
        if (createDTO.getExternalMessageId() != null) {
            Optional<Message> existingMessage = messageRepository.findByExternalMessageId(createDTO.getExternalMessageId());
            if (existingMessage.isPresent()) {
                log.info("Message with external ID {} already exists, returning existing message", 
                    createDTO.getExternalMessageId());
                
                User existingSender = null;
                if (existingMessage.get().getSenderType() == SenderType.AGENT && existingMessage.get().getSenderId() != null) {
                    existingSender = userRepository.findById(existingMessage.get().getSenderId()).orElse(null);
                }
                return toDTO(existingMessage.get(), existingSender);
            }
        }
        
        MessageTemplate messageTemplate = null;
        if (createDTO.getMessageTemplateId() != null) {
            messageTemplate = messageTemplateRepository.findById(createDTO.getMessageTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template de mensagem não encontrado"));
        }
        
        // Get CampaignContact if provided
        com.ruby.rubia_server.core.entity.CampaignContact campaignContact = null;
        if (createDTO.getCampaignContactId() != null) {
            campaignContact = campaignContactRepository.findById(createDTO.getCampaignContactId())
                    .orElseThrow(() -> new IllegalArgumentException("CampaignContact não encontrado"));
        }
        
        // Create ConversationMedia if this is a media message
        ConversationMedia media = null;
        if (createDTO.getMediaUrl() != null && !createDTO.getMediaUrl().trim().isEmpty()) {
            MediaType mediaType = convertToMediaType(createDTO.getMessageType());
            
            CreateConversationMediaDTO mediaDTO = CreateConversationMediaDTO.builder()
                    .companyId(conversation.getCompany().getId())
                    .conversationId(createDTO.getConversationId())
                    .fileUrl(createDTO.getMediaUrl())
                    .mediaType(mediaType)
                    .mimeType(createDTO.getMimeType())
                    .originalFileName(createDTO.getFileName())
                    .build();
            
            // Determine who uploaded based on sender type
            if (createDTO.getSenderType() == SenderType.AGENT && createDTO.getSenderId() != null) {
                mediaDTO.setUploadedByUserId(createDTO.getSenderId());
            } else if (createDTO.getSenderType() == SenderType.CUSTOMER) {
                // For incoming messages, we could set uploadedByCustomerId if we had the customer ID
                // For now, leaving it null as it's optional
            }
            
            media = conversationMediaService.create(mediaDTO);
            log.info("Created ConversationMedia with id: {} for media URL: {}", 
                media.getId(), createDTO.getMediaUrl());
        }
        
        Message message = Message.builder()
                .conversation(conversation)
                .content(createDTO.getContent())
                .senderType(createDTO.getSenderType())
                .senderId(createDTO.getSenderId())
                .externalMessageId(createDTO.getExternalMessageId())
                .isAiGenerated(createDTO.getIsAiGenerated())
                .aiConfidence(createDTO.getAiConfidence())
                .status(createDTO.getStatus())
                .messageTemplate(messageTemplate)
                .campaignContact(campaignContact)
                .media(media)
                .build();
        
        Message saved = messageRepository.save(message);
        log.info("Message created successfully with id: {}", saved.getId());
        
        return toDTO(saved, sender);
    }
    
    private MediaType convertToMediaType(MessageType messageType) {
        if (messageType == null) return null;
        
        return switch (messageType) {
            case IMAGE -> MediaType.IMAGE;
            case AUDIO -> MediaType.AUDIO;
            case FILE -> MediaType.DOCUMENT;
            default -> null;
        };
    }
    
    public MessageDTO createFromIncomingMessage(IncomingMessage incomingMessage, UUID conversationId) {
        log.info("Creating message from incoming message for conversation: {} (isFromMe: {}, mediaType: {})", 
            conversationId, incomingMessage.isFromMe(), incomingMessage.getMediaType());
        
        // Determine sender type based on whether message is from us or customer
        SenderType senderType = incomingMessage.isFromMe() ? SenderType.AGENT : SenderType.CUSTOMER;
        
        // Determine message type based on media
        MessageType messageType = MessageType.TEXT;
        if (incomingMessage.getMediaType() != null) {
            switch (incomingMessage.getMediaType().toLowerCase()) {
                case "image" -> messageType = MessageType.IMAGE;
                case "audio" -> messageType = MessageType.AUDIO;
                case "document", "file" -> messageType = MessageType.FILE;
            }
        }
        
        CreateMessageDTO createDTO = CreateMessageDTO.builder()
                .conversationId(conversationId)
                .content(incomingMessage.getBody())
                .senderType(senderType)
                .senderId(null) // TODO: Could identify which agent sent if needed
                .messageType(messageType)
                .mediaUrl(incomingMessage.getMediaUrl())
                .mimeType(incomingMessage.getMimeType())
                .fileName(incomingMessage.getFileName())
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
        
        if (updateDTO.getExternalMessageId() != null) {
            message.setExternalMessageId(updateDTO.getExternalMessageId());
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
    
    @Transactional(readOnly = true)
    public List<MessageDTO> findByConversationAndStatus(UUID conversationId, MessageStatus status) {
        log.debug("Finding messages by conversation: {} and status: {}", conversationId, status);
        
        List<Message> messages = messageRepository.findByConversationIdAndStatus(conversationId, status);
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
    
    
    
    private MessageDTO toDTO(Message message, User sender) {
        // Extract media information if available
        String mediaUrl = null;
        String messageType = "TEXT";
        
        if (message.getMedia() != null) {
            mediaUrl = message.getMedia().getFileUrl();
            if (message.getMedia().getMediaType() != null) {
                messageType = message.getMedia().getMediaType().name();
            }
        }
        
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .content(message.getContent())
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getName() : null)
                .messageType(messageType)
                .mediaUrl(mediaUrl)
                .externalMessageId(message.getExternalMessageId())
                .isAiGenerated(message.getIsAiGenerated())
                .aiConfidence(message.getAiConfidence())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                // Frontend compatibility
                .timestamp(message.getCreatedAt() != null ? 
                    message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isFromUser(message.getSenderType() != SenderType.CUSTOMER)
                .build();
    }
}