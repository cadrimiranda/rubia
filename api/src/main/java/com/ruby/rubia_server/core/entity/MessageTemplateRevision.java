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
@Table(name = "message_template_revisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplateRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    @CreationTimestamp
    @Column(name = "revision_timestamp", updatable = false)
    private LocalDateTime revisionTimestamp; // Quando esta revisão foi criada
}