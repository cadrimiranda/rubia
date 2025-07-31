package com.ruby.rubia_server.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneCodeResult {
    private boolean success;
    private String code;
    private String phoneNumber;
    private String error;

    public static PhoneCodeResult success(String code, String phoneNumber) {
        return PhoneCodeResult.builder()
            .success(true)
            .code(code)
            .phoneNumber(phoneNumber)
            .build();
    }

    public static PhoneCodeResult error(String error) {
        return PhoneCodeResult.builder()
            .success(false)
            .error(error)
            .build();
    }
}