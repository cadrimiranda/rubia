package com.ruby.rubia_server.config;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CompanyContextResolver companyContextResolver;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try to get company context from the current request
        Optional<Company> companyOpt = getCurrentCompanyContext();
        
        User user;
        if (companyOpt.isPresent()) {
            // If we have company context, search by email and company group
            Company company = companyOpt.get();
            user = userRepository.findByEmailAndCompanyGroupId(email, company.getCompanyGroup().getId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email + " for company group: " + company.getCompanyGroup().getName()));
        } else {
            // Fallback to email-only search (for backwards compatibility)
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        }
        
        return new CustomUserPrincipal(user);
    }
    
    private Optional<Company> getCurrentCompanyContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return companyContextResolver.resolveCompany(request);
        } catch (Exception e) {
            // No request context available (e.g., during testing)
            return Optional.empty();
        }
    }

    public static class CustomUserPrincipal implements UserDetails {
        private final User user;

        public CustomUserPrincipal(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        @Override
        public String getPassword() {
            return user.getPasswordHash();
        }

        @Override
        public String getUsername() {
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true; // You could add an 'enabled' field to User entity if needed
        }

        public User getUser() {
            return user;
        }
    }
}