package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.ConversationChannel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationDTO {
    
    private UUID assignedUserId;
    
    private UUID departmentId;
    
    private ConversationStatus status;
    
    private ConversationChannel channel;
    
    private Integer priority;
    
    private Boolean isPinned;
}