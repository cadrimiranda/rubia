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
public class MessageTemplateRevisionDTO {

    private UUID id;
    private UUID templateId;
    private String templateName;
    private Integer revisionNumber;
    private String content;
    private UUID editedByUserId;
    private String editedByUserName;
    private LocalDateTime revisionTimestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}