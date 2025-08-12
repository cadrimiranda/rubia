package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.UnreadMessageCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnreadMessageCountRepository extends JpaRepository<UnreadMessageCount, UUID> {

    /**
     * Find unread count for a specific user and conversation
     */
    Optional<UnreadMessageCount> findByUserIdAndConversationId(UUID userId, UUID conversationId);

    /**
     * Find all unread counts for a user
     */
    List<UnreadMessageCount> findByUserIdAndUnreadCountGreaterThan(UUID userId, Integer count);

    /**
     * Find all unread counts for a user in a company
     */
    List<UnreadMessageCount> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    /**
     * Get total unread count for a user
     */
    @Query("SELECT COALESCE(SUM(u.unreadCount), 0) FROM UnreadMessageCount u WHERE u.user.id = :userId")
    Long getTotalUnreadCountByUserId(@Param("userId") UUID userId);

    /**
     * Get unread count for specific conversation
     */
    @Query("SELECT COALESCE(u.unreadCount, 0) FROM UnreadMessageCount u WHERE u.user.id = :userId AND u.conversation.id = :conversationId")
    Integer getUnreadCountByUserAndConversation(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId);

    /**
     * Get unread counts for multiple conversations (batch)
     */
    @Query("SELECT u.conversation.id, u.unreadCount FROM UnreadMessageCount u WHERE u.user.id = :userId AND u.conversation.id IN :conversationIds")
    List<Object[]> getUnreadCountsForConversations(@Param("userId") UUID userId, @Param("conversationIds") List<UUID> conversationIds);

    /**
     * Reset unread count for a conversation
     */
    @Modifying
    @Query("UPDATE UnreadMessageCount u SET u.unreadCount = 0, u.updatedAt = CURRENT_TIMESTAMP WHERE u.user.id = :userId AND u.conversation.id = :conversationId")
    void resetUnreadCount(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId);

    /**
     * Increment unread count for a conversation
     */
    @Modifying
    @Query("UPDATE UnreadMessageCount u SET u.unreadCount = u.unreadCount + 1, u.lastMessageId = :messageId, u.updatedAt = CURRENT_TIMESTAMP WHERE u.user.id = :userId AND u.conversation.id = :conversationId")
    void incrementUnreadCount(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId, @Param("messageId") UUID messageId);

    /**
     * Reset all unread counts for a user
     */
    @Modifying
    @Query("UPDATE UnreadMessageCount u SET u.unreadCount = 0, u.updatedAt = CURRENT_TIMESTAMP WHERE u.user.id = :userId")
    void resetAllUnreadCounts(@Param("userId") UUID userId);

    /**
     * Delete old records (cleanup)
     */
    @Modifying
    @Query("DELETE FROM UnreadMessageCount u WHERE u.unreadCount = 0 AND u.updatedAt < :cutoffDate")
    void cleanupOldEmptyCounters(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}