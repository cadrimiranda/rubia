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
            
            log.info("🌐 === INCOMING REQUEST DEBUG ===");
            log.info("🌐 Method: {}", request.getMethod());
            log.info("🌐 URI: {}", request.getRequestURI());
            log.info("🌐 Query: {}", request.getQueryString());
            log.info("🌐 Remote IP: {}", request.getRemoteAddr());
            log.info("🌐 User-Agent: {}", request.getHeader("User-Agent"));
            log.info("🌐 Content-Type: {}", request.getContentType());
            log.info("🌐 Content-Length: {}", request.getContentLength());
            
            // Log all headers
            log.info("🌐 Headers:");
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                log.info("🌐   {}: {}", headerName, request.getHeader(headerName));
            });
            
            try {
                filterChain.doFilter(wrappedRequest, wrappedResponse);
                
                // Log request body if present
                byte[] requestBody = wrappedRequest.getContentAsByteArray();
                if (requestBody.length > 0) {
                    String body = new String(requestBody, StandardCharsets.UTF_8);
                    log.info("🌐 Request Body: {}", body);
                }
                
                log.info("🌐 Response Status: {}", wrappedResponse.getStatus());
                
                // Copy response content back to original response
                wrappedResponse.copyBodyToResponse();
                
            } catch (Exception e) {
                log.error("🌐 Error processing request: {}", e.getMessage(), e);
                throw e;
            }
            
            log.info("🌐 === REQUEST DEBUG COMPLETE ===");
        } else {
            filterChain.doFilter(request, response);
        }
    }
}