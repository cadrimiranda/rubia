package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CompanyGroupDTO;
import com.ruby.rubia_server.core.dto.CreateCompanyGroupDTO;
import com.ruby.rubia_server.core.dto.UpdateCompanyGroupDTO;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyGroupServiceTest {

    @Mock
    private CompanyGroupRepository companyGroupRepository;

    @InjectMocks
    private CompanyGroupService companyGroupService;

    private CompanyGroup companyGroup;
    private CompanyGroupDTO companyGroupDTO;
    private UUID companyGroupId;

    @BeforeEach
    void setUp() {
        companyGroupId = UUID.randomUUID();
        companyGroup = CompanyGroup.builder()
                .id(companyGroupId)
                .name("Test Company Group")
                .description("Description for test group")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        companyGroupDTO = CompanyGroupDTO.builder()
                .id(companyGroupId)
                .name("Test Company Group")
                .description("Description for test group")
                .isActive(true)
                .createdAt(companyGroup.getCreatedAt())
                .updatedAt(companyGroup.getUpdatedAt())
                .build();
    }

    @Test
    void create_Success() {
        CreateCompanyGroupDTO createDTO = CreateCompanyGroupDTO.builder()
                .name("New Company Group")
                .description("New group description")
                .isActive(true)
                .build();

        when(companyGroupRepository.findByName(createDTO.getName())).thenReturn(Optional.empty());
        when(companyGroupRepository.save(any(CompanyGroup.class))).thenReturn(companyGroup);

        CompanyGroupDTO result = companyGroupService.create(createDTO);

        assertNotNull(result);
        assertEquals(companyGroupDTO.getName(), result.getName());
        verify(companyGroupRepository, times(1)).save(any(CompanyGroup.class));
    }

    @Test
    void create_ThrowsExceptionWhenNameExists() {
        CreateCompanyGroupDTO createDTO = CreateCompanyGroupDTO.builder()
                .name("Test Company Group")
                .build();

        when(companyGroupRepository.findByName(createDTO.getName())).thenReturn(Optional.of(companyGroup));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            companyGroupService.create(createDTO);
        });

        assertEquals("Company group with name '" + createDTO.getName() + "' already exists", thrown.getMessage());
        verify(companyGroupRepository, never()).save(any(CompanyGroup.class));
    }

    @Test
    void findAll_ReturnsListOfCompanyGroups() {
        when(companyGroupRepository.findAll()).thenReturn(Arrays.asList(companyGroup));

        List<CompanyGroupDTO> result = companyGroupService.findAll();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(companyGroupDTO.getName(), result.get(0).getName());
    }

    @Test
    void findById_ReturnsCompanyGroupWhenFound() {
        when(companyGroupRepository.findById(companyGroupId)).thenReturn(Optional.of(companyGroup));

        CompanyGroupDTO result = companyGroupService.findById(companyGroupId);

        assertNotNull(result);
        assertEquals(companyGroupDTO.getId(), result.getId());
    }

    @Test
    void findById_ThrowsExceptionWhenNotFound() {
        when(companyGroupRepository.findById(companyGroupId)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            companyGroupService.findById(companyGroupId);
        });

        assertEquals("Company group not found with id: " + companyGroupId, thrown.getMessage());
    }

    @Test
    void update_Success() {
        UpdateCompanyGroupDTO updateDTO = UpdateCompanyGroupDTO.builder()
                .name("Updated Group Name")
                .description("Updated description")
                .isActive(false)
                .build();

        when(companyGroupRepository.findById(companyGroupId)).thenReturn(Optional.of(companyGroup));
        when(companyGroupRepository.findByName(updateDTO.getName())).thenReturn(Optional.empty());
        when(companyGroupRepository.save(any(CompanyGroup.class))).thenReturn(companyGroup);

        CompanyGroupDTO result = companyGroupService.update(companyGroupId, updateDTO);

        assertNotNull(result);
        assertEquals(updateDTO.getName(), result.getName());
        assertEquals(updateDTO.getDescription(), result.getDescription());
        assertEquals(updateDTO.getIsActive(), result.getIsActive());
        verify(companyGroupRepository, times(1)).save(companyGroup);
    }

    @Test
    void update_ThrowsExceptionWhenNotFound() {
        UpdateCompanyGroupDTO updateDTO = UpdateCompanyGroupDTO.builder().name("Updated Name").build();

        when(companyGroupRepository.findById(companyGroupId)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            companyGroupService.update(companyGroupId, updateDTO);
        });

        assertEquals("Company group not found with id: " + companyGroupId, thrown.getMessage());
        verify(companyGroupRepository, never()).save(any(CompanyGroup.class));
    }

    @Test
    void update_ThrowsExceptionWhenNewNameExists() {
        UpdateCompanyGroupDTO updateDTO = UpdateCompanyGroupDTO.builder().name("Existing Group").build();
        CompanyGroup existingGroupWithNewName = CompanyGroup.builder().id(UUID.randomUUID()).name("Existing Group").build();

        when(companyGroupRepository.findById(companyGroupId)).thenReturn(Optional.of(companyGroup));
        when(companyGroupRepository.findByName(updateDTO.getName())).thenReturn(Optional.of(existingGroupWithNewName));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            companyGroupService.update(companyGroupId, updateDTO);
        });

        assertEquals("Company group with name '" + updateDTO.getName() + "' already exists", thrown.getMessage());
        verify(companyGroupRepository, never()).save(any(CompanyGroup.class));
    }

    @Test
    void delete_Success() {
        when(companyGroupRepository.existsById(companyGroupId)).thenReturn(true);
        doNothing().when(companyGroupRepository).deleteById(companyGroupId);

        assertDoesNotThrow(() -> companyGroupService.delete(companyGroupId));
        verify(companyGroupRepository, times(1)).deleteById(companyGroupId);
    }

    @Test
    void delete_ThrowsExceptionWhenNotFound() {
        when(companyGroupRepository.existsById(companyGroupId)).thenReturn(false);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            companyGroupService.delete(companyGroupId);
        });

        assertEquals("Company group not found with id: " + companyGroupId, thrown.getMessage());
        verify(companyGroupRepository, never()).deleteById(any(UUID.class));
    }
}
