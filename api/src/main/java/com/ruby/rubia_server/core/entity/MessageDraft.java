package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_drafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageDraft extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "ai_model", length = 50)
    private String aiModel;
    
    @Column(name = "confidence", precision = 3, scale = 2)
    private Double confidence;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DraftStatus status = DraftStatus.PENDING;
    
    @Column(name = "source_type", length = 50)
    private String sourceType; // "FAQ", "TEMPLATE", "AI_GENERATED"
    
    @Column(name = "source_id")
    private UUID sourceId; // ID da FAQ ou Template usado
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "original_message", columnDefinition = "TEXT")
    private String originalMessage; // Mensagem do cliente que gerou este draft
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // Motivo da rejeição pelo operador
    
    // Método para aprovar draft
    public void approve(User reviewer) {
        this.status = DraftStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }
    
    // Método para rejeitar draft
    public void reject(User reviewer, String reason) {
        this.status = DraftStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }
    
    // Método para editar draft
    public void edit(User reviewer, String newContent) {
        this.status = DraftStatus.EDITED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.content = newContent;
    }
    
    // Helper methods
    public boolean isPending() {
        return DraftStatus.PENDING.equals(this.status);
    }
    
    public boolean isApproved() {
        return DraftStatus.APPROVED.equals(this.status);
    }
    
    public boolean isRejected() {
        return DraftStatus.REJECTED.equals(this.status);
    }
    
    public boolean isEdited() {
        return DraftStatus.EDITED.equals(this.status);
    }
    
    public boolean canBeReviewed() {
        return isPending();
    }
}