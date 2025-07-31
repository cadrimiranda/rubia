package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.base.BaseEntity;
import com.ruby.rubia_server.core.enums.RevisionType;
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
@Table(name = "message_template_revisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplateRevision implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // Para multi-tenancy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MessageTemplate template; // Template ao qual esta revisão pertence

    @Column(nullable = false)
    private Integer revisionNumber; // Número da revisão (1 para a original, 2 para a primeira edição, etc.)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // Conteúdo desta revisão específica do template

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_user_id")
    private User editedBy; // Quem criou/editou esta revisão

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "revision_type", nullable = false)
    @Builder.Default
    private RevisionType revisionType = RevisionType.EDIT; // Tipo da revisão (CREATE, EDIT, DELETE, RESTORE)

    // Novos campos para metadados de IA
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_agent_id")
    private AIAgent aiAgent; // Agente de IA usado para esta melhoria (se aplicável)

    @Column(name = "ai_enhancement_type")
    private String aiEnhancementType; // Tipo de melhoria aplicada pela IA (friendly, professional, etc.)

    @Column(name = "ai_tokens_used")
    private Integer aiTokensUsed; // Tokens consumidos pela IA

    @Column(name = "ai_credits_consumed")
    private Integer aiCreditsConsumed; // Créditos consumidos pela IA

    @Column(name = "ai_model_used")
    private String aiModelUsed; // Nome do modelo de IA usado

    @Column(columnDefinition = "TEXT", name = "ai_explanation")
    private String aiExplanation; // Explicação das melhorias aplicadas pela IA

    @CreationTimestamp
    @Column(name = "revision_timestamp", updatable = false)
    private LocalDateTime revisionTimestamp; // Quando esta revisão foi criada

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}