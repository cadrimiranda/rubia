package com.ruby.rubia_server.auth;

import com.ruby.rubia_server.config.CompanyContextResolver;
import com.ruby.rubia_server.config.JwtService;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
@RequiredArgsConstructor
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
            if (companyOpt.isEmpty()) {
                throw new RuntimeException("Company not found. Please check the subdomain.");
            }
            
            Company company = companyOpt.get();
            
            // Find user by email and company
            Optional<User> userOpt = userRepository.findByEmailAndCompanyId(request.getEmail(), company.getId());
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found for this company");
            }
            
            User user = userOpt.get();
            
            // Authenticate user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            // Generate JWT token with company context
            String jwtToken = jwtService.generateToken(
                user.getEmail(), 
                company.getId(), 
                company.getSlug()
            );

            return AuthResponse.builder()
                .accessToken(jwtToken)
                .user(UserInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .companyId(company.getId())
                    .companyName(company.getName())
                    .companySlug(company.getSlug())
                    .build())
                .build();
                
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    public AuthResponse refresh(RefreshRequest request) {
        String userEmail = jwtService.extractUsername(request.getRefreshToken());
        
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (jwtService.isTokenValid(request.getRefreshToken(), user.getEmail())) {
                String jwtToken = jwtService.generateToken(
                    user.getEmail(),
                    user.getCompany().getId(),
                    user.getCompany().getSlug()
                );
                
                return AuthResponse.builder()
                    .accessToken(jwtToken)
                    .user(UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .companyId(user.getCompany().getId())
                        .companyName(user.getCompany().getName())
                        .companySlug(user.getCompany().getSlug())
                        .build())
                    .build();
            }
        }
        
        throw new RuntimeException("Invalid refresh token");
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}