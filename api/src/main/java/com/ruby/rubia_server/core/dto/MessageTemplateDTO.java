package com.ruby.rubia_server.core.dto;

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
public class MessageTemplateDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private String name;
    private String content;
    private Boolean isAiGenerated;
    private UUID createdByUserId;
    private String createdByUserName;
    private UUID aiAgentId;
    private String aiAgentName;
    private String tone;
    private UUID lastEditedByUserId;
    private String lastEditedByUserName;
    private Integer editCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}