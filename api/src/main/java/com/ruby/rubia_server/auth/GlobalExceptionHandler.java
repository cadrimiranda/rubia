package com.ruby.rubia_server.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "error", "Authentication failed",
                    "message", ex.getMessage(),
                    "status", "401"
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        if (message != null) {
            if (message.contains("Company not found")) {
                status = HttpStatus.BAD_REQUEST;
            } else if (message.contains("User not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (message.contains("Invalid credentials") || message.contains("Invalid refresh token")) {
                status = HttpStatus.UNAUTHORIZED;
            }
        }
        
        return ResponseEntity.status(status)
                .body(Map.of(
                    "error", "Request failed",
                    "message", message != null ? message : "An unexpected error occurred",
                    "status", String.valueOf(status.value())
                ));
    }
}