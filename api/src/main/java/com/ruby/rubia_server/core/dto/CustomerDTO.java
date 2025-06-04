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
public class CustomerDTO {
    
    private UUID id;
    private String phone;
    private String name;
    private String whatsappId;
    private String profileUrl;
    private Boolean isBlocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}