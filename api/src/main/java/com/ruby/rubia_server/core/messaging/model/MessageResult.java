package com.ruby.rubia_server.core.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResult {
    private boolean success;
    private String messageId;
    private String status;
    private String error;
    private String provider;
    
    public static MessageResult success(String messageId, String status, String provider) {
        return MessageResult.builder()
                .success(true)
                .messageId(messageId)
                .status(status)
                .provider(provider)
                .build();
    }
    
    public static MessageResult error(String error, String provider) {
        return MessageResult.builder()
                .success(false)
                .error(error)
                .provider(provider)
                .build();
    }
}