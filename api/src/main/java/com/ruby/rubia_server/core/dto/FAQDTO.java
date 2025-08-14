package com.ruby.rubia_server.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FAQDTO {
    
    private UUID id;
    private UUID companyId;
    private String question;
    private String answer;
    private List<String> keywords;
    private List<String> triggers;
    private Integer usageCount;
    private Double successRate;
    private Boolean isActive;
    private UUID createdById;
    private String createdByName;
    private UUID lastEditedById;
    private String lastEditedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}