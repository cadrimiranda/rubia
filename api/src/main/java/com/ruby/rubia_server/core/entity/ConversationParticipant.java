package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.base.BaseEntity;
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
@Table(name = "conversation_participants",
        uniqueConstraints = {
                // Garante que um Customer, User ou AIAgent só pode ser adicionado uma vez a uma conversa específica
                @UniqueConstraint(columnNames = {"conversation_id", "customer_id"}),
                @UniqueConstraint(columnNames = {"conversation_id", "user_id"}),
                @UniqueConstraint(columnNames = {"conversation_id", "ai_agent_id"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // Para multi-tenancy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation; // A qual conversa este participante pertence

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id") // Opcional: Se o participante é um cliente
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Opcional: Se o participante é um usuário humano (agente)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_agent_id") // Opcional: Se o participante é um agente de IA
    private AIAgent aiAgent;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // Indica se o participante está ativo na conversa (não saiu)

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt; // Quando o participante entrou na conversa

    @Column(name = "left_at")
    private LocalDateTime leftAt; // Quando o participante saiu da conversa (se aplicável)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Importante: A lógica de negócio ou uma restrição de banco de dados (CHECK constraint)
    // deve garantir que APENAS UM dos campos (customer_id, user_id, ai_agent_id) seja não nulo
    // para cada registro de ConversationParticipant.
}