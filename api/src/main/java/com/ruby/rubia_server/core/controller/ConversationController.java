package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.ConversationSummaryDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationDTO;
import com.ruby.rubia_server.core.dto.CreateMessageDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.ConversationMediaDTO;
import com.ruby.rubia_server.core.dto.CreateConversationMediaDTO;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.enums.MediaType;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.service.MessageService;
import com.ruby.rubia_server.core.service.CustomerService;
import com.ruby.rubia_server.core.service.ConversationMediaService;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.ConversationMedia;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import com.ruby.rubia_server.core.service.MessagingService;
import com.ruby.rubia_server.core.service.WebSocketNotificationService;
import com.ruby.rubia_server.core.entity.MessageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.entity.User;

import java.security.Principal;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {
    
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CustomerService customerService;
    private final ConversationMediaService conversationMediaService;
    private final MessagingService messagingService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final CompanyContextUtil companyContextUtil;
    private final UserRepository userRepository;
    
    /**
     * Helper method to get user UUID from Principal (email)
     */
    private UUID getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return user.getId();
    }
    
    @PostMapping
    public ResponseEntity<ConversationDTO> create(@Valid @RequestBody CreateConversationDTO createDTO) {
        log.info("Creating conversation for customer: {}", createDTO.getCustomerId());
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO created = conversationService.create(createDTO, currentCompanyId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating conversation: {}", e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> findById(@PathVariable UUID id) {
        log.debug("Finding conversation by id: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO conversation = conversationService.findById(id, currentCompanyId);
            return ResponseEntity.ok(conversation);
        } catch (IllegalArgumentException e) {
            log.warn("Conversation not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<Page<ConversationDTO>> findByStatus(
            @RequestParam ConversationStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            Principal principal) {
        log.debug("Finding conversations by status: {} with pageable: {}", status, pageable);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        UUID userId = getUserIdFromPrincipal(principal);
        Page<ConversationDTO> conversations = conversationService.findByStatusAndCompanyWithPagination(status, currentCompanyId, pageable, userId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/paginated")
    public ResponseEntity<Page<ConversationDTO>> findByStatusPaginated(
            @RequestParam ConversationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Finding conversations by status with pagination: {}", status);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        Page<ConversationDTO> conversations = conversationService.findByStatusAndCompanyWithPagination(status, currentCompanyId, pageable);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/summaries")
    public ResponseEntity<List<ConversationSummaryDTO>> findSummariesByStatus(
            @RequestParam ConversationStatus status) {
        log.debug("Finding conversation summaries by status: {}", status);
        
        // This endpoint is not implemented for multi-tenant - use /api/conversations instead
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ConversationDTO>> findByCustomer(@PathVariable UUID customerId) {
        log.debug("Finding conversations by customer: {}", customerId);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<ConversationDTO> conversations = conversationService.findByCustomerAndCompany(customerId, currentCompanyId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationDTO>> findByAssignedUser(@PathVariable UUID userId) {
        log.debug("Finding conversations by assigned user: {}", userId);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<ConversationDTO> conversations = conversationService.findByAssignedUserAndCompany(userId, currentCompanyId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/unassigned")
    public ResponseEntity<List<ConversationDTO>> findUnassigned() {
        log.debug("Finding unassigned conversations");
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<ConversationDTO> conversations = conversationService.findUnassignedByCompany(currentCompanyId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/ordered-by-last-message")
    public ResponseEntity<List<ConversationDTO>> findConversationsOrderedByLastMessage() {
        log.debug("Finding conversations ordered by last message date");
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<ConversationDTO> conversations = conversationService.findConversationsOrderByLastMessageDate(currentCompanyId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/ordered-by-last-message/paginated")
    public ResponseEntity<Page<ConversationDTO>> findConversationsOrderedByLastMessagePaginated(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) ConversationStatus status) {
        log.debug("Finding conversations ordered by last message date with pagination, status: {}", status);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        Page<ConversationDTO> conversations;
        
        if (status != null) {
            conversations = conversationService
                    .findConversationsOrderByLastMessageDateByStatusWithPagination(currentCompanyId, status, pageable);
        } else {
            conversations = conversationService
                    .findConversationsOrderByLastMessageDateWithPagination(currentCompanyId, pageable);
        }
        
        return ResponseEntity.ok(conversations);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ConversationDTO> update(@PathVariable UUID id, 
                                                 @Valid @RequestBody UpdateConversationDTO updateDTO) {
        log.info("Updating conversation: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO updated = conversationService.update(id, updateDTO, currentCompanyId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating conversation: {}", e.getMessage());
            if (e.getMessage().contains("não encontrad")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
    
    @PutMapping("/{conversationId}/assign/{userId}")
    public ResponseEntity<ConversationDTO> assignToUser(@PathVariable UUID conversationId,
                                                        @PathVariable UUID userId) {
        log.info("Assigning conversation {} to user {}", conversationId, userId);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO assigned = conversationService.assignToUser(conversationId, userId, currentCompanyId);
            return ResponseEntity.ok(assigned);
        } catch (IllegalArgumentException e) {
            log.warn("Error assigning conversation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{conversationId}/status")
    public ResponseEntity<ConversationDTO> changeStatus(@PathVariable UUID conversationId,
                                                        @RequestParam ConversationStatus status) {
        log.info("Changing conversation {} status to {}", conversationId, status);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO updated = conversationService.changeStatus(conversationId, status, currentCompanyId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error changing conversation status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    
    
    @GetMapping("/stats/count")
    public ResponseEntity<Long> countByStatus(@RequestParam ConversationStatus status) {
        log.debug("Counting conversations by status: {}", status);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        long count = conversationService.countByStatusAndCompany(status, currentCompanyId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/active-by-user/{userId}")
    public ResponseEntity<Long> countActiveByUser(@PathVariable UUID userId) {
        log.debug("Counting active conversations by user: {}", userId);
        
        // This endpoint is not implemented for multi-tenant
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting conversation: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            conversationService.delete(id, currentCompanyId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting conversation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    // Message endpoints for conversations
    
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) MessageStatus status) {
        log.debug("Getting messages for conversation: {} with status: {}", conversationId, status);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            
            // Validate conversation exists and user has access
            conversationService.findById(conversationId, currentCompanyId);
            
            List<MessageDTO> messages;
            if (status != null) {
                messages = messageService.findByConversationAndStatus(conversationId, status);
            } else {
                messages = messageService.findByConversation(conversationId);
            }
            
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            log.warn("Error getting messages: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        
        log.info("Sending message for conversation: {}", conversationId);
        
        try {
            // Validate that either content or mediaUrl is provided
            if ((request.getContent() == null || request.getContent().trim().isEmpty()) && 
                (request.getMediaUrl() == null || request.getMediaUrl().trim().isEmpty())) {
                return ResponseEntity.badRequest().build();
            }
            
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            
            // Get conversation and validate access
            ConversationDTO conversation = conversationService.findById(conversationId, currentCompanyId);
            
            // Get customer from conversation
            CustomerDTO customerDTO = customerService.findById(conversation.getCustomerId(), currentCompanyId);
            
            // Create message in database first
            CreateMessageDTO createDTO = CreateMessageDTO.builder()
                .conversationId(conversationId)
                .companyId(currentCompanyId)
                .content(request.getContent())
                .senderType(SenderType.AGENT)
                .senderId(request.getSenderId())
                .messageType(com.ruby.rubia_server.core.enums.MessageType.valueOf(request.getMessageType()))
                .mediaUrl(request.getMediaUrl())
                .build();
            
            MessageDTO message = messageService.create(createDTO);
            
            // Send via WhatsApp if customer has phone and conversation is WhatsApp
            if (customerDTO.getPhone() != null && !customerDTO.getPhone().trim().isEmpty()) {
                try {
                    MessageResult result = messagingService.sendMessage(
                        customerDTO.getPhone(),
                        request.getContent(),
                        currentCompanyId,
                        request.getSenderId()
                    );
                    
                    if (result.isSuccess()) {
                        log.info("Message sent successfully via WhatsApp. External ID: {}", result.getMessageId());
                    } else {
                        log.warn("Failed to send message via WhatsApp: {}", result.getError());
                    }
                } catch (Exception e) {
                    log.error("Error sending message via WhatsApp: {}", e.getMessage(), e);
                }
            }
            
            // Send WebSocket notification for real-time updates
            webSocketNotificationService.notifyNewMessage(message, conversation);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
            
        } catch (IllegalArgumentException e) {
            log.warn("Error sending message: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // File validation helper methods
    private void validateFileUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File must have a valid filename");
        }
        
        // Check for executable file extensions
        String[] dangerousExtensions = {".exe", ".bat", ".cmd", ".scr", ".pif", ".vbs", ".js", ".jar", ".com", ".msi"};
        String lowerFilename = originalFilename.toLowerCase();
        for (String ext : dangerousExtensions) {
            if (lowerFilename.endsWith(ext)) {
                throw new IllegalArgumentException("Executable files are not allowed");
            }
        }
        
        // Check for double extensions (common malware technique)
        String[] parts = originalFilename.split("\\.");
        if (parts.length > 2) {
            // Allow common cases like .tar.gz, but reject suspicious ones like .pdf.exe
            String lastExtension = "." + parts[parts.length - 1].toLowerCase();
            for (String dangerousExt : dangerousExtensions) {
                if (lastExtension.equals(dangerousExt)) {
                    throw new IllegalArgumentException("Files with multiple extensions ending in executable types are not allowed");
                }
            }
        }
    }

    // Media endpoints for conversations
    
    @PostMapping("/{conversationId}/media")
    public ResponseEntity<ConversationMediaDTO> uploadMedia(
            @PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType) {
        
        log.info("📤 [MEDIA UPLOAD] Starting upload for conversation: {}, file: {}, type: {}, size: {} bytes", 
                conversationId, file.getOriginalFilename(), mediaType, file.getSize());
        
        try {
            // Validate file security first
            validateFileUpload(file);
            
            // Debug company context
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            log.info("🏢 [MEDIA UPLOAD] Current company ID: {}", currentCompanyId);
            
            // Validate conversation exists and user has access
            log.debug("🔍 [MEDIA UPLOAD] Validating conversation access...");
            ConversationDTO conversation = conversationService.findById(conversationId, currentCompanyId);
            log.info("✅ [MEDIA UPLOAD] Conversation found: {}", conversation.getId());
            
            // Create media upload request
            log.debug("📋 [MEDIA UPLOAD] Creating media DTO...");
            // Generate a unique file URL (temporary implementation - replace with actual storage service)
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String fileUrl = "/uploads/media/" + currentCompanyId + "/" + conversationId + "/" + fileName;
            
            log.info("📁 [MEDIA UPLOAD] Generated file URL: {}", fileUrl);
            
            CreateConversationMediaDTO createMediaDTO = CreateConversationMediaDTO.builder()
                    .companyId(currentCompanyId)
                    .conversationId(conversationId)
                    .fileUrl(fileUrl)
                    .mediaType(mediaType)
                    .mimeType(file.getContentType())
                    .originalFileName(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .build();
            
            log.info("💾 [MEDIA UPLOAD] Creating media record with DTO: {}", createMediaDTO);
            
            // Create media record first
            ConversationMedia media = conversationMediaService.create(createMediaDTO);
            
            // TODO: Implement actual file storage (S3, GCS, local filesystem, etc.)
            // For now, we're just creating the database record
            log.info("⚠️ [MEDIA UPLOAD] File storage not implemented yet. Only database record created.");
            log.info("✅ [MEDIA UPLOAD] Media created successfully with ID: {}", media.getId());
            
            // Convert to DTO
            ConversationMediaDTO responseDTO = ConversationMediaDTO.builder()
                    .id(media.getId())
                    .companyId(media.getCompany().getId())
                    .companyName(media.getCompany().getName())
                    .conversationId(media.getConversation().getId())
                    .fileUrl(media.getFileUrl())
                    .mediaType(media.getMediaType())
                    .mimeType(media.getMimeType())
                    .originalFileName(media.getOriginalFileName())
                    .fileSizeBytes(media.getFileSizeBytes())
                    .checksum(media.getChecksum())
                    .uploadedByUserId(media.getUploadedByUser() != null ? media.getUploadedByUser().getId() : null)
                    .uploadedByUserName(media.getUploadedByUser() != null ? media.getUploadedByUser().getName() : null)
                    .uploadedByCustomerId(media.getUploadedByCustomer() != null ? media.getUploadedByCustomer().getId() : null)
                    .uploadedByCustomerName(media.getUploadedByCustomer() != null ? media.getUploadedByCustomer().getName() : null)
                    .uploadedAt(media.getUploadedAt())
                    .build();
            
            log.info("🎉 [MEDIA UPLOAD] Upload completed successfully for conversation: {}", conversationId);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ [MEDIA UPLOAD] IllegalArgumentException: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ [MEDIA UPLOAD] Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{conversationId}/media")
    public ResponseEntity<List<ConversationMediaDTO>> getConversationMedia(@PathVariable UUID conversationId) {
        log.debug("Getting media for conversation: {}", conversationId);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            
            // Validate conversation exists and user has access
            conversationService.findById(conversationId, currentCompanyId);
            
            // Get media for conversation
            List<ConversationMedia> mediaList = conversationMediaService.findByConversationId(conversationId);
            
            // Convert to DTOs
            List<ConversationMediaDTO> responseDTOs = mediaList.stream()
                    .map(media -> ConversationMediaDTO.builder()
                            .id(media.getId())
                            .companyId(media.getCompany().getId())
                            .companyName(media.getCompany().getName())
                            .conversationId(media.getConversation().getId())
                            .fileUrl(media.getFileUrl())
                            .mediaType(media.getMediaType())
                            .mimeType(media.getMimeType())
                            .originalFileName(media.getOriginalFileName())
                            .fileSizeBytes(media.getFileSizeBytes())
                            .checksum(media.getChecksum())
                            .uploadedByUserId(media.getUploadedByUser() != null ? media.getUploadedByUser().getId() : null)
                            .uploadedByUserName(media.getUploadedByUser() != null ? media.getUploadedByUser().getName() : null)
                            .uploadedByCustomerId(media.getUploadedByCustomer() != null ? media.getUploadedByCustomer().getId() : null)
                            .uploadedByCustomerName(media.getUploadedByCustomer() != null ? media.getUploadedByCustomer().getName() : null)
                            .uploadedAt(media.getUploadedAt())
                            .build())
                    .toList();
            
            return ResponseEntity.ok(responseDTOs);
            
        } catch (IllegalArgumentException e) {
            log.warn("Error getting conversation media: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error getting conversation media: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{conversationId}/ai-auto-response")
    public ResponseEntity<ConversationDTO> toggleAiAutoResponse(
            @PathVariable UUID conversationId,
            @RequestParam Boolean enabled) {
        
        log.info("Toggling AI auto-response for conversation: {} to: {}", conversationId, enabled);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO conversation = conversationService.toggleAiAutoResponse(conversationId, enabled, currentCompanyId);
            
            log.info("AI auto-response toggled successfully for conversation: {}", conversationId);
            return ResponseEntity.ok(conversation);
            
        } catch (IllegalArgumentException e) {
            log.warn("Error toggling AI auto-response: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error toggling AI auto-response: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{conversationId}/ai-reset-limit")
    public ResponseEntity<ConversationDTO> resetAiMessageLimit(@PathVariable UUID conversationId) {
        log.info("Resetting AI message limit for conversation: {}", conversationId);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO conversation = conversationService.resetAiMessageLimit(conversationId, currentCompanyId);
            
            log.info("AI message limit reset successfully for conversation: {}", conversationId);
            return ResponseEntity.ok(conversation);
            
        } catch (IllegalArgumentException e) {
            log.warn("Error resetting AI message limit: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error resetting AI message limit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    // Request DTO for sending messages
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        @Size(max = 4000, message = "Conteúdo não pode exceder 4000 caracteres")
        private String content;
        
        private UUID senderId;
        @Builder.Default
        private String messageType = "TEXT";
        private String mediaUrl;
    }
}