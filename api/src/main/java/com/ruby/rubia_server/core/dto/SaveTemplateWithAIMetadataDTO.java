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
public class SaveTemplateWithAIMetadataDTO {

    @NotNull(message = "Template ID é obrigatório")
    private UUID templateId;

    @NotBlank(message = "Conteúdo é obrigatório")
    private String content;
    
    @NotNull(message = "User ID é obrigatório")
    private UUID userId;

    // Metadados de IA
    private UUID aiAgentId;
    
    @NotBlank(message = "Tipo de melhoria da IA é obrigatório")
    private String aiEnhancementType;
    
    private Integer aiTokensUsed;
    private Integer aiCreditsConsumed;
    private String aiModelUsed;
    private String aiExplanation;
}