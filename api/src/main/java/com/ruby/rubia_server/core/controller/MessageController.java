package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CreateMessageDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageDTO;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.service.MessageService;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.service.CustomerService;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.messaging.service.MessagingService;
import com.ruby.rubia_server.messaging.model.MessageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final CustomerService customerService;
    private final MessagingService messagingService;
    
    @PostMapping
    public ResponseEntity<MessageDTO> create(@Valid @RequestBody CreateMessageDTO createDTO) {
        log.info("Creating message for conversation: {}", createDTO.getConversationId());
        
        try {
            MessageDTO created = messageService.create(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating message: {}", e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MessageDTO> findById(@PathVariable UUID id) {
        log.debug("Finding message by id: {}", id);
        
        try {
            MessageDTO message = messageService.findById(id);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            log.warn("Message not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/external/{externalMessageId}")
    public ResponseEntity<MessageDTO> findByExternalMessageId(@PathVariable String externalMessageId) {
        log.debug("Finding message by external ID: {}", externalMessageId);
        
        try {
            MessageDTO message = messageService.findByExternalMessageId(externalMessageId);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            log.warn("Message not found: {}", externalMessageId);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> findByConversation(@PathVariable UUID conversationId) {
        log.debug("Finding messages by conversation: {}", conversationId);
        
        List<MessageDTO> messages = messageService.findByConversation(conversationId);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/conversation/{conversationId}/paginated")
    public ResponseEntity<Page<MessageDTO>> findByConversationPaginated(
            @PathVariable UUID conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("Finding messages by conversation with pagination: {}", conversationId);
        
        Page<MessageDTO> messages = messageService.findByConversationWithPagination(conversationId, pageable);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<MessageDTO>> searchInContent(@RequestParam String searchTerm) {
        log.debug("Searching messages by content: {}", searchTerm);
        
        List<MessageDTO> messages = messageService.searchInContent(searchTerm);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/conversation/{conversationId}/search")
    public ResponseEntity<List<MessageDTO>> searchInConversation(
            @PathVariable UUID conversationId,
            @RequestParam String searchTerm) {
        log.debug("Searching messages in conversation: {} with term: {}", conversationId, searchTerm);
        
        List<MessageDTO> messages = messageService.searchInConversation(conversationId, searchTerm);
        return ResponseEntity.ok(messages);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MessageDTO> update(@PathVariable UUID id, 
                                            @Valid @RequestBody UpdateMessageDTO updateDTO) {
        log.info("Updating message: {}", id);
        
        try {
            MessageDTO updated = messageService.update(id, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating message: {}", e.getMessage());
            if (e.getMessage().contains("não encontrada")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
    
    @PutMapping("/{id}/delivered")
    public ResponseEntity<MessageDTO> markAsDelivered(@PathVariable UUID id) {
        log.info("Marking message as delivered: {}", id);
        
        try {
            MessageDTO delivered = messageService.markAsDelivered(id);
            return ResponseEntity.ok(delivered);
        } catch (IllegalArgumentException e) {
            log.warn("Error marking message as delivered: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<MessageDTO> markAsRead(@PathVariable UUID id) {
        log.info("Marking message as read: {}", id);
        
        try {
            MessageDTO read = messageService.markAsRead(id);
            return ResponseEntity.ok(read);
        } catch (IllegalArgumentException e) {
            log.warn("Error marking message as read: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/conversation/{conversationId}/read-all")
    public ResponseEntity<Void> markConversationAsRead(@PathVariable UUID conversationId) {
        log.info("Marking all messages as read for conversation: {}", conversationId);
        
        messageService.markConversationMessagesAsRead(conversationId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/conversation/{conversationId}/stats/unread")
    public ResponseEntity<Long> countUnreadByConversation(@PathVariable UUID conversationId) {
        log.debug("Counting unread messages for conversation: {}", conversationId);
        
        long count = messageService.countUnreadByConversation(conversationId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/conversation/{conversationId}/stats/total")
    public ResponseEntity<Long> countByConversation(@PathVariable UUID conversationId) {
        log.debug("Counting total messages for conversation: {}", conversationId);
        
        long count = messageService.countByConversation(conversationId);
        return ResponseEntity.ok(count);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting message: {}", id);
        
        try {
            messageService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting message: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/conversation/{conversationId}/send")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        
        log.info("Sending message via WhatsApp for conversation: {}", conversationId);
        
        try {
            // Get conversation and validate access
            ConversationDTO conversation = conversationService.findById(conversationId, request.getCompanyId());
            
            // Get customer from conversation
            CustomerDTO customerDTO = customerService.findById(conversation.getCustomerId(), request.getCompanyId());
            
            // Validate customer has WhatsApp
            if (customerDTO.getPhone() == null || customerDTO.getPhone().trim().isEmpty()) {
                log.warn("Customer {} does not have a phone number", customerDTO.getId());
                return ResponseEntity.badRequest().build();
            }
            
            // Create message in database with SENDING status first
            CreateMessageDTO createDTO = CreateMessageDTO.builder()
                .conversationId(conversationId)
                .companyId(request.getCompanyId())
                .content(request.getContent())
                .senderType(SenderType.AGENT)
                .senderId(request.getSenderId())
                .build();
            
            MessageDTO message = messageService.create(createDTO);
            
            try {
                // Send via Twilio
                MessageResult result = messagingService.sendMessage(
                    customerDTO.getPhone(),
                    request.getContent(),
                    request.getCompanyId(),
                    request.getSenderId()
                );
                
                if (result.isSuccess()) {
                    // Update message with external ID and SENT status
                    UpdateMessageDTO updateDTO = UpdateMessageDTO.builder()
                        .status(MessageStatus.SENT)
                        .externalMessageId(result.getMessageId())
                        .build();
                    
                    MessageDTO updatedMessage = messageService.update(message.getId(), updateDTO);
                    log.info("Message sent successfully via WhatsApp. External ID: {}", result.getMessageId());
                    
                    return ResponseEntity.ok(updatedMessage);
                } else {
                    // Mark message as failed
                    UpdateMessageDTO failedUpdateDTO = UpdateMessageDTO.builder()
                        .status(MessageStatus.FAILED)
                        .build();
                    
                    messageService.update(message.getId(), failedUpdateDTO);
                    log.error("Failed to send message via WhatsApp: {}", result.getError());
                    
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
                
            } catch (Exception e) {
                // Mark message as failed
                UpdateMessageDTO failedUpdateDTO = UpdateMessageDTO.builder()
                    .status(MessageStatus.FAILED)
                    .build();
                
                messageService.update(message.getId(), failedUpdateDTO);
                log.error("Error sending message via WhatsApp: {}", e.getMessage(), e);
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Error sending message: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Inner class for request body
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        @NotBlank(message = "Conteúdo da mensagem é obrigatório")
        @Size(max = 4000, message = "Conteúdo não pode exceder 4000 caracteres")
        private String content;
        
        @NotNull(message = "ID da empresa é obrigatório")
        private UUID companyId;
        
        @NotNull(message = "ID do remetente é obrigatório")
        private UUID senderId;
    }
}