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

/**
 * Entidade para auditoria das melhorias de mensagens feitas com IA em tempo real
 * Registra todas as interações de enhancement para fins de auditoria e análise
 */
@Entity
@Table(name = "message_enhancement_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEnhancementAudit implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // Para multi-tenancy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Usuário que solicitou a melhoria

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_agent_id", nullable = false)
    private AIAgent aiAgent; // Agente de IA usado para esta melhoria

    @Column(name = "conversation_id")
    private UUID conversationId; // ID da conversa onde a melhoria foi aplicada (opcional)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalMessage; // Mensagem original antes da melhoria

    @Column(columnDefinition = "TEXT", nullable = false)
    private String enhancedMessage; // Mensagem melhorada pela IA

    @Column(name = "temperament_used", nullable = false)
    private String temperamentUsed; // Temperamento do agente usado

    @Column(name = "ai_model_used", nullable = false)
    private String aiModelUsed; // Nome do modelo de IA usado (ex: gpt-4o-mini)

    @Column(name = "temperature_used", nullable = false)
    private Double temperatureUsed; // Temperatura usada na geração

    @Column(name = "max_tokens_used")
    private Integer maxTokensUsed; // Máximo de tokens configurado

    @Column(name = "tokens_consumed")
    private Integer tokensConsumed; // Tokens realmente consumidos (se disponível)

    @Column(name = "response_time_ms")
    private Long responseTimeMs; // Tempo de resposta da API em millisegundos

    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true; // Se a melhoria foi bem-sucedida

    @Column(name = "error_message")
    private String errorMessage; // Mensagem de erro (se aplicável)

    @Column(name = "user_agent")
    private String userAgent; // User agent do navegador

    @Column(name = "ip_address")
    private String ipAddress; // IP do usuário

    // Payload enviado para OpenAI (para auditoria e debugging)
    @Column(columnDefinition = "TEXT", name = "openai_system_message")
    private String openaiSystemMessage; // System message enviada à OpenAI

    @Column(columnDefinition = "TEXT", name = "openai_user_message")
    private String openaiUserMessage; // User message (prompt) enviada à OpenAI

    @Column(columnDefinition = "TEXT", name = "openai_full_payload")
    private String openaiFullPayload; // JSON completo do payload enviado (opcional, para debugging)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Métodos de conveniência
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    public boolean isFailed() {
        return Boolean.FALSE.equals(success);
    }
}