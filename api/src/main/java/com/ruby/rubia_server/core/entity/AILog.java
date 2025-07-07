package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.enums.AILogStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AILog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // A qual empresa esta interação de IA pertence

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_agent_id", nullable = false)
    private AIAgent aiAgent; // Qual agente de IA foi utilizado nesta interação

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Opcional: Qual usuário humano disparou a interação da IA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation; // Opcional: A qual conversa a interação da IA estava ligada

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message; // Opcional: A qual mensagem a IA gerou/processou

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_template_id")
    private MessageTemplate messageTemplate; // Opcional: A qual template a IA gerou/processou

    @Column(name = "request_prompt", columnDefinition = "TEXT", nullable = false)
    private String requestPrompt; // O prompt enviado à API da IA

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse; // A resposta bruta recebida da API da IA

    @Column(name = "processed_response", columnDefinition = "TEXT")
    private String processedResponse; // A resposta da IA após ser processada/parseada pelo sistema

    @Column(name = "tokens_used_input")
    private Integer tokensUsedInput; // Número de tokens usados no prompt de entrada

    @Column(name = "tokens_used_output")
    private Integer tokensUsedOutput; // Número de tokens usados na resposta de saída

    @Column(name = "estimated_cost", precision = 10, scale = 8) // Custos podem ser muito pequenos (ex: $0.00000020)
    private BigDecimal estimatedCost; // Custo estimado desta chamada à API da IA

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AILogStatus status; // Sucesso, Falha, Parcial, etc.

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Mensagem de erro, se a interação falhou

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // Timestamp da interação
}