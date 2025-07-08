package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CampaignContactStatus;
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
public class CampaignContactDTO {

    private UUID id;
    private UUID campaignId;
    private String campaignName;
    private UUID customerId;
    private String customerName;
    private CampaignContactStatus status;
    private LocalDateTime messageSentAt;
    private LocalDateTime responseReceivedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}