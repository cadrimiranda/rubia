package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private UUID companyId;
    private UUID departmentId;
    private String departmentName;
    private UserRole role;
    private String avatarUrl;
    private Boolean isOnline;
    private LocalDateTime lastSeen;
    private LocalDate birthDate;
    private Double weight;
    private Double height;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}