package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.UnreadMessageCount;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UnreadMessageCountRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UnreadMessageCountService {

    private final UnreadMessageCountRepository unreadCountRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    /**
     * Increment unread count when a new message arrives
     */
    public void incrementUnreadCount(UUID userId, UUID conversationId, UUID messageId) {
        log.debug("Incrementing unread count for user {} in conversation {}", userId, conversationId);
        
        var existingCount = unreadCountRepository.findByUserIdAndConversationId(userId, conversationId);
        
        if (existingCount.isPresent()) {
            // Update existing record
            unreadCountRepository.incrementUnreadCount(userId, conversationId, messageId);
            // Get updated count and notify via WebSocket
            Integer newCount = getUnreadCount(userId, conversationId);
            webSocketNotificationService.sendUnreadCountUpdate(userId, conversationId, newCount);
        } else {
            // Create new record
            createUnreadCount(userId, conversationId, messageId, 1);
            // Notify via WebSocket
            webSocketNotificationService.sendUnreadCountUpdate(userId, conversationId, 1);
        }
    }

    /**
     * Reset unread count when user reads the conversation
     */
    public void markAsRead(UUID userId, UUID conversationId) {
        log.debug("Marking conversation {} as read for user {}", conversationId, userId);
        
        unreadCountRepository.resetUnreadCount(userId, conversationId);
        
        // Notify via WebSocket that count is now 0
        webSocketNotificationService.sendUnreadCountUpdate(userId, conversationId, 0);
    }

    /**
     * Reset all unread counts for a user
     */
    public void markAllAsRead(UUID userId) {
        log.debug("Marking all conversations as read for user {}", userId);
        
        unreadCountRepository.resetAllUnreadCounts(userId);
    }

    /**
     * Get unread count for a specific conversation
     */
    @Transactional(readOnly = true)
    public Integer getUnreadCount(UUID userId, UUID conversationId) {
        return unreadCountRepository.getUnreadCountByUserAndConversation(userId, conversationId);
    }

    /**
     * Get total unread count for a user
     */
    @Transactional(readOnly = true)
    public Long getTotalUnreadCount(UUID userId) {
        return unreadCountRepository.getTotalUnreadCountByUserId(userId);
    }

    /**
     * Get unread counts for multiple conversations
     */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> getUnreadCountsForConversations(UUID userId, List<UUID> conversationIds) {
        List<Object[]> results = unreadCountRepository.getUnreadCountsForConversations(userId, conversationIds);
        
        Map<UUID, Integer> countsMap = new HashMap<>();
        for (Object[] result : results) {
            UUID conversationId = (UUID) result[0];
            Integer count = (Integer) result[1];
            countsMap.put(conversationId, count);
        }
        
        // Add missing conversations with count 0
        for (UUID conversationId : conversationIds) {
            countsMap.putIfAbsent(conversationId, 0);
        }
        
        return countsMap;
    }

    /**
     * Get all conversations with unread messages for a user
     */
    @Transactional(readOnly = true)
    public List<UnreadMessageCount> getUnreadConversations(UUID userId) {
        return unreadCountRepository.findByUserIdAndUnreadCountGreaterThan(userId, 0);
    }

    /**
     * Create notifications for all users in company when message arrives
     */
    public void createUnreadCountsForNewMessage(UUID messageId, UUID conversationId, Company company) {
        log.debug("Creating unread counts for message {} in conversation {}", messageId, conversationId);
        
        try {
            List<User> companyUsers = userRepository.findByCompanyId(company.getId());
            
            for (User user : companyUsers) {
                incrementUnreadCount(user.getId(), conversationId, messageId);
            }
            
        } catch (Exception e) {
            log.error("Error creating unread counts for message {}: {}", messageId, e.getMessage(), e);
        }
    }

    /**
     * Cleanup old empty counters
     */
    public void cleanupOldCounters() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // Remove empty counters older than 30 days
        unreadCountRepository.cleanupOldEmptyCounters(cutoffDate);
        log.debug("Cleaned up old empty unread counters before {}", cutoffDate);
    }

    /**
     * Helper method to create new unread count record
     */
    private void createUnreadCount(UUID userId, UUID conversationId, UUID messageId, Integer count) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
            
            UnreadMessageCount unreadCount = new UnreadMessageCount();
            unreadCount.setUser(user);
            unreadCount.setConversation(conversation);
            unreadCount.setCompany(user.getCompany());
            unreadCount.setUnreadCount(count);
            unreadCount.setLastMessageId(messageId);
            
            unreadCountRepository.save(unreadCount);
            
        } catch (Exception e) {
            log.error("Error creating unread count for user {} in conversation {}: {}", 
                    userId, conversationId, e.getMessage(), e);
        }
    }
}