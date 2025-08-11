package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Notification;
import com.ruby.rubia_server.core.enums.NotificationStatus;
import com.ruby.rubia_server.core.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    // Find notifications by user
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
    
    // Find unread notifications by user
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.status = 'UNREAD' AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.status = 'UNREAD' AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    // Count unread notifications by user
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.status = 'UNREAD' AND n.deletedAt IS NULL")
    long countUnreadByUserId(@Param("userId") UUID userId);
    
    // Find notifications by conversation and user
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.conversation.id = :conversationId AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndConversationId(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId);
    
    // Count notifications by conversation and user
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.conversation.id = :conversationId AND n.status = 'UNREAD' AND n.deletedAt IS NULL")
    long countUnreadByUserIdAndConversationId(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId);
    
    // Find notifications by type and user
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndType(@Param("userId") UUID userId, @Param("type") NotificationType type);
    
    // Find notifications by status and user
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.status = :status AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") NotificationStatus status);
    
    // Find notifications by company
    @Query("SELECT n FROM Notification n WHERE n.company.id = :companyId AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findByCompanyId(@Param("companyId") UUID companyId);
    
    // Check if notification exists for user, conversation and message
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.conversation.id = :conversationId AND n.message.id = :messageId AND n.deletedAt IS NULL")
    Optional<Notification> findByUserIdAndConversationIdAndMessageId(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId, @Param("messageId") UUID messageId);
    
    // Mark notifications as read by conversation
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.user.id = :userId AND n.conversation.id = :conversationId AND n.status = 'UNREAD' AND n.deletedAt IS NULL")
    void markAsReadByUserIdAndConversationId(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId, @Param("readAt") LocalDateTime readAt);
    
    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'read', n.readAt = :readAt WHERE n.user.id = :userId AND n.status = 'UNREAD' AND n.deletedAt IS NULL")
    void markAllAsReadByUserId(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
    
    // Soft delete notifications by conversation
    @Modifying
    @Query("UPDATE Notification n SET n.deletedAt = :deletedAt WHERE n.user.id = :userId AND n.conversation.id = :conversationId AND n.deletedAt IS NULL")
    void softDeleteByUserIdAndConversationId(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId, @Param("deletedAt") LocalDateTime deletedAt);
    
    // Clean up old notifications (older than specified date)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Get notification summary by user (grouped by conversation)
    @Query("""
        SELECT n.conversation.id as conversationId, 
               COUNT(n) as count, 
               MAX(n.createdAt) as lastNotification
        FROM Notification n 
        WHERE n.user.id = :userId AND n.status = 'UNREAD' AND n.deletedAt IS NULL 
        GROUP BY n.conversation.id 
        ORDER BY lastNotification DESC
    """)
    List<Object[]> getUnreadNotificationSummaryByUserId(@Param("userId") UUID userId);
}