package com.ruby.rubia_server.core.dto;

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
public class CustomerDTO {
    
    private UUID id;
    private UUID companyId;
    private String phone;
    private String name;
    private String whatsappId;
    private String profileUrl;
    private Boolean isBlocked;
    private String sourceSystemName;
    private String sourceSystemId;
    private LocalDateTime importedAt;
    private LocalDate birthDate;
    private LocalDate lastDonationDate;
    private LocalDate nextEligibleDonationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}