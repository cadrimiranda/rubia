package com.ruby.rubia_server.core.dto;

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
public class NotificationSummaryDTO {
    private UUID conversationId;
    private String conversationTitle;
    private String customerName;
    private long count;
    private LocalDateTime lastNotification;
    private String lastMessageContent;
}