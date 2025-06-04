package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.UserRole;
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
public class UserDTO {
    
    private UUID id;
    private String name;
    private String email;
    private UUID departmentId;
    private String departmentName;
    private UserRole role;
    private String avatarUrl;
    private Boolean isOnline;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}