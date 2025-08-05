package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.RevisionType;
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
public class MessageTemplateRevisionDTO {

    private UUID id;
    private UUID templateId;
    private String templateName;
    private Integer revisionNumber;
    private String content;
    private UUID editedByUserId;
    private String editedByUserName;
    private RevisionType revisionType;
    private LocalDateTime revisionTimestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Campos para metadados de IA
    private UUID aiAgentId;
    private String aiAgentName;
    private String aiEnhancementType;
    private Integer aiTokensUsed;
    private Integer aiCreditsConsumed;
    private String aiModelUsed;
    private String aiExplanation;
}