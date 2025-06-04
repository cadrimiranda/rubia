package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepartmentDTO {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String name;
    
    @Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    private String description;
    
    @Builder.Default
    private Boolean autoAssign = true;
}