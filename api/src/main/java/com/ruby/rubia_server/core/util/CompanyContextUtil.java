package com.ruby.rubia_server.core.util;

import com.ruby.rubia_server.config.CompanyContextResolver;
import com.ruby.rubia_server.config.CustomUserDetailsService;
import com.ruby.rubia_server.core.entity.Company;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyContextUtil {

    private final CompanyContextResolver companyContextResolver;

    /**
     * Extracts company group ID from current request context
     * First tries JWT token, then falls back to subdomain resolution
     */
    public UUID getCurrentCompanyGroupId() {
        HttpServletRequest request = getCurrentHttpRequest();
        
        // Try to get from JWT token first (set by JwtAuthenticationFilter)
        UUID companyGroupIdFromToken = (UUID) request.getAttribute("companyGroupId");
        if (companyGroupIdFromToken != null) {
            return companyGroupIdFromToken;
        }
        
        // Fallback to subdomain resolution - get company from subdomain and return its group ID
        return companyContextResolver.resolveCompany(request)
                .map(company -> company.getCompanyGroup().getId())
                .orElse(null);
    }

    /**
     * Extracts company ID from current request context
     * First tries JWT token, then falls back to subdomain resolution
     */
    public UUID getCurrentCompanyId() {
        HttpServletRequest request = getCurrentHttpRequest();
        
        // Try to get from JWT token first (set by JwtAuthenticationFilter)
        UUID companyGroupIdFromToken = (UUID) request.getAttribute("companyGroupId");
        if (companyGroupIdFromToken != null) {
            // Need to find company by group ID, but this method should return company ID
            return companyContextResolver.resolveCompany(request)
                    .map(Company::getId)
                    .orElse(null);
        }
        
        // Fallback to subdomain resolution
        return companyContextResolver.getCompanyId(request);
    }

    /**
     * Gets the current company entity
     */
    public Company getCurrentCompany() {
        HttpServletRequest request = getCurrentHttpRequest();
        return companyContextResolver.resolveCompany(request)
                .orElseThrow(() -> new IllegalStateException("No company context found. Please check subdomain or authentication."));
    }

    /**
     * Gets the authenticated user's company group ID
     */
    public UUID getAuthenticatedUserCompanyGroupId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal) {
            CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUser().getCompany().getCompanyGroup().getId();
        }
        
        // Fallback to request context
        return getCurrentCompanyGroupId();
    }

    /**
     * Gets the authenticated user's company ID
     */
    public UUID getAuthenticatedUserCompanyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal) {
            CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUser().getCompany().getId();
        }
        
        // Fallback to request context
        return getCurrentCompanyId();
    }

    /**
     * Validates that the provided company group ID matches the current context
     */
    public void validateCompanyGroupContext(UUID providedCompanyGroupId) {
        UUID currentCompanyGroupId = getCurrentCompanyGroupId();
        
        if (currentCompanyGroupId == null) {
            throw new IllegalStateException("No company group context available");
        }
        
        if (!currentCompanyGroupId.equals(providedCompanyGroupId)) {
            throw new SecurityException("Company group ID mismatch. Access denied.");
        }
    }

    /**
     * Validates that the provided company ID matches the current context
     */
    public void validateCompanyContext(UUID providedCompanyId) {
        UUID currentCompanyId = getCurrentCompanyId();
        
        if (currentCompanyId == null) {
            throw new IllegalStateException("No company context available");
        }
        
        if (!currentCompanyId.equals(providedCompanyId)) {
            throw new SecurityException("Company ID mismatch. Access denied.");
        }
    }

    /**
     * Ensures that any entity with a company group ID belongs to the current company group context
     */
    public void ensureCompanyGroupAccess(UUID entityCompanyGroupId) {
        UUID currentCompanyGroupId = getCurrentCompanyGroupId();
        
        if (currentCompanyGroupId == null) {
            throw new IllegalStateException("No company group context available");
        }
        
        if (!currentCompanyGroupId.equals(entityCompanyGroupId)) {
            throw new SecurityException("Access denied. Entity belongs to different company group.");
        }
    }

    /**
     * Ensures that any entity with a company ID belongs to the current company context
     */
    public void ensureCompanyAccess(UUID entityCompanyId) {
        UUID currentCompanyId = getCurrentCompanyId();
        
        if (currentCompanyId == null) {
            throw new IllegalStateException("No company context available");
        }
        
        if (!currentCompanyId.equals(entityCompanyId)) {
            throw new SecurityException("Access denied. Entity belongs to different company.");
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}