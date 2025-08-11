package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.NotificationType;
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
public class CreateNotificationDTO {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;
    
    @NotNull(message = "Message ID is required")
    private UUID messageId;
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    @NotNull(message = "Title is required")
    private String title;
    
    private String content;
}