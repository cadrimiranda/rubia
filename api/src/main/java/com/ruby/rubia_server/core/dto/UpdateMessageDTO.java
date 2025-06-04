package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.MessageStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMessageDTO {
    
    @Size(max = 4000, message = "Conteúdo não pode exceder 4000 caracteres")
    private String content;
    
    private String mediaUrl;
    
    private MessageStatus status;
    
    private Boolean isAiGenerated;
    
    private BigDecimal aiConfidence;
}