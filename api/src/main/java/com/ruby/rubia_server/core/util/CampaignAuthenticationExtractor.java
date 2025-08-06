package com.ruby.rubia_server.core.util;

import com.ruby.rubia_server.core.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignAuthenticationExtractor {

    private final CompanyContextUtil companyContextUtil;

    public CampaignUserContext extractFromAuthentication(Authentication authentication) {
        try {
            User user = companyContextUtil.getAuthenticatedUser();
            UUID companyId = companyContextUtil.getAuthenticatedUserCompanyId();
            UUID companyGroupId = companyContextUtil.getAuthenticatedUserCompanyGroupId();

            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            log.debug("Extracted user context: userId={}, companyId={}, roles={}", 
                    user.getId(), companyId, roles);

            return CampaignUserContext.builder()
                    .userId(user.getId().toString())
                    .companyId(companyId.toString())
                    .companyGroupId(companyGroupId.toString())
                    .userEmail(user.getEmail())
                    .userName(user.getName())
                    .roles(roles)
                    .build();

        } catch (Exception e) {
            log.error("Failed to extract user context from authentication: {}", e.getMessage(), e);
            throw new SecurityException("Unable to extract user context", e);
        }
    }

    public static class CampaignUserContext {
        private final String userId;
        private final String companyId;
        private final String companyGroupId;
        private final String userEmail;
        private final String userName;
        private final Set<String> roles;

        private CampaignUserContext(Builder builder) {
            this.userId = builder.userId;
            this.companyId = builder.companyId;
            this.companyGroupId = builder.companyGroupId;
            this.userEmail = builder.userEmail;
            this.userName = builder.userName;
            this.roles = builder.roles;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getUserId() { return userId; }
        public String getCompanyId() { return companyId; }
        public String getCompanyGroupId() { return companyGroupId; }
        public String getUserEmail() { return userEmail; }
        public String getUserName() { return userName; }
        public Set<String> getRoles() { return roles; }

        public static class Builder {
            private String userId;
            private String companyId;
            private String companyGroupId;
            private String userEmail;
            private String userName;
            private Set<String> roles;

            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder companyId(String companyId) {
                this.companyId = companyId;
                return this;
            }

            public Builder companyGroupId(String companyGroupId) {
                this.companyGroupId = companyGroupId;
                return this;
            }

            public Builder userEmail(String userEmail) {
                this.userEmail = userEmail;
                return this;
            }

            public Builder userName(String userName) {
                this.userName = userName;
                return this;
            }

            public Builder roles(Set<String> roles) {
                this.roles = roles;
                return this;
            }

            public CampaignUserContext build() {
                return new CampaignUserContext(this);
            }
        }
    }
}