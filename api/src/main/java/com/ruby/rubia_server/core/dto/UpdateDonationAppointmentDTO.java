package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDonationAppointmentDTO {

    private LocalDateTime appointmentDateTime;

    private DonationAppointmentStatus status;

    @Size(max = 500, message = "Confirmation URL must not exceed 500 characters")
    private String confirmationUrl;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}