package com.ruby.rubia_server.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftStatsDTO {
    
    private UUID companyId;
    private long totalDrafts;
    private long pendingDrafts;
    private long approvedDrafts;
    private long rejectedDrafts;
    private long editedDrafts;
    private double approvalRate; // Taxa de aprovação (approved / total)
    private double avgConfidence; // Confiança média dos drafts
    private String mostUsedAiModel;
    private String mostUsedSourceType;
    
    // Computed properties
    public double getRejectionRate() {
        return totalDrafts > 0 ? (double) rejectedDrafts / totalDrafts * 100 : 0;
    }
    
    public double getEditRate() {
        return totalDrafts > 0 ? (double) editedDrafts / totalDrafts * 100 : 0;
    }
    
    public String getApprovalRateFormatted() {
        return String.format("%.1f%%", approvalRate);
    }
    
    public String getAvgConfidenceFormatted() {
        return String.format("%.1f%%", avgConfidence * 100);
    }
}