package com.ruby.rubia_server.config;

import com.ruby.rubia_server.auth.UserInfo;
import com.ruby.rubia_server.auth.WebSocketUserPrincipal;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("=== WEBSOCKET CONNECTION ATTEMPT ===");
            String token = accessor.getFirstNativeHeader("Authorization");
            log.info("Authorization header present: {}", token != null);
            log.info("Token value: {}", token != null ? token.substring(0, Math.min(30, token.length())) + "..." : "null");
            
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    String jwt = token.substring(7);
                    String username = jwtService.extractUsername(jwt);
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (username != null && jwtService.isTokenValid(jwt, username)) {
                        // Get user from database to build UserInfo
                        User user = userRepository.findByEmailWithCompanyAndDepartment(username).orElse(null);
                        if (user != null) {
                            UserInfo userInfo = UserInfo.builder()
                                    .id(user.getId())
                                    .name(user.getName())
                                    .email(user.getEmail())
                                    .role(user.getRole().name())
                                    .companyId(user.getCompany().getId())
                                    .companyGroupId(user.getCompany().getCompanyGroup() != null ? 
                                        user.getCompany().getCompanyGroup().getId() : null)
                                    .companySlug(user.getCompany().getSlug())
                                    .departmentId(user.getDepartment() != null ? 
                                        user.getDepartment().getId() : null)
                                    .departmentName(user.getDepartment() != null ? 
                                        user.getDepartment().getName() : null)
                                    .avatarUrl(user.getAvatarUrl())
                                    .isOnline(true)
                                    .build();
                                    
                            // Use WebSocketUserPrincipal to ensure correct principal name for user-based messaging
                            WebSocketUserPrincipal principal = new WebSocketUserPrincipal(userInfo);
                            UsernamePasswordAuthenticationToken authToken = 
                                new UsernamePasswordAuthenticationToken(principal, null, userDetails.getAuthorities());
                            
                            accessor.setUser(authToken);
                            log.info("✅ WebSocket connection authenticated for user: {} (principal name: {})", 
                                    username, principal.getName());
                        } else {
                            log.warn("❌ User not found in database: {}", username);
                        }
                    } else {
                        log.warn("❌ Invalid JWT token for user: {}", username);
                    }
                } catch (Exception e) {
                    log.error("❌ WebSocket authentication failed: {}", e.getMessage(), e);
                }
            } else {
                log.warn("❌ No valid Authorization header found");
            }
        }
        
        return message;
    }
}