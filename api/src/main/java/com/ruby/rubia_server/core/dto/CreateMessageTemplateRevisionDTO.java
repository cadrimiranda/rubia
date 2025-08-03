package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.Min;
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
public class CreateMessageTemplateRevisionDTO {

    @NotNull(message = "Company ID é obrigatório")
    private UUID companyId;

    @NotNull(message = "Template ID é obrigatório")
    private UUID templateId;

    @NotNull(message = "Número da revisão é obrigatório")
    @Min(value = 1, message = "Número da revisão deve ser maior que 0")
    private Integer revisionNumber;

    @NotBlank(message = "Conteúdo é obrigatório")
    private String content;

    private UUID editedByUserId;
    
    // Campos para metadados de IA
    private UUID aiAgentId;
    private String aiEnhancementType;
    private Integer aiTokensUsed;
    private Integer aiCreditsConsumed;
    private String aiModelUsed;
    private String aiExplanation;
}