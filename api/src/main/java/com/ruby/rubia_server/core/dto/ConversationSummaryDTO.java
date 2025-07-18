package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDTO {
    
    private UUID id;
    private String customerName;
    private String customerPhone;
    private String assignedUserName;
    private ConversationStatus status;
    private Channel channel;
    private Integer priority;
    private LocalDateTime updatedAt;
    private Long unreadCount;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
}