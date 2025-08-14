package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageDraftDTO {
    
    @NotNull(message = "ID da empresa é obrigatório")
    private UUID companyId;
    
    @NotNull(message = "ID da conversa é obrigatório") 
    private UUID conversationId;
    
    @NotBlank(message = "Conteúdo do draft é obrigatório")
    private String content;
    
    private String aiModel;
    
    @DecimalMin(value = "0.0", message = "Confiança deve ser entre 0.0 e 1.0")
    @DecimalMax(value = "1.0", message = "Confiança deve ser entre 0.0 e 1.0")
    private Double confidence;
    
    private String sourceType; // "FAQ", "TEMPLATE", "AI_GENERATED"
    
    private UUID sourceId; // ID da FAQ ou Template usado
    
    private String originalMessage; // Mensagem do cliente que gerou este draft
}