package com.ruby.rubia_server.core.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateCustomerDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void createCustomerDTO_ValidData_Success() {
        // Arrange
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("João Silva")
                .phone("+5511999887766")
                .bloodType("O+")
                .height(175)
                .weight(70.5)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertTrue(violations.isEmpty());
        assertEquals("João Silva", dto.getName());
        assertEquals("+5511999887766", dto.getPhone());
        assertEquals("O+", dto.getBloodType());
        assertEquals(175, dto.getHeight());
        assertEquals(70.5, dto.getWeight());
        assertEquals(LocalDate.of(1990, 1, 1), dto.getBirthDate());
    }

    @Test
    void createCustomerDTO_BlankPhone_ValidationFails() {
        // Arrange
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("João Silva")
                .phone("")
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Telefone é obrigatório")));
    }

    @Test
    void createCustomerDTO_InvalidPhoneFormat_ValidationFails() {
        // Arrange
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("João Silva")
                .phone("11999887766") // Sem código do país
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Telefone deve estar no formato +55XXXXXXXXXXX")));
    }

    @Test
    void createCustomerDTO_BloodTypeTooLong_ValidationFails() {
        // Arrange
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("João Silva")
                .phone("+5511999887766")
                .bloodType("AB+RH+EXTRA") // Muito longo
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Tipo sanguíneo não pode exceder 10 caracteres")));
    }

    @Test
    void createCustomerDTO_NameTooShort_ValidationFails() {
        // Arrange
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("J") // Muito curto
                .phone("+5511999887766")
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Nome deve ter entre 2 e 255 caracteres")));
    }

    @Test
    void createCustomerDTO_AllBloodTypes_Valid() {
        // Arrange
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

        for (String bloodType : bloodTypes) {
            CreateCustomerDTO dto = CreateCustomerDTO.builder()
                    .name("João Silva")
                    .phone("+5511999887766")
                    .bloodType(bloodType)
                    .build();

            // Act
            Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

            // Assert
            assertTrue(violations.isEmpty(), "BloodType " + bloodType + " should be valid");
        }
    }

    @Test
    void createCustomerDTO_OptionalFields_Success() {
        // Arrange
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("Maria Santos")
                .phone("+5511888777666")
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertTrue(violations.isEmpty());
        assertNull(dto.getBloodType());
        assertNull(dto.getHeight());
        assertNull(dto.getWeight());
        assertNull(dto.getBirthDate());
    }

    @Test
    void createCustomerDTO_NegativeHeight_StillValid() {
        // Arrange (Note: Height validation is not enforced at DTO level)
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("João Silva")
                .phone("+5511999887766")
                .height(-10)
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertTrue(violations.isEmpty()); // DTO validation doesn't enforce positive height
    }

    @Test
    void createCustomerDTO_NegativeWeight_StillValid() {
        // Arrange (Note: Weight validation is not enforced at DTO level)
        CreateCustomerDTO dto = CreateCustomerDTO.builder()
                .name("João Silva")
                .phone("+5511999887766")
                .weight(-5.0)
                .build();

        // Act
        Set<ConstraintViolation<CreateCustomerDTO>> violations = validator.validate(dto);

        // Assert
        assertTrue(violations.isEmpty()); // DTO validation doesn't enforce positive weight
    }
}