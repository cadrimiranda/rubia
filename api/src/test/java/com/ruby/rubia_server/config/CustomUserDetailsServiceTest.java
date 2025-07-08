package com.ruby.rubia_server.config;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;
    private final String testEmail = "test@example.com";
    private final String testPasswordHash = "hashedPassword123";

    @BeforeEach
    void setUp() {
        CompanyGroup companyGroup = CompanyGroup.builder()
                .id(UUID.randomUUID())
                .name("Test Company Group")
                .build();

        Company company = Company.builder()
                .id(UUID.randomUUID())
                .name("Test Company")
                .slug("test-company")
                .companyGroup(companyGroup)
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email(testEmail)
                .passwordHash(testPasswordHash)
                .role(UserRole.ADMIN)
                .company(company)
                .build();
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testEmail);

        // Then
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertEquals(testPasswordHash, userDetails.getPassword());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());

        // Verify authorities
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername(nonExistentEmail)
        );
        
        assertEquals("User not found with email: " + nonExistentEmail, exception.getMessage());
    }

    @Test
    void customUserPrincipal_ShouldHaveCorrectAuthorities_ForDifferentRoles() {
        // Test ADMIN role
        testUserRole(UserRole.ADMIN, "ROLE_ADMIN");
        
        // Test AGENT role
        testUserRole(UserRole.AGENT, "ROLE_AGENT");
        
        // Test SUPERVISOR role
        testUserRole(UserRole.SUPERVISOR, "ROLE_SUPERVISOR");
    }

    private void testUserRole(UserRole role, String expectedAuthority) {
        // Given
        User userWithRole = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(role)
                .company(user.getCompany())
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(userWithRole));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testEmail);

        // Then
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority(expectedAuthority)));
    }

    @Test
    void customUserPrincipal_ShouldProvideAccessToOriginalUser() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testEmail);

        // Then
        assertTrue(userDetails instanceof CustomUserDetailsService.CustomUserPrincipal);
        CustomUserDetailsService.CustomUserPrincipal customPrincipal = 
            (CustomUserDetailsService.CustomUserPrincipal) userDetails;
        
        User retrievedUser = customPrincipal.getUser();
        assertEquals(user.getId(), retrievedUser.getId());
        assertEquals(user.getName(), retrievedUser.getName());
        assertEquals(user.getEmail(), retrievedUser.getEmail());
        assertEquals(user.getRole(), retrievedUser.getRole());
        assertEquals(user.getCompany().getId(), retrievedUser.getCompany().getId());
    }

    @Test
    void customUserPrincipal_ShouldReturnCorrectAccountStatus() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testEmail);

        // Then
        // All account status methods should return true by default
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void customUserPrincipal_ShouldBeEqualBasedOnUser() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));
        
        // When
        UserDetails userDetails1 = customUserDetailsService.loadUserByUsername(testEmail);
        UserDetails userDetails2 = customUserDetailsService.loadUserByUsername(testEmail);

        // Then
        assertEquals(userDetails1.getUsername(), userDetails2.getUsername());
        assertEquals(userDetails1.getPassword(), userDetails2.getPassword());
        assertEquals(userDetails1.getAuthorities(), userDetails2.getAuthorities());
    }

    @Test
    void loadUserByUsername_ShouldHandleNullEmail() {
        // When & Then
        assertThrows(
            UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername(null)
        );
    }

    @Test
    void loadUserByUsername_ShouldHandleEmptyEmail() {
        // Given
        String emptyEmail = "";
        when(userRepository.findByEmail(emptyEmail)).thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername(emptyEmail)
        );
        
        assertEquals("User not found with email: " + emptyEmail, exception.getMessage());
    }
}