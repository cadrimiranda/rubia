package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.NotificationDTO;
import com.ruby.rubia_server.core.dto.NotificationSummaryDTO;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class NotificationController {
    
    private final NotificationService notificationService;
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

    /**
     * Get all notifications for the current user
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            Principal principal) {
        log.debug("Getting notifications for user: {}", principal.getName());
        
        UUID userId = getUserIdFromPrincipal(principal);
        Page<NotificationDTO> notifications = notificationService.getNotificationsByUser(userId, pageable);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for the current user
     */
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationDTO>> getUnreadNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            Principal principal) {
        log.debug("Getting unread notifications for user: {}", principal.getName());
        
        UUID userId = getUserIdFromPrincipal(principal);
        Page<NotificationDTO> notifications = notificationService.getUnreadNotificationsByUser(userId, pageable);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notification count for a specific conversation
     */
    @GetMapping("/count/conversation/{conversationId}")
    public ResponseEntity<Map<String, Long>> getNotificationCountByConversation(
            @PathVariable UUID conversationId,
            Principal principal) {
        log.debug("Getting notification count for user {} in conversation {}", 
                principal.getName(), conversationId);
        
        UUID userId = getUserIdFromPrincipal(principal);
        long count = notificationService.getUnreadCountByUserAndConversation(userId, conversationId);
        
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get total unread notification count for the current user
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalNotificationCount(Principal principal) {
        log.debug("Getting total notification count for user: {}", principal.getName());
        
        UUID userId = getUserIdFromPrincipal(principal);
        long count = notificationService.getUnreadCountByUser(userId);
        
        return ResponseEntity.ok(Map.of("totalCount", count));
    }

    /**
     * Get notification summary grouped by conversation
     */
    @GetMapping("/summary")
    public ResponseEntity<List<NotificationSummaryDTO>> getNotificationSummary(Principal principal) {
        log.debug("Getting notification summary for user: {}", principal.getName());
        
        UUID userId = getUserIdFromPrincipal(principal);
        List<NotificationSummaryDTO> summary = notificationService.getNotificationSummaryByUser(userId);
        
        return ResponseEntity.ok(summary);
    }

    /**
     * Mark notifications as read for a specific conversation
     */
    @PutMapping("/conversation/{conversationId}/read")
    public ResponseEntity<Void> markConversationNotificationsAsRead(
            @PathVariable UUID conversationId,
            Principal principal) {
        log.debug("Marking notifications as read for user {} in conversation {}", 
                principal.getName(), conversationId);
        
        UUID userId = getUserIdFromPrincipal(principal);
        notificationService.markAsReadByConversation(userId, conversationId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for the current user
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(Principal principal) {
        log.debug("Marking all notifications as read for user: {}", principal.getName());
        
        UUID userId = getUserIdFromPrincipal(principal);
        notificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Delete notifications for a specific conversation
     */
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> deleteConversationNotifications(
            @PathVariable UUID conversationId,
            Principal principal) {
        log.debug("Deleting notifications for user {} in conversation {}", 
                principal.getName(), conversationId);
        
        UUID userId = getUserIdFromPrincipal(principal);
        notificationService.deleteByConversation(userId, conversationId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get counts for multiple conversations (batch request)
     */
    @PostMapping("/counts/conversations")
    public ResponseEntity<Map<UUID, Long>> getNotificationCountsForConversations(
            @RequestBody List<UUID> conversationIds,
            Principal principal) {
        log.debug("Getting notification counts for {} conversations for user {}", 
                conversationIds.size(), principal.getName());
        
        UUID userId = getUserIdFromPrincipal(principal);
        
        Map<UUID, Long> counts = conversationIds.stream()
                .collect(java.util.stream.Collectors.toMap(
                        conversationId -> conversationId,
                        conversationId -> notificationService.getUnreadCountByUserAndConversation(userId, conversationId)
                ));
        
        return ResponseEntity.ok(counts);
    }
}