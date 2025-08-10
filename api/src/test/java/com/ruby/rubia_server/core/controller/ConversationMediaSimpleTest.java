package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Teste simples para verificar se as entidades e mocks estão funcionando
 */
@ExtendWith(MockitoExtension.class)
class ConversationMediaSimpleTest {

    @Mock
    private CompanyContextUtil companyContextUtil;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyGroupRepository companyGroupRepository;

    @Mock
    private ConversationMediaRepository mediaRepository;

    @Test
    void mocksAreInitialized() {
        // Verify that all mocks are properly initialized
        assertNotNull(companyRepository);
        assertNotNull(mediaRepository);
        assertNotNull(companyContextUtil);
    }

    @Test
    void shouldCreateBasicEntityObjects() {
        // Test entity creation without database persistence
        CompanyGroup group = CompanyGroup.builder()
                .name("Test Group")
                .build();

        Company company = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .contactEmail("test@test.com")
                .companyGroup(group)
                .build();

        // Verify entities are created correctly
        assertNotNull(group);
        assertEquals("Test Group", group.getName());
        assertNotNull(company);
        assertEquals("Test Company", company.getName());
        assertEquals("test-company", company.getSlug());
        assertEquals(group, company.getCompanyGroup());
    }

    @Test
    void shouldMockCompanyContextUtil() {
        // Given
        UUID testCompanyId = UUID.randomUUID();
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(testCompanyId);
        
        // When
        UUID result = companyContextUtil.getCurrentCompanyId();
        
        // Then
        assertEquals(testCompanyId, result);
    }

    @Test
    void shouldTestRepositoryMockBehavior() {
        // Test that repository mocks can be configured
        UUID testId = UUID.randomUUID();
        when(companyRepository.existsById(testId)).thenReturn(true);
        
        boolean exists = companyRepository.existsById(testId);
        assertTrue(exists);
    }
}