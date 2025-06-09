package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDTO {
    
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    private String name;
    
    @Email(message = "Email deve ter formato válido")
    private String email;
    
    @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
    private String password;
    
    private UUID departmentId;
    
    private UserRole role;
    
    private String avatarUrl;
    
    private LocalDate birthDate;
    
    private Double weight;
    
    private Double height;
    
    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    private String address;
}