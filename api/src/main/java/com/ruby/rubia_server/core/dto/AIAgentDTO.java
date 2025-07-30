package com.ruby.rubia_server.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAgentDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private String name;
    private String description;
    private String avatarBase64;
    private UUID aiModelId;
    private String aiModelName;
    private String aiModelDisplayName;
    private String temperament;
    private Integer maxResponseLength;
    private BigDecimal temperature;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}