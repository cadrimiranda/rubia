package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.entity.DraftStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDraftDTO {
    
    private UUID id;
    private UUID companyId;
    private UUID conversationId;
    private String content;
    private String aiModel;
    private Double confidence;
    private DraftStatus status;
    private String sourceType;
    private UUID sourceId;
    private UUID createdById;
    private String createdByName;
    private UUID reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String originalMessage;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper methods for frontend
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
    
    public String getConfidencePercentage() {
        if (confidence == null) return "0%";
        return Math.round(confidence * 100) + "%";
    }
    
    public String getStatusDisplay() {
        switch (status) {
            case PENDING: return "Pendente";
            case APPROVED: return "Aprovado";
            case REJECTED: return "Rejeitado";
            case EDITED: return "Editado";
            default: return status.name();
        }
    }
}