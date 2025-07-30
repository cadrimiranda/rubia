// src/main/java/com/ruby/rubia_server/core/entity/Conversation.java
package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.ConversationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "channel", nullable = false)
    private Channel channel; // WHATSAPP, etc.

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private ConversationStatus status; // ENTRADA, ESPERANDO, FINALIZADOS

    @Column(name = "priority")
    private Integer priority; // 1-5, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser; // Usuário humano atribuído a esta conversa (pode ser nulo para grupos ou não atribuídas)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User ownerUser; // Usuário humano que criou/é o "dono" da conversa (pode ser nulo)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign; // Opcional, para indicar se a conversa faz parte de uma campanha

    // NOVO CAMPO: Tipo da conversa (individual ou grupo)
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "conversation_type", nullable = false)
    @Builder.Default
    private ConversationType conversationType = ConversationType.ONE_TO_ONE;

    // Campo para vincular com o chatLid da Z-API
    @Column(name = "chat_lid", unique = true)
    private String chatLid;

    // Remove a relação direta com Customer.
    // Os participantes (incluindo Customers) são agora gerenciados pela entidade ConversationParticipant.
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConversationParticipant> participants = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}