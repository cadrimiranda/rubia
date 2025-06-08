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
    private String companyName;
    private String companySlug;
}