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
public class DepartmentDTO {
    
    private UUID id;
    private UUID companyId;
    private String name;
    private String description;
    private Boolean autoAssign;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}