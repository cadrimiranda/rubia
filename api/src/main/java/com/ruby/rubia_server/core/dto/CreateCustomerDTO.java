package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class CreateCustomerDTO {
    
    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^\\+55\\d{10,11}$", message = "Telefone deve estar no formato +55XXXXXXXXXXX")
    private String phone;
    
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String name;
    
    @Size(max = 255, message = "WhatsApp ID não pode exceder 255 caracteres")
    private String whatsappId;
    
    private String profileUrl;
    
    @Builder.Default
    private Boolean isBlocked = false;

    private String sourceSystemName;
    private String sourceSystemId;
    private LocalDateTime importedAt;
    private LocalDate birthDate;
    private LocalDate lastDonationDate;
    private LocalDate nextEligibleDonationDate;
    
    @Size(max = 10, message = "Tipo sanguíneo não pode exceder 10 caracteres")
    private String bloodType;
    
    private Integer height; // Altura em centímetros
    private Double weight; // Peso em quilogramas
    
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
    
    // Campos adicionais para campanhas
    @Size(max = 255, message = "Email não pode exceder 255 caracteres")
    private String email;
    
    @Size(max = 14, message = "CPF não pode exceder 14 caracteres")
    private String cpf;
    
    @Size(max = 20, message = "RG não pode exceder 20 caracteres")
    private String rg;
    
    @Size(max = 10, message = "Fator RH não pode exceder 10 caracteres")
    private String rhFactor;
}