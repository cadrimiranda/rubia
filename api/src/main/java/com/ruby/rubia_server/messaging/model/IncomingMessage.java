package com.ruby.rubia_server.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMessage {
    private String messageId;
    private String from;
    private String to;
    private String body;
    private String mediaUrl;
    private String mediaType;
    private LocalDateTime timestamp;
    private String provider;
    private Object rawPayload;
}