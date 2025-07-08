package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateCampaignContactDTO;
import com.ruby.rubia_server.core.dto.UpdateCampaignContactDTO;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.repository.CampaignContactRepository;
import com.ruby.rubia_server.core.repository.CampaignRepository;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignContactServiceTest {

    @Mock
    private CampaignContactRepository campaignContactRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CampaignContactService campaignContactService;

    private Campaign campaign;
    private Customer customer;
    private CampaignContact campaignContact;
    private CreateCampaignContactDTO createDTO;
    private UpdateCampaignContactDTO updateDTO;
    private UUID campaignId;
    private UUID customerId;
    private UUID campaignContactId;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        campaignContactId = UUID.randomUUID();

        campaign = Campaign.builder()
                .id(campaignId)
                .name("Test Campaign")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .build();

        createDTO = CreateCampaignContactDTO.builder()
                .campaignId(campaignId)
                .customerId(customerId)
                .status(CampaignContactStatus.PENDING)
                .notes("Test notes")
                .build();

        updateDTO = UpdateCampaignContactDTO.builder()
                .status(CampaignContactStatus.SENT)
                .messageSentAt(LocalDateTime.now())
                .notes("Updated notes")
                .build();

        campaignContact = CampaignContact.builder()
                .id(campaignContactId)
                .campaign(campaign)
                .customer(customer)
                .status(createDTO.getStatus())
                .notes(createDTO.getNotes())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createCampaignContact_ShouldCreateAndReturnCampaignContact_WhenValidData() {
        // Given
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(campaignContactRepository.save(any(CampaignContact.class))).thenReturn(campaignContact);

        // When
        CampaignContact result = campaignContactService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(campaignContact.getId(), result.getId());
        assertEquals(createDTO.getStatus(), result.getStatus());
        assertEquals(createDTO.getNotes(), result.getNotes());
        assertEquals(campaignId, result.getCampaign().getId());
        assertEquals(customerId, result.getCustomer().getId());

        verify(campaignRepository).findById(campaignId);
        verify(customerRepository).findById(customerId);
        verify(campaignContactRepository).save(any(CampaignContact.class));
    }

    @Test
    void createCampaignContact_ShouldCreateWithoutOptionalFields_WhenOnlyRequiredDataProvided() {
        // Given
        CreateCampaignContactDTO minimalDTO = CreateCampaignContactDTO.builder()
                .campaignId(campaignId)
                .customerId(customerId)
                .status(CampaignContactStatus.PENDING)
                .build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(campaignContactRepository.save(any(CampaignContact.class))).thenReturn(campaignContact);

        // When
        CampaignContact result = campaignContactService.create(minimalDTO);

        // Then
        assertNotNull(result);
        assertEquals(campaignId, result.getCampaign().getId());
        assertEquals(customerId, result.getCustomer().getId());
        assertEquals(CampaignContactStatus.PENDING, result.getStatus());

        verify(campaignRepository).findById(campaignId);
        verify(customerRepository).findById(customerId);
        verify(campaignContactRepository).save(any(CampaignContact.class));
    }

    @Test
    void createCampaignContact_ShouldThrowException_WhenCampaignNotFound() {
        // Given
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> campaignContactService.create(createDTO));
        
        assertEquals("Campaign not found with ID: " + campaignId, exception.getMessage());
        verify(campaignRepository).findById(campaignId);
        verify(customerRepository, never()).findById(customerId);
        verify(campaignContactRepository, never()).save(any(CampaignContact.class));
    }

    @Test
    void createCampaignContact_ShouldThrowException_WhenCustomerNotFound() {
        // Given
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> campaignContactService.create(createDTO));
        
        assertEquals("Customer not found with ID: " + customerId, exception.getMessage());
        verify(campaignRepository).findById(campaignId);
        verify(customerRepository).findById(customerId);
        verify(campaignContactRepository, never()).save(any(CampaignContact.class));
    }

    @Test
    void getCampaignContactById_ShouldReturnCampaignContact_WhenExists() {
        // Given
        when(campaignContactRepository.findById(campaignContactId)).thenReturn(Optional.of(campaignContact));

        // When
        Optional<CampaignContact> result = campaignContactService.findById(campaignContactId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(campaignContact.getId(), result.get().getId());
        assertEquals(campaignContact.getStatus(), result.get().getStatus());
        
        verify(campaignContactRepository).findById(campaignContactId);
    }

    @Test
    void getCampaignContactById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(campaignContactRepository.findById(campaignContactId)).thenReturn(Optional.empty());

        // When
        Optional<CampaignContact> result = campaignContactService.findById(campaignContactId);

        // Then
        assertTrue(result.isEmpty());
        verify(campaignContactRepository).findById(campaignContactId);
    }

    @Test
    void getAllCampaignContacts_ShouldReturnPagedResults() {
        // Given
        List<CampaignContact> campaignContacts = List.of(campaignContact);
        Page<CampaignContact> page = new PageImpl<>(campaignContacts);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(campaignContactRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<CampaignContact> result = campaignContactService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(campaignContact.getId(), result.getContent().get(0).getId());
        
        verify(campaignContactRepository).findAll(pageable);
    }

    @Test
    void getCampaignContactsByCampaignId_ShouldReturnCampaignContactsForCampaign() {
        // Given
        List<CampaignContact> campaignContacts = List.of(campaignContact);
        when(campaignContactRepository.findByCampaignId(campaignId)).thenReturn(campaignContacts);

        // When
        List<CampaignContact> result = campaignContactService.findByCampaignId(campaignId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(campaignContact.getId(), result.get(0).getId());
        assertEquals(campaignId, result.get(0).getCampaign().getId());
        
        verify(campaignContactRepository).findByCampaignId(campaignId);
    }

    @Test
    void updateCampaignContact_ShouldUpdateAndReturnCampaignContact_WhenValidData() {
        // Given
        when(campaignContactRepository.findById(campaignContactId)).thenReturn(Optional.of(campaignContact));
        when(campaignContactRepository.save(any(CampaignContact.class))).thenReturn(campaignContact);

        // When
        Optional<CampaignContact> result = campaignContactService.update(campaignContactId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        CampaignContact updated = result.get();
        assertEquals(campaignContact.getId(), updated.getId());
        
        verify(campaignContactRepository).findById(campaignContactId);
        verify(campaignContactRepository).save(any(CampaignContact.class));
    }

    @Test
    void updateCampaignContact_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(campaignContactRepository.findById(campaignContactId)).thenReturn(Optional.empty());

        // When
        Optional<CampaignContact> result = campaignContactService.update(campaignContactId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(campaignContactRepository).findById(campaignContactId);
        verify(campaignContactRepository, never()).save(any(CampaignContact.class));
    }

    @Test
    void deleteCampaignContact_ShouldReturnTrue_WhenExists() {
        // Given
        when(campaignContactRepository.existsById(campaignContactId)).thenReturn(true);

        // When
        boolean result = campaignContactService.deleteById(campaignContactId);

        // Then
        assertTrue(result);
        
        verify(campaignContactRepository).existsById(campaignContactId);
        verify(campaignContactRepository).deleteById(campaignContactId);
    }

    @Test
    void deleteCampaignContact_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(campaignContactRepository.existsById(campaignContactId)).thenReturn(false);

        // When
        boolean result = campaignContactService.deleteById(campaignContactId);

        // Then
        assertFalse(result);
        
        verify(campaignContactRepository).existsById(campaignContactId);
        verify(campaignContactRepository, never()).deleteById(campaignContactId);
    }

    @Test
    void countByCampaignId_ShouldReturnCorrectCount() {
        // Given
        when(campaignContactRepository.countByCampaignId(campaignId)).thenReturn(5L);

        // When
        long count = campaignContactService.countByCampaignId(campaignId);

        // Then
        assertEquals(5L, count);
        verify(campaignContactRepository).countByCampaignId(campaignId);
    }

    @Test
    void findByStatus_ShouldReturnCampaignContactsWithSpecificStatus() {
        // Given
        List<CampaignContact> campaignContacts = List.of(campaignContact);
        when(campaignContactRepository.findByStatus(CampaignContactStatus.SENT)).thenReturn(campaignContacts);

        // When
        List<CampaignContact> result = campaignContactService.findByStatus(CampaignContactStatus.SENT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(campaignContact.getId(), result.get(0).getId());
        verify(campaignContactRepository).findByStatus(CampaignContactStatus.SENT);
    }

    @Test
    void findByCampaignIdAndStatus_ShouldReturnFilteredCampaignContacts() {
        // Given
        List<CampaignContact> campaignContacts = List.of(campaignContact);
        when(campaignContactRepository.findByCampaignIdAndStatus(campaignId, CampaignContactStatus.SENT)).thenReturn(campaignContacts);

        // When
        List<CampaignContact> result = campaignContactService.findByCampaignIdAndStatus(campaignId, CampaignContactStatus.SENT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(campaignContact.getId(), result.get(0).getId());
        verify(campaignContactRepository).findByCampaignIdAndStatus(campaignId, CampaignContactStatus.SENT);
    }

    @Test
    void findByCustomerId_ShouldReturnCampaignContactsForCustomer() {
        // Given
        List<CampaignContact> campaignContacts = List.of(campaignContact);
        when(campaignContactRepository.findByCustomerId(customerId)).thenReturn(campaignContacts);

        // When
        List<CampaignContact> result = campaignContactService.findByCustomerId(customerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(campaignContact.getId(), result.get(0).getId());
        assertEquals(customerId, result.get(0).getCustomer().getId());
        verify(campaignContactRepository).findByCustomerId(customerId);
    }

    @Test
    void existsByCampaignIdAndCustomerId_ShouldReturnTrue_WhenExists() {
        // Given
        when(campaignContactRepository.existsByCampaignIdAndCustomerId(campaignId, customerId)).thenReturn(true);

        // When
        boolean result = campaignContactService.existsByCampaignIdAndCustomerId(campaignId, customerId);

        // Then
        assertTrue(result);
        verify(campaignContactRepository).existsByCampaignIdAndCustomerId(campaignId, customerId);
    }

    @Test
    void existsByCampaignIdAndCustomerId_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(campaignContactRepository.existsByCampaignIdAndCustomerId(campaignId, customerId)).thenReturn(false);

        // When
        boolean result = campaignContactService.existsByCampaignIdAndCustomerId(campaignId, customerId);

        // Then
        assertFalse(result);
        verify(campaignContactRepository).existsByCampaignIdAndCustomerId(campaignId, customerId);
    }

    @Test
    void markAsCompleted_ShouldUpdateStatusAndTimestamp() {
        // Given
        when(campaignContactRepository.findById(campaignContactId)).thenReturn(Optional.of(campaignContact));
        when(campaignContactRepository.save(any(CampaignContact.class))).thenReturn(campaignContact);

        // When
        Optional<CampaignContact> result = campaignContactService.markAsCompleted(campaignContactId);

        // Then
        assertTrue(result.isPresent());
        verify(campaignContactRepository).findById(campaignContactId);
        verify(campaignContactRepository).save(any(CampaignContact.class));
    }

    @Test
    void markAsCompleted_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(campaignContactRepository.findById(campaignContactId)).thenReturn(Optional.empty());

        // When
        Optional<CampaignContact> result = campaignContactService.markAsCompleted(campaignContactId);

        // Then
        assertTrue(result.isEmpty());
        verify(campaignContactRepository).findById(campaignContactId);
        verify(campaignContactRepository, never()).save(any(CampaignContact.class));
    }
}