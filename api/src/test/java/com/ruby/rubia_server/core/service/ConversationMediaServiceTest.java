package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateConversationMediaDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationMediaDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.MediaType;
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
class ConversationMediaServiceTest {

    @Mock
    private ConversationMediaRepository conversationMediaRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @InjectMocks
    private ConversationMediaService conversationMediaService;

    private Company company;
    private Conversation conversation;
    private User user;
    private Customer customer;
    private ConversationMedia conversationMedia;
    private CreateConversationMediaDTO createDTO;
    private UpdateConversationMediaDTO updateDTO;
    private UUID companyId;
    private UUID conversationId;
    private UUID userId;
    private UUID customerId;
    private UUID conversationMediaId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        conversationMediaId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        conversation = Conversation.builder()
                .id(conversationId)
                .company(company)
                .build();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .build();

        createDTO = CreateConversationMediaDTO.builder()
                .companyId(companyId)
                .conversationId(conversationId)
                .fileUrl("https://example.com/file.jpg")
                .mediaType(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .originalFileName("photo.jpg")
                .fileSizeBytes(1024L)
                .checksum("abc123")
                .uploadedByUserId(userId)
                .build();

        updateDTO = UpdateConversationMediaDTO.builder()
                .fileUrl("https://example.com/updated-file.jpg")
                .mimeType("image/png")
                .originalFileName("updated-photo.png")
                .fileSizeBytes(2048L)
                .checksum("def456")
                .build();

        conversationMedia = ConversationMedia.builder()
                .id(conversationMediaId)
                .company(company)
                .conversation(conversation)
                .fileUrl(createDTO.getFileUrl())
                .mediaType(createDTO.getMediaType())
                .mimeType(createDTO.getMimeType())
                .originalFileName(createDTO.getOriginalFileName())
                .fileSizeBytes(createDTO.getFileSizeBytes())
                .checksum(createDTO.getChecksum())
                .uploadedByUser(user)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createConversationMedia_ShouldCreateAndReturnConversationMedia_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationMediaRepository.save(any(ConversationMedia.class))).thenReturn(conversationMedia);

        // When
        ConversationMedia result = conversationMediaService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(conversationMedia.getId(), result.getId());
        assertEquals(createDTO.getFileUrl(), result.getFileUrl());
        assertEquals(createDTO.getMediaType(), result.getMediaType());
        assertEquals(createDTO.getMimeType(), result.getMimeType());
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(conversationId, result.getConversation().getId());
        assertEquals(userId, result.getUploadedByUser().getId());

        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(userRepository).findById(userId);
        verify(conversationMediaRepository).save(any(ConversationMedia.class));
    }

    @Test
    void createConversationMedia_ShouldCreateWithoutOptionalEntities_WhenOnlyRequiredDataProvided() {
        // Given
        CreateConversationMediaDTO minimalDTO = CreateConversationMediaDTO.builder()
                .companyId(companyId)
                .conversationId(conversationId)
                .fileUrl("https://example.com/file.jpg")
                .mediaType(MediaType.IMAGE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationMediaRepository.save(any(ConversationMedia.class))).thenReturn(conversationMedia);

        // When
        ConversationMedia result = conversationMediaService.create(minimalDTO);

        // Then
        assertNotNull(result);
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(conversationId, result.getConversation().getId());
        assertEquals(minimalDTO.getFileUrl(), result.getFileUrl());
        assertEquals(minimalDTO.getMediaType(), result.getMediaType());

        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(userRepository, never()).findById(any());
        verify(customerRepository, never()).findById(any());
        verify(conversationMediaRepository).save(any(ConversationMedia.class));
    }

    @Test
    void createConversationMedia_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationMediaService.create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository, never()).findById(conversationId);
        verify(conversationMediaRepository, never()).save(any(ConversationMedia.class));
    }

    @Test
    void createConversationMedia_ShouldThrowException_WhenConversationNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationMediaService.create(createDTO));
        
        assertEquals("Conversation not found with ID: " + conversationId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(conversationMediaRepository, never()).save(any(ConversationMedia.class));
    }

    @Test
    void createConversationMedia_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationMediaService.create(createDTO));
        
        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(userRepository).findById(userId);
        verify(conversationMediaRepository, never()).save(any(ConversationMedia.class));
    }

    @Test
    void createConversationMedia_ShouldThrowException_WhenCustomerNotFound() {
        // Given
        createDTO.setUploadedByUserId(null);
        createDTO.setUploadedByCustomerId(customerId);
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationMediaService.create(createDTO));
        
        assertEquals("Customer not found with ID: " + customerId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(customerRepository).findById(customerId);
        verify(conversationMediaRepository, never()).save(any(ConversationMedia.class));
    }

    @Test
    void getConversationMediaById_ShouldReturnConversationMedia_WhenExists() {
        // Given
        when(conversationMediaRepository.findById(conversationMediaId)).thenReturn(Optional.of(conversationMedia));

        // When
        Optional<ConversationMedia> result = conversationMediaService.findById(conversationMediaId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(conversationMedia.getId(), result.get().getId());
        assertEquals(conversationMedia.getFileUrl(), result.get().getFileUrl());
        
        verify(conversationMediaRepository).findById(conversationMediaId);
    }

    @Test
    void getConversationMediaById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(conversationMediaRepository.findById(conversationMediaId)).thenReturn(Optional.empty());

        // When
        Optional<ConversationMedia> result = conversationMediaService.findById(conversationMediaId);

        // Then
        assertTrue(result.isEmpty());
        verify(conversationMediaRepository).findById(conversationMediaId);
    }

    @Test
    void getAllConversationMedias_ShouldReturnPagedResults() {
        // Given
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        Page<ConversationMedia> page = new PageImpl<>(conversationMedias);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(conversationMediaRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<ConversationMedia> result = conversationMediaService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(conversationMedia.getId(), result.getContent().get(0).getId());
        
        verify(conversationMediaRepository).findAll(pageable);
    }

    @Test
    void getConversationMediasByCompanyId_ShouldReturnConversationMediasForCompany() {
        // Given
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        when(conversationMediaRepository.findByCompanyId(companyId)).thenReturn(conversationMedias);

        // When
        List<ConversationMedia> result = conversationMediaService.findByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationMedia.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(conversationMediaRepository).findByCompanyId(companyId);
    }

    @Test
    void updateConversationMedia_ShouldUpdateAndReturnConversationMedia_WhenValidData() {
        // Given
        when(conversationMediaRepository.findById(conversationMediaId)).thenReturn(Optional.of(conversationMedia));
        when(conversationMediaRepository.save(any(ConversationMedia.class))).thenReturn(conversationMedia);

        // When
        Optional<ConversationMedia> result = conversationMediaService.update(conversationMediaId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        ConversationMedia updated = result.get();
        assertEquals(conversationMedia.getId(), updated.getId());
        
        verify(conversationMediaRepository).findById(conversationMediaId);
        verify(conversationMediaRepository).save(any(ConversationMedia.class));
    }

    @Test
    void updateConversationMedia_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(conversationMediaRepository.findById(conversationMediaId)).thenReturn(Optional.empty());

        // When
        Optional<ConversationMedia> result = conversationMediaService.update(conversationMediaId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(conversationMediaRepository).findById(conversationMediaId);
        verify(conversationMediaRepository, never()).save(any(ConversationMedia.class));
    }

    @Test
    void deleteConversationMedia_ShouldReturnTrue_WhenExists() {
        // Given
        when(conversationMediaRepository.existsById(conversationMediaId)).thenReturn(true);

        // When
        boolean result = conversationMediaService.deleteById(conversationMediaId);

        // Then
        assertTrue(result);
        
        verify(conversationMediaRepository).existsById(conversationMediaId);
        verify(conversationMediaRepository).deleteById(conversationMediaId);
    }

    @Test
    void deleteConversationMedia_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(conversationMediaRepository.existsById(conversationMediaId)).thenReturn(false);

        // When
        boolean result = conversationMediaService.deleteById(conversationMediaId);

        // Then
        assertFalse(result);
        
        verify(conversationMediaRepository).existsById(conversationMediaId);
        verify(conversationMediaRepository, never()).deleteById(conversationMediaId);
    }

    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        // Given
        when(conversationMediaRepository.countByCompanyId(companyId)).thenReturn(5L);

        // When
        long count = conversationMediaService.countByCompanyId(companyId);

        // Then
        assertEquals(5L, count);
        verify(conversationMediaRepository).countByCompanyId(companyId);
    }

    @Test
    void findByMediaType_ShouldReturnConversationMediasWithSpecificType() {
        // Given
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        when(conversationMediaRepository.findByMediaType(MediaType.IMAGE)).thenReturn(conversationMedias);

        // When
        List<ConversationMedia> result = conversationMediaService.findByMediaType(MediaType.IMAGE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationMedia.getId(), result.get(0).getId());
        verify(conversationMediaRepository).findByMediaType(MediaType.IMAGE);
    }

    @Test
    void findByConversationId_ShouldReturnConversationMediasForConversation() {
        // Given
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        when(conversationMediaRepository.findByConversationId(conversationId)).thenReturn(conversationMedias);

        // When
        List<ConversationMedia> result = conversationMediaService.findByConversationId(conversationId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationMedia.getId(), result.get(0).getId());
        assertEquals(conversationId, result.get(0).getConversation().getId());
        verify(conversationMediaRepository).findByConversationId(conversationId);
    }

    @Test
    void findByCompanyIdAndMediaType_ShouldReturnFilteredConversationMedias() {
        // Given
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        when(conversationMediaRepository.findByCompanyIdAndMediaType(companyId, MediaType.IMAGE)).thenReturn(conversationMedias);

        // When
        List<ConversationMedia> result = conversationMediaService.findByCompanyIdAndMediaType(companyId, MediaType.IMAGE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationMedia.getId(), result.get(0).getId());
        verify(conversationMediaRepository).findByCompanyIdAndMediaType(companyId, MediaType.IMAGE);
    }

    @Test
    void findByConversationIdAndMediaType_ShouldReturnFilteredConversationMedias() {
        // Given
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        when(conversationMediaRepository.findByConversationIdAndMediaType(conversationId, MediaType.IMAGE)).thenReturn(conversationMedias);

        // When
        List<ConversationMedia> result = conversationMediaService.findByConversationIdAndMediaType(conversationId, MediaType.IMAGE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationMedia.getId(), result.get(0).getId());
        verify(conversationMediaRepository).findByConversationIdAndMediaType(conversationId, MediaType.IMAGE);
    }

    @Test
    void findByUploadedByUserId_ShouldReturnConversationMediasForUser() {
        // Given
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        when(conversationMediaRepository.findByUploadedByUserId(userId)).thenReturn(conversationMedias);

        // When
        List<ConversationMedia> result = conversationMediaService.findByUploadedByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationMedia.getId(), result.get(0).getId());
        assertEquals(userId, result.get(0).getUploadedByUser().getId());
        verify(conversationMediaRepository).findByUploadedByUserId(userId);
    }

    @Test
    void findByUploadedByCustomerId_ShouldReturnConversationMediasForCustomer() {
        // Given
        conversationMedia.setUploadedByCustomer(customer);
        List<ConversationMedia> conversationMedias = List.of(conversationMedia);
        when(conversationMediaRepository.findByUploadedByCustomerId(customerId)).thenReturn(conversationMedias);

        // When
        List<ConversationMedia> result = conversationMediaService.findByUploadedByCustomerId(customerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationMedia.getId(), result.get(0).getId());
        assertEquals(customerId, result.get(0).getUploadedByCustomer().getId());
        verify(conversationMediaRepository).findByUploadedByCustomerId(customerId);
    }

    @Test
    void getTotalFileSizeByCompanyId_ShouldReturnTotalSize() {
        // Given
        when(conversationMediaRepository.getTotalFileSizeByCompanyId(companyId)).thenReturn(1024L);

        // When
        long totalSize = conversationMediaService.getTotalFileSizeByCompanyId(companyId);

        // Then
        assertEquals(1024L, totalSize);
        verify(conversationMediaRepository).getTotalFileSizeByCompanyId(companyId);
    }

    @Test
    void getTotalFileSizeByConversationId_ShouldReturnTotalSize() {
        // Given
        when(conversationMediaRepository.getTotalFileSizeByConversationId(conversationId)).thenReturn(2048L);

        // When
        long totalSize = conversationMediaService.getTotalFileSizeByConversationId(conversationId);

        // Then
        assertEquals(2048L, totalSize);
        verify(conversationMediaRepository).getTotalFileSizeByConversationId(conversationId);
    }
}