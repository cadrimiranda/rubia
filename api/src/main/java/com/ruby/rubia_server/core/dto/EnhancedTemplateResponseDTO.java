package com.ruby.rubia_server.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedTemplateResponseDTO {

    private String originalContent;
    private String enhancedContent;
    private String enhancementType;
    private String aiModelUsed;
    private Integer tokensUsed;
    private Integer creditsConsumed;
    private String explanation; // Explicação do que foi melhorado
}