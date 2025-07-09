package com.ruby.rubia_server.auth;

import com.ruby.rubia_server.config.CompanyContextResolver;
import com.ruby.rubia_server.config.JwtService;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CompanyContextResolver companyContextResolver;

    public AuthResponse login(LoginRequest request) {
        try {
            // Get current HTTP request to resolve company context
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            
            // Resolve company from subdomain
            Optional<Company> companyOpt = companyContextResolver.resolveCompany(httpRequest);
            log.debug("Company resolved: {}", companyOpt.map(Company::getSlug).orElse("none"));
            if (companyOpt.isEmpty()) {
                throw new com.ruby.rubia_server.auth.AuthenticationException("Company not found. Please check the subdomain.");
            }
            
            Company company = companyOpt.get();
            
            // Find user by email and company group
            log.debug("Searching for user with email: {} and company group: {}", request.getEmail(), company.getCompanyGroup().getId());
            Optional<User> userOpt = userRepository.findByEmailAndCompanyGroupId(request.getEmail(), company.getCompanyGroup().getId());
            if (userOpt.isEmpty()) {
                log.error("User not found for email: {} and company group: {}", request.getEmail(), company.getCompanyGroup().getId());
                throw new com.ruby.rubia_server.auth.AuthenticationException("User not found for this company group");
            }
            
            User user = userOpt.get();
            log.debug("User found: {} with role: {}", user.getEmail(), user.getRole());
            
            // Authenticate user
            log.debug("Attempting authentication for user: {}", request.getEmail());
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
            log.debug("Authentication successful for user: {}", request.getEmail());

            // Generate JWT token with company group context
            String jwtToken = jwtService.generateToken(
                user.getEmail(), 
                company.getCompanyGroup().getId(), 
                company.getSlug()
            );

            return AuthResponse.builder()
                .token(jwtToken)
                .user(UserInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .companyId(company.getId())
                    .companyGroupId(company.getCompanyGroup().getId())
                    .companyGroupName(company.getCompanyGroup().getName())
                    .companySlug(company.getSlug())
                    .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                    .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                    .avatarUrl(user.getAvatarUrl())
                    .isOnline(user.getIsOnline() != null ? user.getIsOnline() : false)
                    .build())
                .expiresIn(3600)
                .companyId(company.getId().toString())
                .companyGroupId(company.getCompanyGroup().getId().toString())
                .companySlug(company.getSlug())
                .build();
                
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new com.ruby.rubia_server.auth.AuthenticationException("Invalid credentials");
        }
    }

    public AuthResponse refresh(RefreshRequest request) {
        String userEmail = jwtService.extractUsername(request.getRefreshToken());
        
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new com.ruby.rubia_server.auth.AuthenticationException("User not found"));
            
            if (jwtService.isTokenValid(request.getRefreshToken(), user.getEmail())) {
                String jwtToken = jwtService.generateToken(
                    user.getEmail(),
                    user.getCompany().getCompanyGroup().getId(),
                    user.getCompany().getSlug()
                );
                
                return AuthResponse.builder()
                    .token(jwtToken)
                    .user(UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .companyGroupId(user.getCompany().getCompanyGroup().getId())
                        .companyGroupName(user.getCompany().getCompanyGroup().getName())
                        .companySlug(user.getCompany().getSlug())
                        .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                        .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                        .avatarUrl(user.getAvatarUrl())
                        .isOnline(user.getIsOnline() != null ? user.getIsOnline() : false)
                        .build())
                    .expiresIn(3600)
                    .companyGroupId(user.getCompany().getCompanyGroup().getId().toString())
                    .companySlug(user.getCompany().getSlug())
                    .build();
            }
        }
        
        throw new com.ruby.rubia_server.auth.AuthenticationException("Invalid refresh token");
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}