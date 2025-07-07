package com.ruby.rubia_server.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_agents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // A qual empresa este agente de IA pertence

    @Column(nullable = false)
    private String name; // Nome do agente de IA (ex: "Rubi", "Atendente Inteligente")

    @Column(columnDefinition = "TEXT")
    private String description; // Descrição do agente (ex: "Especialista em suporte técnico para produtos X.")

    @Column(name = "avatar_url")
    private String avatarUrl; // URL da foto do agente

    @Column(name = "ai_model_type", nullable = false)
    private String aiModelType; // Tipo do modelo de IA (ex: "GPT-4", "Claude 3.5", "Gemini Pro")

    @Column(name = "temperament", nullable = false)
    private String temperament; // Temperamento/Personalidade (ex: "ENGRAÇADO", "SÉRIO", "NORMAL", "EMPATICO")

    @Column(name = "max_response_length")
    @Builder.Default
    private Integer maxResponseLength = 500; // Limite de caracteres para a resposta da IA

    @Column(name = "temperature", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal temperature = BigDecimal.valueOf(0.7); // Parâmetro de criatividade da IA (0.0 a 1.0)

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // Se o agente está ativo e disponível para uso

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}