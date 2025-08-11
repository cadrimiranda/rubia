package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.service.UnreadMessageCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/unread-counts")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class UnreadMessageCountController {
    
    private final UnreadMessageCountService unreadCountService;
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
     * Get unread count for a specific conversation
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @PathVariable UUID conversationId,
            Principal principal) {
        
        UUID userId = getUserIdFromPrincipal(principal);
        Integer count = unreadCountService.getUnreadCount(userId, conversationId);
        
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get total unread count for the current user
     */
    @GetMapping("/total")
    public ResponseEntity<Map<String, Long>> getTotalUnreadCount(Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        Long count = unreadCountService.getTotalUnreadCount(userId);
        
        return ResponseEntity.ok(Map.of("totalCount", count));
    }

    /**
     * Get unread counts for multiple conversations (batch request)
     */
    @PostMapping("/conversations")
    public ResponseEntity<Map<UUID, Integer>> getUnreadCountsForConversations(
            @RequestBody List<UUID> conversationIds,
            Principal principal) {
        
        UUID userId = getUserIdFromPrincipal(principal);
        Map<UUID, Integer> counts = unreadCountService.getUnreadCountsForConversations(userId, conversationIds);
        
        return ResponseEntity.ok(counts);
    }

    /**
     * Mark conversation as read (reset counter)
     */
    @PutMapping("/conversation/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            Principal principal) {
        
        UUID userId = getUserIdFromPrincipal(principal);
        unreadCountService.markAsRead(userId, conversationId);
        
        log.debug("Marked conversation {} as read for user {}", conversationId, principal.getName());
        
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all conversations as read for the current user
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        unreadCountService.markAllAsRead(userId);
        
        log.debug("Marked all conversations as read for user {}", principal.getName());
        
        return ResponseEntity.ok().build();
    }
}