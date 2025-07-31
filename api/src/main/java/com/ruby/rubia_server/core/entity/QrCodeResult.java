package com.ruby.rubia_server.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResult {
    private boolean success;
    private Object data;
    private String type;
    private String error;

    public static QrCodeResult success(Object data, String type) {
        return QrCodeResult.builder()
            .success(true)
            .data(data)
            .type(type)
            .build();
    }

    public static QrCodeResult error(String error) {
        return QrCodeResult.builder()
            .success(false)
            .error(error)
            .build();
    }
}