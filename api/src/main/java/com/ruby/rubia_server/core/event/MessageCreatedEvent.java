package com.ruby.rubia_server.core.event;

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
public class MessageCreatedEvent {

    private UUID messageId;
    private UUID conversationId;
    private String content;
    private LocalDateTime createdAt;
}