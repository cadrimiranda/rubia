package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.ConversationChannel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
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
public class CreateConversationDTO {
    
    @NotNull(message = "ID do cliente é obrigatório")
    private UUID customerId;
    
    // CompanyId é obtido do contexto JWT, não enviado pelo frontend
    // @NotNull(message = "ID da empresa é obrigatório")
    // private UUID companyId;
    
    private UUID assignedUserId;
    
    private UUID departmentId;
    
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ENTRADA;
    
    @Builder.Default
    private ConversationChannel channel = ConversationChannel.WHATSAPP;
    
    @Builder.Default
    private Integer priority = 0;
    
    @Builder.Default
    private Boolean isPinned = false;
}