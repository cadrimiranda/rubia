package com.ruby.rubia_server.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZApiStatus {
    private boolean connected;
    private String session;
    private boolean smartphoneConnected;
    private boolean needsQrCode;
    private String error;
    private Map<String, Object> rawResponse;

    public static ZApiStatus error(String error) {
        return ZApiStatus.builder()
            .connected(false)
            .needsQrCode(true)
            .error(error)
            .build();
    }
}