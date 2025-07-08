package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
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
public class DonationAppointmentDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID customerId;
    private String customerName;
    private UUID conversationId;
    private String externalAppointmentId;
    private LocalDateTime appointmentDateTime;
    private DonationAppointmentStatus status;
    private String confirmationUrl;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}