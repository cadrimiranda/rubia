package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.dto.campaign.CreateCampaignDTO;
import com.ruby.rubia_server.dto.campaign.UpdateCampaignDTO;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.repository.CampaignRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @InjectMocks
    private CampaignService campaignService;

    private Company company;
    private Campaign campaign;
    private CreateCampaignDTO createDTO;
    private UpdateCampaignDTO updateDTO;
    private UUID companyId;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        campaignId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        createDTO = CreateCampaignDTO.builder()
                .companyId(companyId)
                .name("Test Campaign")
                .description("Test Description")
                .status(CampaignStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .targetAudienceDescription("Test audience")
                .totalContacts(100)
                .sourceSystemName("Test System")
                .sourceSystemId("SYS-123")
                .build();

        updateDTO = UpdateCampaignDTO.builder()
                .name("Updated Campaign")
                .description("Updated Description")
                .status(CampaignStatus.ACTIVE)
                .totalContacts(200)
                .contactsReached(50)
                .build();

        campaign = Campaign.builder()
                .id(campaignId)
                .company(company)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .status(createDTO.getStatus())
                .startDate(createDTO.getStartDate())
                .endDate(createDTO.getEndDate())
                .targetAudienceDescription(createDTO.getTargetAudienceDescription())
                .totalContacts(createDTO.getTotalContacts())
                .contactsReached(0)
                .sourceSystemName(createDTO.getSourceSystemName())
                .sourceSystemId(createDTO.getSourceSystemId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_ShouldCreateAndReturnCampaign_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        // When
        Campaign result = campaignService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(campaign.getId(), result.getId());
        assertEquals(createDTO.getName(), result.getName());
        assertEquals(createDTO.getDescription(), result.getDescription());
        assertEquals(createDTO.getStatus(), result.getStatus());
        assertEquals(company.getId(), result.getCompany().getId());

        verify(companyRepository).findById(companyId);
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void create_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> campaignService.create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void findById_ShouldReturnCampaign_WhenExists() {
        // Given
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        // When
        Optional<Campaign> result = campaignService.findById(campaignId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(campaign.getId(), result.get().getId());
        assertEquals(campaign.getName(), result.get().getName());
        
        verify(campaignRepository).findById(campaignId);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        // When
        Optional<Campaign> result = campaignService.findById(campaignId);

        // Then
        assertTrue(result.isEmpty());
        verify(campaignRepository).findById(campaignId);
    }

    @Test
    void findAll_ShouldReturnPagedResults() {
        // Given
        List<Campaign> campaigns = List.of(campaign);
        Page<Campaign> page = new PageImpl<>(campaigns);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(campaignRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<Campaign> result = campaignService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(campaign.getId(), result.getContent().get(0).getId());
        
        verify(campaignRepository).findAll(pageable);
    }

    @Test
    void findByCompanyId_ShouldReturnCampaignsForCompany() {
        // Given
        List<Campaign> campaigns = List.of(campaign);
        when(campaignRepository.findByCompanyId(companyId)).thenReturn(campaigns);

        // When
        List<Campaign> result = campaignService.findByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(campaign.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(campaignRepository).findByCompanyId(companyId);
    }

    @Test
    void update_ShouldUpdateAndReturnCampaign_WhenValidData() {
        // Given
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        // When
        Optional<Campaign> result = campaignService.update(campaignId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        Campaign updated = result.get();
        assertEquals(campaign.getId(), updated.getId());
        
        verify(campaignRepository).findById(campaignId);
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void update_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        // When
        Optional<Campaign> result = campaignService.update(campaignId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(campaignRepository).findById(campaignId);
        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void deleteById_ShouldReturnTrue_WhenExists() {
        // Given
        when(campaignRepository.existsById(campaignId)).thenReturn(true);

        // When
        boolean result = campaignService.deleteById(campaignId);

        // Then
        assertTrue(result);
        
        verify(campaignRepository).existsById(campaignId);
        verify(campaignRepository).deleteById(campaignId);
    }

    @Test
    void deleteById_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(campaignRepository.existsById(campaignId)).thenReturn(false);

        // When
        boolean result = campaignService.deleteById(campaignId);

        // Then
        assertFalse(result);
        
        verify(campaignRepository).existsById(campaignId);
        verify(campaignRepository, never()).deleteById(campaignId);
    }

    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        // Given
        when(campaignRepository.countByCompanyId(companyId)).thenReturn(5L);

        // When
        long count = campaignService.countByCompanyId(companyId);

        // Then
        assertEquals(5L, count);
        verify(campaignRepository).countByCompanyId(companyId);
    }

    @Test
    void findByStatus_ShouldReturnCampaignsWithSpecificStatus() {
        // Given
        List<Campaign> campaigns = List.of(campaign);
        when(campaignRepository.findByStatus(CampaignStatus.ACTIVE)).thenReturn(campaigns);

        // When
        List<Campaign> result = campaignService.findByStatus(CampaignStatus.ACTIVE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(campaign.getId(), result.get(0).getId());
        verify(campaignRepository).findByStatus(CampaignStatus.ACTIVE);
    }

    @Test
    void findByCompanyIdAndStatus_ShouldReturnFilteredCampaigns() {
        // Given
        List<Campaign> campaigns = List.of(campaign);
        when(campaignRepository.findByCompanyIdAndStatus(companyId, CampaignStatus.ACTIVE)).thenReturn(campaigns);

        // When
        List<Campaign> result = campaignService.findByCompanyIdAndStatus(companyId, CampaignStatus.ACTIVE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(campaign.getId(), result.get(0).getId());
        verify(campaignRepository).findByCompanyIdAndStatus(companyId, CampaignStatus.ACTIVE);
    }
}