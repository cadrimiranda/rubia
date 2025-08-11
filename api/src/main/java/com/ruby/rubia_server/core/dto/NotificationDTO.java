package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.NotificationStatus;
import com.ruby.rubia_server.core.enums.NotificationType;
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
public class NotificationDTO {
    private UUID id;
    private UUID userId;
    private UUID conversationId;
    private UUID messageId;
    private NotificationType type;
    private NotificationStatus status;
    private String title;
    private String content;
    private LocalDateTime readAt;
    private UUID companyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for frontend display
    private String customerName;
    private String conversationTitle;
    private boolean isRead;
}