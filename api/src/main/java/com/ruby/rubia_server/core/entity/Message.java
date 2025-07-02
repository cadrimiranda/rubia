package com.ruby.rubia_server.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType; // AGENT, CUSTOMER

    @Column(name = "sender_id")
    private UUID senderId; // ID do User se senderType for AGENT

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType; // TEXT, MEDIA

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "external_message_id")
    private String externalMessageId; // ID da mensagem na plataforma externa (WhatsApp)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status; // SENT, DELIVERED, READ

    @Column(name = "is_ai_generated")
    private Boolean isAiGenerated; // Indica se a mensagem foi gerada por IA

    @Column(name = "ai_confidence")
    private Double aiConfidence; // Confiança da IA na geração da mensagem

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_agent_id")
    private AIAgent aiAgent;

    // NOVO CAMPO: Referência ao MessageTemplate usado para criar esta mensagem
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_template_id") // Esta coluna será nula se a mensagem não for de um template
    private MessageTemplate messageTemplate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

// Enums (apenas para contexto, não são entidades JPA)
enum SenderType {
    AGENT, CUSTOMER
}

enum MessageType {
    TEXT, MEDIA
}

enum MessageStatus {
    SENT, DELIVERED, READ
}