package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAIAgentDTO {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

    @Size(max = 100, message = "AI Model Type must not exceed 100 characters")
    private String aiModelType;

    @Size(max = 50, message = "Temperament must not exceed 50 characters")
    private String temperament;

    @Min(value = 1, message = "Max response length must be at least 1")
    @Max(value = 10000, message = "Max response length must not exceed 10000")
    private Integer maxResponseLength;

    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Temperature must not exceed 1.0")
    private BigDecimal temperature;

    private Boolean isActive;
}