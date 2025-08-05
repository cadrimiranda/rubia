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
@Table(name = "ai_models")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // Nome técnico do modelo (ex: "gpt-4.1", "gpt-4o-mini", "o3")

    @Column(name = "display_name", nullable = false)
    private String displayName; // Nome amigável para exibição (ex: "GPT-4.1", "GPT-4 Mini", "O3")

    @Column(columnDefinition = "TEXT")
    private String description; // Descrição básica do modelo

    @Column(name = "capabilities", columnDefinition = "TEXT")
    private String capabilities; // Descrição detalhada das capacidades do modelo

    @Column(name = "impact_description", columnDefinition = "TEXT")
    private String impactDescription; // Descrição do impacto e casos de uso recomendados

    @Column(name = "cost_per_1k_tokens")
    private Integer costPer1kTokens; // Custo em créditos por 1000 tokens

    @Column(name = "performance_level")
    private String performanceLevel; // Nível de performance: "BASICO", "INTERMEDIARIO", "AVANCADO", "PREMIUM"

    @Column(name = "provider", nullable = false)
    private String provider; // Provedor do modelo (ex: "OpenAI", "Anthropic", "Google")

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // Se o modelo está ativo e disponível para uso

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0; // Ordem de exibição na lista

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}