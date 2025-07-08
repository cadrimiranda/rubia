package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.SenderType;
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
public class MessageDTO {
    
    private UUID id;
    private UUID companyId;
    private UUID conversationId;
    private String content;
    private SenderType senderType;
    private UUID senderId;
    private String senderName;
    private String externalMessageId;
    private Boolean isAiGenerated;
    private Double aiConfidence;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    
    // Frontend compatibility fields
    private String timestamp; // ISO string for frontend
    private Boolean isFromUser; // Computed based on senderType
}