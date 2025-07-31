package com.ruby.rubia_server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Only log webhook requests for debugging
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/webhook") || requestURI.contains("/messaging")) {
            
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            
            // Removed excessive logging for cleaner output
            
            try {
                filterChain.doFilter(wrappedRequest, wrappedResponse);
                
                // Minimal logging only for errors
                
                // Copy response content back to original response
                wrappedResponse.copyBodyToResponse();
                
            } catch (Exception e) {
                log.error("🌐 Error processing request: {}", e.getMessage(), e);
                throw e;
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}