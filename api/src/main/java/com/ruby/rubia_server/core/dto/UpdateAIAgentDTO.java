package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAIAgentDTO {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Pattern(regexp = "^data:image\\/(jpeg|jpg|png|gif);base64,[A-Za-z0-9+/]+=*$|^$", 
             message = "Avatar must be a valid base64 image (data:image/jpeg;base64,...) or empty")
    private String avatarBase64;

    private UUID aiModelId;

    @Size(max = 50, message = "Temperament must not exceed 50 characters")
    private String temperament;

    @Min(value = 1, message = "Max response length must be at least 1")
    @Max(value = 10000, message = "Max response length must not exceed 10000")
    private Integer maxResponseLength;

    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Temperature must not exceed 1.0")
    private BigDecimal temperature;

    @Min(value = 1, message = "AI message limit must be at least 1")
    @Max(value = 1000, message = "AI message limit must not exceed 1000")
    private Integer aiMessageLimit;

    private Boolean isActive;
}