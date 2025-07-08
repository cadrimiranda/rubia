package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerDTO {
    
    @Pattern(regexp = "^\\+55\\d{10,11}$", message = "Telefone deve estar no formato +55XXXXXXXXXXX")
    private String phone;
    
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String name;
    
    @Size(max = 255, message = "WhatsApp ID não pode exceder 255 caracteres")
    private String whatsappId;
    
    private String profileUrl;
    
    private Boolean isBlocked;

    private String sourceSystemName;
    private String sourceSystemId;
    private LocalDateTime importedAt;
    private LocalDate birthDate;
    private LocalDate lastDonationDate;
    private LocalDate nextEligibleDonationDate;
}