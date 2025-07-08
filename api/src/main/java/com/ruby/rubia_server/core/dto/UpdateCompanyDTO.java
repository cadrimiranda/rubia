package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CompanyPlanType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyDTO {

    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String name;

    @Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    private String description;

    @Email(message = "Email de contato deve ter formato válido")
    private String contactEmail;

    @Pattern(regexp = "^\\+?[0-9.()\\-\\s]*$", message = "Telefone de contato inválido")
    private String contactPhone;

    private String logoUrl;

    private Boolean isActive;

    private CompanyPlanType planType;

    private Integer maxUsers;

    private Integer maxWhatsappNumbers;
}