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
public class CreateAIAgentDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Pattern(regexp = "^data:image\\/(jpeg|jpg|png|gif);base64,[A-Za-z0-9+/]+=*$|^$", 
             message = "Avatar must be a valid base64 image (data:image/jpeg;base64,...) or empty")
    private String avatarBase64;

    @NotNull(message = "AI Model ID is required")
    private UUID aiModelId;

    @NotBlank(message = "Temperament is required")
    @Size(max = 50, message = "Temperament must not exceed 50 characters")
    private String temperament;

    @Min(value = 1, message = "Max response length must be at least 1")
    @Max(value = 10000, message = "Max response length must not exceed 10000")
    @Builder.Default
    private Integer maxResponseLength = 500;

    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Temperature must not exceed 1.0")
    @Builder.Default
    private BigDecimal temperature = BigDecimal.valueOf(0.7);

    @Min(value = 1, message = "AI message limit must be at least 1")
    @Max(value = 1000, message = "AI message limit must not exceed 1000")
    @Builder.Default
    private Integer aiMessageLimit = 10;

    @Builder.Default
    private Boolean isActive = true;
}