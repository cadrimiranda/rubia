package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageTemplateDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Template content is required")
    private String content;

    @NotNull(message = "AI generated flag is required")
    private Boolean isAiGenerated;

    private UUID aiAgentId;

    @Size(max = 50, message = "Tone must not exceed 50 characters")
    private String tone;
}