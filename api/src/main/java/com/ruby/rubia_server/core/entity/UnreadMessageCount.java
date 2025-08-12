package com.ruby.rubia_server.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unread_message_counts",
       indexes = {
           @Index(name = "idx_unread_user_conversation", columnList = "user_id, conversation_id"),
           @Index(name = "idx_unread_user_company", columnList = "user_id, company_id"),
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_unread_user_conversation", columnNames = {"user_id", "conversation_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadMessageCount {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;

    @Column(name = "last_message_id")
    private UUID lastMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void incrementCount() {
        this.unreadCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void resetCount() {
        this.unreadCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCount(Integer count) {
        this.unreadCount = count;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasUnreadMessages() {
        return this.unreadCount > 0;
    }
}