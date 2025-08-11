package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateNotificationDTO;
import com.ruby.rubia_server.core.dto.NotificationDTO;
import com.ruby.rubia_server.core.dto.NotificationSummaryDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.NotificationStatus;
import com.ruby.rubia_server.core.enums.NotificationType;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CompanyRepository companyRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * Create a new notification for a user when they receive a new message
     */
    public NotificationDTO createMessageNotification(UUID userId, UUID conversationId, UUID messageId) {
        log.debug("Creating message notification for user {} in conversation {}", userId, conversationId);
        
        // Validate entities exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        
        // Don't create notification for user's own messages
        if (message.getSenderType() == SenderType.AGENT && message.getSenderId() != null && message.getSenderId().equals(userId)) {
            log.debug("Skipping notification for user's own message");
            return null;
        }
        
        // Check if notification already exists for this message and user
        var existingNotification = notificationRepository.findByUserIdAndConversationIdAndMessageId(
                userId, conversationId, messageId);
        
        if (existingNotification.isPresent()) {
            log.debug("Notification already exists for user {} and message {}", userId, messageId);
            return toDTO(existingNotification.get());
        }
        
        // Create notification
        String customerName = getCustomerNameFromConversation(conversation);
        
        String title = "Nova mensagem de " + customerName;
        String content = message.getContent() != null ? 
                message.getContent().substring(0, Math.min(100, message.getContent().length())) :
                "Nova mensagem";
        
        Notification notification = Notification.builder()
                .user(user)
                .conversation(conversation)
                .message(message)
                .type(NotificationType.NEW_MESSAGE)
                .status(NotificationStatus.UNREAD)
                .title(title)
                .content(content)
                .company(user.getCompany())
                .build();
        
        notification = notificationRepository.save(notification);
        
        NotificationDTO dto = toDTO(notification);
        
        // Send real-time notification via WebSocket
        webSocketNotificationService.sendNotificationToUser(userId, dto);
        
        log.info("Created notification {} for user {} in conversation {}", 
                notification.getId(), userId, conversationId);
        
        return dto;
    }

    /**
     * Get all notifications for a user
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsByUser(UUID userId, Pageable pageable) {
        log.debug("Getting notifications for user {} with pagination", userId);
        
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::toDTO);
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUnreadNotificationsByUser(UUID userId, Pageable pageable) {
        log.debug("Getting unread notifications for user {}", userId);
        
        Page<Notification> notifications = notificationRepository.findUnreadByUserId(userId, pageable);
        return notifications.map(this::toDTO);
    }

    /**
     * Get notification count by conversation for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadCountByUserAndConversation(UUID userId, UUID conversationId) {
        return notificationRepository.countUnreadByUserIdAndConversationId(userId, conversationId);
    }

    /**
     * Get total unread notification count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadCountByUser(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Get notification summary grouped by conversation
     */
    @Transactional(readOnly = true)
    public List<NotificationSummaryDTO> getNotificationSummaryByUser(UUID userId) {
        log.debug("Getting notification summary for user {}", userId);
        
        List<Object[]> results = notificationRepository.getUnreadNotificationSummaryByUserId(userId);
        
        return results.stream().map(result -> {
            UUID conversationId = (UUID) result[0];
            Long count = (Long) result[1];
            LocalDateTime lastNotification = (LocalDateTime) result[2];
            
            // Get conversation details
            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            String customerName = conversation != null ? getCustomerNameFromConversation(conversation) : null;
            String conversationTitle = customerName != null ? customerName : "Conversa";
            
            return NotificationSummaryDTO.builder()
                    .conversationId(conversationId)
                    .conversationTitle(conversationTitle)
                    .customerName(customerName)
                    .count(count)
                    .lastNotification(lastNotification)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Mark notifications as read when user opens a conversation
     */
    public void markAsReadByConversation(UUID userId, UUID conversationId) {
        log.debug("Marking notifications as read for user {} in conversation {}", userId, conversationId);
        
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.markAsReadByUserIdAndConversationId(userId, conversationId, now);
        
        // Send WebSocket update to reflect the change
        webSocketNotificationService.sendNotificationCountUpdate(userId);
        
        log.info("Marked notifications as read for user {} in conversation {}", userId, conversationId);
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(UUID userId) {
        log.debug("Marking all notifications as read for user {}", userId);
        
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.markAllAsReadByUserId(userId, now);
        
        // Send WebSocket update
        webSocketNotificationService.sendNotificationCountUpdate(userId);
        
        log.info("Marked all notifications as read for user {}", userId);
    }

    /**
     * Delete notifications for a conversation (when conversation is deleted)
     */
    public void deleteByConversation(UUID userId, UUID conversationId) {
        log.debug("Soft deleting notifications for user {} in conversation {}", userId, conversationId);
        
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.softDeleteByUserIdAndConversationId(userId, conversationId, now);
        
        // Send WebSocket update
        webSocketNotificationService.sendNotificationCountUpdate(userId);
        
        log.info("Soft deleted notifications for user {} in conversation {}", userId, conversationId);
    }

    /**
     * Clean up old notifications (can be scheduled to run periodically)
     */
    @Transactional
    public void cleanupOldNotifications(int daysOld) {
        log.debug("Cleaning up notifications older than {} days", daysOld);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteOldNotifications(cutoffDate);
        
        log.info("Cleaned up notifications older than {}", cutoffDate);
    }

    /**
     * Helper method to get customer name from conversation
     */
    private String getCustomerNameFromConversation(Conversation conversation) {
        // Since Conversation doesn't have a direct customerName field,
        // we need to get it from the participants
        try {
            return conversation.getParticipants().stream()
                    .filter(participant -> participant.getCustomer() != null)
                    .map(participant -> participant.getCustomer().getName())
                    .findFirst()
                    .orElse("Cliente");
        } catch (Exception e) {
            log.warn("Error getting customer name from conversation {}: {}", conversation.getId(), e.getMessage());
            return "Cliente";
        }
    }

    /**
     * Convert entity to DTO
     */
    private NotificationDTO toDTO(Notification notification) {
        String customerName = getCustomerNameFromConversation(notification.getConversation());
        String conversationTitle = customerName != null ? customerName : "Conversa";
        
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .conversationId(notification.getConversation().getId())
                .messageId(notification.getMessage().getId())
                .type(notification.getType())
                .status(notification.getStatus())
                .title(notification.getTitle())
                .content(notification.getContent())
                .readAt(notification.getReadAt())
                .companyId(notification.getCompany().getId())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .customerName(customerName)
                .conversationTitle(conversationTitle)
                .isRead(notification.isRead())
                .build();
    }
}