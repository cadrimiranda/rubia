package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.enums.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    
    private UUID id;
    private UUID conversationId;
    private String content;
    private SenderType senderType;
    private UUID senderId;
    private String senderName;
    private MessageType messageType;
    private String mediaUrl;
    private String externalMessageId;
    private Boolean isAiGenerated;
    private BigDecimal aiConfidence;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    
    // Frontend compatibility fields
    private String timestamp; // ISO string for frontend
    private Boolean isFromUser; // Computed based on senderType
}