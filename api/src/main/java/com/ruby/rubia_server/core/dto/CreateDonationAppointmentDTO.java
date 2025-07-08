package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
import jakarta.validation.constraints.NotBlank;
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
public class CreateDonationAppointmentDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    private UUID conversationId;

    @NotBlank(message = "External appointment ID is required")
    @Size(max = 255, message = "External appointment ID must not exceed 255 characters")
    private String externalAppointmentId;

    @NotNull(message = "Appointment date time is required")
    private LocalDateTime appointmentDateTime;

    @NotNull(message = "Status is required")
    private DonationAppointmentStatus status;

    @Size(max = 500, message = "Confirmation URL must not exceed 500 characters")
    private String confirmationUrl;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}