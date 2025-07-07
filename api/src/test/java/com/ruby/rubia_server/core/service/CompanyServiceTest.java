package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CompanyDTO;
import com.ruby.rubia_server.core.dto.CreateCompanyDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyGroupRepository companyGroupRepository;

    @InjectMocks
    private CompanyService companyService;

    private Company company;
    private CompanyGroup companyGroup;
    private CreateCompanyDTO createCompanyDTO;
    private UUID companyId;
    private UUID companyGroupId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        companyGroupId = UUID.randomUUID();

        companyGroup = CompanyGroup.builder().id(companyGroupId).name("Test Group").build();
        
        createCompanyDTO = CreateCompanyDTO.builder()
                .name("Test Company")
                .slug("test-company")
                .companyGroupId(companyGroupId)
                .build();

        company = Company.builder()
                .id(companyId)
                .name(createCompanyDTO.getName())
                .slug(createCompanyDTO.getSlug())
                .companyGroup(companyGroup)
                .build();
    }

    @Test
    void createCompany_Success() {
        // Arrange
        when(companyRepository.existsBySlug(anyString())).thenReturn(false);
        when(companyGroupRepository.findById(companyGroupId)).thenReturn(Optional.of(companyGroup));
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        CompanyDTO result = companyService.create(createCompanyDTO);

        // Assert
        assertNotNull(result);
        assertEquals(company.getId(), result.getId());
        assertEquals(createCompanyDTO.getName(), result.getName());
        assertEquals(createCompanyDTO.getSlug(), result.getSlug());
        verify(companyRepository, times(1)).save(any(Company.class));
    }

    @Test
    void createCompany_FailsWhenSlugExists() {
        // Arrange
        when(companyRepository.existsBySlug(createCompanyDTO.getSlug())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            companyService.create(createCompanyDTO);
        });
        assertEquals("Company with slug already exists: " + createCompanyDTO.getSlug(), exception.getMessage());
        verify(companyRepository, never()).save(any(Company.class));
    }
}
