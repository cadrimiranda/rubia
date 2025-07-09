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
    
    @Size(max = 10, message = "Tipo sanguíneo não pode exceder 10 caracteres")
    private String bloodType;
    
    private Integer height;
    private Double weight;
    
    // Campos de endereço
    @Size(max = 255, message = "Rua não pode exceder 255 caracteres")
    private String addressStreet;
    
    @Size(max = 20, message = "Número não pode exceder 20 caracteres")
    private String addressNumber;
    
    @Size(max = 255, message = "Complemento não pode exceder 255 caracteres")
    private String addressComplement;
    
    @Size(max = 10, message = "CEP não pode exceder 10 caracteres")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$|^$", message = "CEP deve estar no formato XXXXX-XXX")
    private String addressPostalCode;
    
    @Size(max = 100, message = "Cidade não pode exceder 100 caracteres")
    private String addressCity;
    
    @Size(max = 2, message = "Estado deve ter 2 caracteres")
    @Pattern(regexp = "^[A-Z]{2}$|^$", message = "Estado deve ser uma UF válida (ex: SP, RJ)")
    private String addressState;
}