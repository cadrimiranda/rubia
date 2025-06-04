package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentDTO {
    
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String name;
    
    @Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    private String description;
    
    private Boolean autoAssign;
}