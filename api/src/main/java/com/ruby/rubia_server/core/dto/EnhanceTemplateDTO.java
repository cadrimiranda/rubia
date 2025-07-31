package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhanceTemplateDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotBlank(message = "Original template content is required")
    private String originalContent;

    @NotBlank(message = "Enhancement type is required")
    private String enhancementType; // "friendly", "professional", "empathetic", "urgent", "motivational"

    @NotBlank(message = "Template category is required")
    private String category; // Categoria do template para contexto

    private String title; // Título do template para contexto adicional
}