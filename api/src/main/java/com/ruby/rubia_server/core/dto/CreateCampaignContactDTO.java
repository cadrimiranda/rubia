package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateCampaignContactDTO {

    @NotNull(message = "Campaign ID is required")
    private UUID campaignId;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Status is required")
    private CampaignContactStatus status;

    private LocalDateTime messageSentAt;

    private LocalDateTime responseReceivedAt;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}