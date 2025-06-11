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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.FORBIDDEN; // 403
        
        if (message != null && message.contains("No company context")) {
            return ResponseEntity.status(status)
                    .body(Map.of(
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Você não tem permissão para realizar esta ação",
                        "code", "INSUFFICIENT_PERMISSIONS",
                        "status", String.valueOf(status.value())
                    ));
        }
        
        return ResponseEntity.status(status)
                .body(Map.of(
                    "error", "Forbidden",
                    "message", message != null ? message : "Access denied",
                    "status", String.valueOf(status.value())
                ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException ex) {
        log.error("SecurityException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "INSUFFICIENT_PERMISSIONS",
                    "message", "Você não tem permissão para realizar esta ação",
                    "code", "INSUFFICIENT_PERMISSIONS",
                    "status", "403"
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