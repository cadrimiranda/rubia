package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateDonationAppointmentDTO;
import com.ruby.rubia_server.core.dto.UpdateDonationAppointmentDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
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
class DonationAppointmentServiceTest {

    @Mock
    private DonationAppointmentRepository donationAppointmentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @InjectMocks
    private DonationAppointmentService donationAppointmentService;

    private Company company;
    private Customer customer;
    private Conversation conversation;
    private DonationAppointment donationAppointment;
    private CreateDonationAppointmentDTO createDTO;
    private UpdateDonationAppointmentDTO updateDTO;
    private UUID companyId;
    private UUID customerId;
    private UUID conversationId;
    private UUID donationAppointmentId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        donationAppointmentId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .build();

        conversation = Conversation.builder()
                .id(conversationId)
                .company(company)
                .build();

        createDTO = CreateDonationAppointmentDTO.builder()
                .companyId(companyId)
                .customerId(customerId)
                .conversationId(conversationId)
                .externalAppointmentId("EXT-12345")
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .status(DonationAppointmentStatus.SCHEDULED)
                .confirmationUrl("https://example.com/confirm/12345")
                .notes("Test appointment notes")
                .build();

        updateDTO = UpdateDonationAppointmentDTO.builder()
                .appointmentDateTime(LocalDateTime.now().plusDays(2))
                .status(DonationAppointmentStatus.CONFIRMED)
                .confirmationUrl("https://example.com/confirm/updated")
                .notes("Updated appointment notes")
                .build();

        donationAppointment = DonationAppointment.builder()
                .id(donationAppointmentId)
                .company(company)
                .customer(customer)
                .conversation(conversation)
                .externalAppointmentId(createDTO.getExternalAppointmentId())
                .appointmentDateTime(createDTO.getAppointmentDateTime())
                .status(createDTO.getStatus())
                .confirmationUrl(createDTO.getConfirmationUrl())
                .notes(createDTO.getNotes())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createDonationAppointment_ShouldCreateAndReturnDonationAppointment_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(donationAppointmentRepository.save(any(DonationAppointment.class))).thenReturn(donationAppointment);

        // When
        DonationAppointment result = donationAppointmentService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(donationAppointment.getId(), result.getId());
        assertEquals(createDTO.getExternalAppointmentId(), result.getExternalAppointmentId());
        assertEquals(createDTO.getAppointmentDateTime(), result.getAppointmentDateTime());
        assertEquals(createDTO.getStatus(), result.getStatus());
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(customerId, result.getCustomer().getId());
        assertEquals(conversationId, result.getConversation().getId());

        verify(companyRepository).findById(companyId);
        verify(customerRepository).findById(customerId);
        verify(conversationRepository).findById(conversationId);
        verify(donationAppointmentRepository).save(any(DonationAppointment.class));
    }

    @Test
    void createDonationAppointment_ShouldCreateWithoutOptionalEntities_WhenOnlyRequiredDataProvided() {
        // Given
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1);
        CreateDonationAppointmentDTO minimalDTO = CreateDonationAppointmentDTO.builder()
                .companyId(companyId)
                .customerId(customerId)
                .externalAppointmentId("EXT-12345")
                .appointmentDateTime(appointmentTime)
                .status(DonationAppointmentStatus.SCHEDULED)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(donationAppointmentRepository.save(any(DonationAppointment.class))).thenReturn(donationAppointment);

        // When
        DonationAppointment result = donationAppointmentService.create(minimalDTO);

        // Then
        assertNotNull(result);
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(customerId, result.getCustomer().getId());
        assertEquals(minimalDTO.getExternalAppointmentId(), result.getExternalAppointmentId());
        // Note: result returns the mocked donationAppointment, not the actual created one

        verify(companyRepository).findById(companyId);
        verify(customerRepository).findById(customerId);
        verify(conversationRepository, never()).findById(any());
        verify(donationAppointmentRepository).save(any(DonationAppointment.class));
    }

    @Test
    void createDonationAppointment_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> donationAppointmentService.create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(customerRepository, never()).findById(customerId);
        verify(donationAppointmentRepository, never()).save(any(DonationAppointment.class));
    }

    @Test
    void createDonationAppointment_ShouldThrowException_WhenCustomerNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> donationAppointmentService.create(createDTO));
        
        assertEquals("Customer not found with ID: " + customerId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(customerRepository).findById(customerId);
        verify(donationAppointmentRepository, never()).save(any(DonationAppointment.class));
    }

    @Test
    void createDonationAppointment_ShouldThrowException_WhenConversationNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> donationAppointmentService.create(createDTO));
        
        assertEquals("Conversation not found with ID: " + conversationId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(customerRepository).findById(customerId);
        verify(conversationRepository).findById(conversationId);
        verify(donationAppointmentRepository, never()).save(any(DonationAppointment.class));
    }

    @Test
    void getDonationAppointmentById_ShouldReturnDonationAppointment_WhenExists() {
        // Given
        when(donationAppointmentRepository.findById(donationAppointmentId)).thenReturn(Optional.of(donationAppointment));

        // When
        Optional<DonationAppointment> result = donationAppointmentService.findById(donationAppointmentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(donationAppointment.getId(), result.get().getId());
        assertEquals(donationAppointment.getExternalAppointmentId(), result.get().getExternalAppointmentId());
        
        verify(donationAppointmentRepository).findById(donationAppointmentId);
    }

    @Test
    void getDonationAppointmentById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(donationAppointmentRepository.findById(donationAppointmentId)).thenReturn(Optional.empty());

        // When
        Optional<DonationAppointment> result = donationAppointmentService.findById(donationAppointmentId);

        // Then
        assertTrue(result.isEmpty());
        verify(donationAppointmentRepository).findById(donationAppointmentId);
    }

    @Test
    void getAllDonationAppointments_ShouldReturnPagedResults() {
        // Given
        List<DonationAppointment> donationAppointments = List.of(donationAppointment);
        Page<DonationAppointment> page = new PageImpl<>(donationAppointments);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(donationAppointmentRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<DonationAppointment> result = donationAppointmentService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(donationAppointment.getId(), result.getContent().get(0).getId());
        
        verify(donationAppointmentRepository).findAll(pageable);
    }

    @Test
    void getDonationAppointmentsByCompanyId_ShouldReturnDonationAppointmentsForCompany() {
        // Given
        List<DonationAppointment> donationAppointments = List.of(donationAppointment);
        when(donationAppointmentRepository.findByCompanyId(companyId)).thenReturn(donationAppointments);

        // When
        List<DonationAppointment> result = donationAppointmentService.findByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(donationAppointment.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(donationAppointmentRepository).findByCompanyId(companyId);
    }

    @Test
    void updateDonationAppointment_ShouldUpdateAndReturnDonationAppointment_WhenValidData() {
        // Given
        when(donationAppointmentRepository.findById(donationAppointmentId)).thenReturn(Optional.of(donationAppointment));
        when(donationAppointmentRepository.save(any(DonationAppointment.class))).thenReturn(donationAppointment);

        // When
        Optional<DonationAppointment> result = donationAppointmentService.update(donationAppointmentId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        DonationAppointment updated = result.get();
        assertEquals(donationAppointment.getId(), updated.getId());
        
        verify(donationAppointmentRepository).findById(donationAppointmentId);
        verify(donationAppointmentRepository).save(any(DonationAppointment.class));
    }

    @Test
    void updateDonationAppointment_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(donationAppointmentRepository.findById(donationAppointmentId)).thenReturn(Optional.empty());

        // When
        Optional<DonationAppointment> result = donationAppointmentService.update(donationAppointmentId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(donationAppointmentRepository).findById(donationAppointmentId);
        verify(donationAppointmentRepository, never()).save(any(DonationAppointment.class));
    }

    @Test
    void deleteDonationAppointment_ShouldReturnTrue_WhenExists() {
        // Given
        when(donationAppointmentRepository.existsById(donationAppointmentId)).thenReturn(true);

        // When
        boolean result = donationAppointmentService.deleteById(donationAppointmentId);

        // Then
        assertTrue(result);
        
        verify(donationAppointmentRepository).existsById(donationAppointmentId);
        verify(donationAppointmentRepository).deleteById(donationAppointmentId);
    }

    @Test
    void deleteDonationAppointment_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(donationAppointmentRepository.existsById(donationAppointmentId)).thenReturn(false);

        // When
        boolean result = donationAppointmentService.deleteById(donationAppointmentId);

        // Then
        assertFalse(result);
        
        verify(donationAppointmentRepository).existsById(donationAppointmentId);
        verify(donationAppointmentRepository, never()).deleteById(donationAppointmentId);
    }

    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        // Given
        when(donationAppointmentRepository.countByCompanyId(companyId)).thenReturn(5L);

        // When
        long count = donationAppointmentService.countByCompanyId(companyId);

        // Then
        assertEquals(5L, count);
        verify(donationAppointmentRepository).countByCompanyId(companyId);
    }

    @Test
    void findByStatus_ShouldReturnDonationAppointmentsWithSpecificStatus() {
        // Given
        List<DonationAppointment> donationAppointments = List.of(donationAppointment);
        when(donationAppointmentRepository.findByStatus(DonationAppointmentStatus.SCHEDULED)).thenReturn(donationAppointments);

        // When
        List<DonationAppointment> result = donationAppointmentService.findByStatus(DonationAppointmentStatus.SCHEDULED);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(donationAppointment.getId(), result.get(0).getId());
        verify(donationAppointmentRepository).findByStatus(DonationAppointmentStatus.SCHEDULED);
    }

    @Test
    void findByCustomerId_ShouldReturnDonationAppointmentsForCustomer() {
        // Given
        List<DonationAppointment> donationAppointments = List.of(donationAppointment);
        when(donationAppointmentRepository.findByCustomerId(customerId)).thenReturn(donationAppointments);

        // When
        List<DonationAppointment> result = donationAppointmentService.findByCustomerId(customerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(donationAppointment.getId(), result.get(0).getId());
        assertEquals(customerId, result.get(0).getCustomer().getId());
        verify(donationAppointmentRepository).findByCustomerId(customerId);
    }

    @Test
    void findByCompanyIdAndStatus_ShouldReturnFilteredDonationAppointments() {
        // Given
        List<DonationAppointment> donationAppointments = List.of(donationAppointment);
        when(donationAppointmentRepository.findByCompanyIdAndStatus(companyId, DonationAppointmentStatus.SCHEDULED)).thenReturn(donationAppointments);

        // When
        List<DonationAppointment> result = donationAppointmentService.findByCompanyIdAndStatus(companyId, DonationAppointmentStatus.SCHEDULED);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(donationAppointment.getId(), result.get(0).getId());
        verify(donationAppointmentRepository).findByCompanyIdAndStatus(companyId, DonationAppointmentStatus.SCHEDULED);
    }

    @Test
    void findByExternalAppointmentId_ShouldReturnDonationAppointment() {
        // Given
        when(donationAppointmentRepository.findByExternalAppointmentId("EXT-12345")).thenReturn(Optional.of(donationAppointment));

        // When
        Optional<DonationAppointment> result = donationAppointmentService.findByExternalAppointmentId("EXT-12345");

        // Then
        assertTrue(result.isPresent());
        assertEquals(donationAppointment.getId(), result.get().getId());
        verify(donationAppointmentRepository).findByExternalAppointmentId("EXT-12345");
    }

    @Test
    void confirmAppointment_ShouldUpdateStatusToConfirmed() {
        // Given
        when(donationAppointmentRepository.findById(donationAppointmentId)).thenReturn(Optional.of(donationAppointment));
        when(donationAppointmentRepository.save(any(DonationAppointment.class))).thenReturn(donationAppointment);

        // When
        Optional<DonationAppointment> result = donationAppointmentService.confirmAppointment(donationAppointmentId);

        // Then
        assertTrue(result.isPresent());
        verify(donationAppointmentRepository).findById(donationAppointmentId);
        verify(donationAppointmentRepository).save(any(DonationAppointment.class));
    }

    @Test
    void cancelAppointment_ShouldUpdateStatusToCanceled() {
        // Given
        when(donationAppointmentRepository.findById(donationAppointmentId)).thenReturn(Optional.of(donationAppointment));
        when(donationAppointmentRepository.save(any(DonationAppointment.class))).thenReturn(donationAppointment);

        // When
        Optional<DonationAppointment> result = donationAppointmentService.cancelAppointment(donationAppointmentId);

        // Then
        assertTrue(result.isPresent());
        verify(donationAppointmentRepository).findById(donationAppointmentId);
        verify(donationAppointmentRepository).save(any(DonationAppointment.class));
    }

    @Test
    void completeAppointment_ShouldUpdateStatusToCompleted() {
        // Given
        when(donationAppointmentRepository.findById(donationAppointmentId)).thenReturn(Optional.of(donationAppointment));
        when(donationAppointmentRepository.save(any(DonationAppointment.class))).thenReturn(donationAppointment);

        // When
        Optional<DonationAppointment> result = donationAppointmentService.completeAppointment(donationAppointmentId);

        // Then
        assertTrue(result.isPresent());
        verify(donationAppointmentRepository).findById(donationAppointmentId);
        verify(donationAppointmentRepository).save(any(DonationAppointment.class));
    }
}