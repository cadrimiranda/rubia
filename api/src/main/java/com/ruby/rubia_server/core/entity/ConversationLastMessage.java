package com.ruby.rubia_server.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_last_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationLastMessage {

    @Id
    @Column(name = "conversation_id")
    private UUID conversationId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "last_message_date", nullable = false)
    private LocalDateTime lastMessageDate;

    @Column(name = "last_message_id")
    private UUID lastMessageId;

    @Column(name = "last_message_content", columnDefinition = "TEXT")
    private String lastMessageContent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}