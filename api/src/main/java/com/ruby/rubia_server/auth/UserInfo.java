package com.ruby.rubia_server.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private UUID companyId;
    private UUID companyGroupId;
    private String companyGroupName;
    private String companySlug;
    private UUID departmentId;
    private String departmentName;
    private String avatarUrl;
    private boolean isOnline;
}