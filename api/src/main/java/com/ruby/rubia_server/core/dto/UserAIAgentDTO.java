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
public class UserAIAgentDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID userId;
    private String userName;
    private UUID aiAgentId;
    private String aiAgentName;
    private Boolean isDefault;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}