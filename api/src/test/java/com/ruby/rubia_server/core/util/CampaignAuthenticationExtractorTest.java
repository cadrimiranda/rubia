package com.ruby.rubia_server.core.util;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignAuthenticationExtractorTest {

    @Mock
    private CompanyContextUtil companyContextUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CampaignAuthenticationExtractor authExtractor;

    private User testUser;
    private Company testCompany;
    private CompanyGroup testCompanyGroup;
    private UUID userId;
    private UUID companyId;
    private UUID companyGroupId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        companyGroupId = UUID.randomUUID();

        testCompanyGroup = new CompanyGroup();
        testCompanyGroup.setId(companyGroupId);

        testCompany = new Company();
        testCompany.setId(companyId);
        testCompany.setCompanyGroup(testCompanyGroup);

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setCompany(testCompany);
    }

    @Test
    void extractFromAuthentication_WithValidUser_ShouldReturnCorrectContext() {
        // Arrange
        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_CAMPAIGN_MANAGER"),
            new SimpleGrantedAuthority("ROLE_USER")
        );

        when(companyContextUtil.getAuthenticatedUser()).thenReturn(testUser);
        when(companyContextUtil.getAuthenticatedUserCompanyId()).thenReturn(companyId);
        when(companyContextUtil.getAuthenticatedUserCompanyGroupId()).thenReturn(companyGroupId);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act
        CampaignAuthenticationExtractor.CampaignUserContext context = 
            authExtractor.extractFromAuthentication(authentication);

        // Assert
        assertNotNull(context);
        assertEquals(userId.toString(), context.getUserId());
        assertEquals(companyId.toString(), context.getCompanyId());
        assertEquals(companyGroupId.toString(), context.getCompanyGroupId());
        assertEquals("test@example.com", context.getUserEmail());
        assertEquals("Test User", context.getUserName());
        assertTrue(context.getRoles().contains("ROLE_CAMPAIGN_MANAGER"));
        assertTrue(context.getRoles().contains("ROLE_USER"));

        verify(companyContextUtil).getAuthenticatedUser();
        verify(companyContextUtil).getAuthenticatedUserCompanyId();
        verify(companyContextUtil).getAuthenticatedUserCompanyGroupId();
        verify(authentication).getAuthorities();
    }

    @Test
    void extractFromAuthentication_WhenCompanyContextUtilThrowsException_ShouldThrowSecurityException() {
        // Arrange
        when(companyContextUtil.getAuthenticatedUser())
            .thenThrow(new IllegalStateException("No authenticated user found"));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> 
            authExtractor.extractFromAuthentication(authentication));

        assertEquals("Unable to extract user context", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void extractFromAuthentication_WithEmptyRoles_ShouldReturnEmptyRoleSet() {
        // Arrange
        when(companyContextUtil.getAuthenticatedUser()).thenReturn(testUser);
        when(companyContextUtil.getAuthenticatedUserCompanyId()).thenReturn(companyId);
        when(companyContextUtil.getAuthenticatedUserCompanyGroupId()).thenReturn(companyGroupId);
        when(authentication.getAuthorities()).thenAnswer(invocation -> List.of());

        // Act
        CampaignAuthenticationExtractor.CampaignUserContext context = 
            authExtractor.extractFromAuthentication(authentication);

        // Assert
        assertNotNull(context);
        assertTrue(context.getRoles().isEmpty());
    }

    @Test
    void campaignUserContext_Builder_ShouldBuildCorrectly() {
        // Arrange
        Set<String> roles = Set.of("ROLE_ADMIN", "ROLE_CAMPAIGN_MANAGER");

        // Act
        CampaignAuthenticationExtractor.CampaignUserContext context = 
            CampaignAuthenticationExtractor.CampaignUserContext.builder()
                .userId("user-123")
                .companyId("company-456")
                .companyGroupId("group-789")
                .userEmail("builder@test.com")
                .userName("Builder User")
                .roles(roles)
                .build();

        // Assert
        assertEquals("user-123", context.getUserId());
        assertEquals("company-456", context.getCompanyId());
        assertEquals("group-789", context.getCompanyGroupId());
        assertEquals("builder@test.com", context.getUserEmail());
        assertEquals("Builder User", context.getUserName());
        assertEquals(roles, context.getRoles());
    }
}