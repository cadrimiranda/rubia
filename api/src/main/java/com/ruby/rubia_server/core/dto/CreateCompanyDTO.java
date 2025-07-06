package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CompanyPlanType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String name;

    @NotBlank(message = "Slug é obrigatório")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug deve conter apenas letras minúsculas, números e hífens")
    @Size(min = 2, max = 255, message = "Slug deve ter entre 2 e 255 caracteres")
    private String slug;

    @Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    private String description;

    @Email(message = "Email de contato deve ter formato válido")
    private String contactEmail;

    @Pattern(regexp = "^\\+?[0-9.()\\-\\s]*$", message = "Telefone de contato inválido")
    private String contactPhone;

    private String logoUrl;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private CompanyPlanType planType = CompanyPlanType.BASIC;

    @Builder.Default
    private Integer maxUsers = 10;

    @Builder.Default
    private Integer maxWhatsappNumbers = 1;

    @NotNull(message = "ID do grupo da empresa é obrigatório")
    private UUID companyGroupId;
}