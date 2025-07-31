package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.ConversationType;
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
    
    private UUID assignedUserId;
    
    private UUID departmentId;
    
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ENTRADA;
    
    @Builder.Default
    private Channel channel = Channel.WHATSAPP;
    
    @Builder.Default
    private Integer priority = 0;
    
    @Builder.Default
    private Boolean isPinned = false;
    
    private UUID campaignId;
    
    @Builder.Default
    private ConversationType conversationType = ConversationType.ONE_TO_ONE;
    
    private String chatLid;
}