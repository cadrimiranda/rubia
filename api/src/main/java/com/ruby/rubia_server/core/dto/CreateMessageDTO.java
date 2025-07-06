package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.enums.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageDTO {
    
    @NotNull(message = "ID da conversa é obrigatório")
    private UUID conversationId;
    
    @NotNull(message = "ID da empresa é obrigatório")
    private UUID companyId;
    
    @NotBlank(message = "Conteúdo da mensagem é obrigatório")
    @Size(max = 4000, message = "Conteúdo não pode exceder 4000 caracteres")
    private String content;
    
    @NotNull(message = "Tipo do remetente é obrigatório")
    private SenderType senderType;
    
    private UUID senderId; // Required when senderType = AGENT
    
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    
    private String mediaUrl;
    
    private String externalMessageId;
    
    @Builder.Default
    private Boolean isAiGenerated = false;
    
    private Double aiConfidence;
}