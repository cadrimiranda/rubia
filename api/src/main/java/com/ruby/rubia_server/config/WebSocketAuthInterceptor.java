package com.ruby.rubia_server.config;

import com.ruby.rubia_server.auth.UserInfo;
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
            String token = accessor.getFirstNativeHeader("Authorization");
            
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    String jwt = token.substring(7);
                    String username = jwtService.extractUsername(jwt);
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (username != null && jwtService.isTokenValid(jwt, username)) {
                        // Get user from database to build UserInfo
                        User user = userRepository.findByEmail(username).orElse(null);
                        if (user != null) {
                            UserInfo userInfo = UserInfo.builder()
                                    .id(user.getId())
                                    .name(user.getName())
                                    .email(user.getEmail())
                                    .role(user.getRole().name())
                                    .companyId(user.getCompany().getId())
                                    .companyGroupId(user.getCompany().getCompanyGroup().getId())
                                    .companySlug(user.getCompany().getSlug())
                                    .departmentId(user.getDepartment().getId())
                                    .departmentName(user.getDepartment().getName())
                                    .avatarUrl(user.getAvatarUrl())
                                    .isOnline(true)
                                    .build();
                                    
                            UsernamePasswordAuthenticationToken authToken = 
                                new UsernamePasswordAuthenticationToken(userInfo, null, userDetails.getAuthorities());
                            
                            accessor.setUser(authToken);
                            log.debug("WebSocket connection authenticated for user: {}", username);
                        }
                    }
                } catch (Exception e) {
                    log.warn("WebSocket authentication failed: {}", e.getMessage());
                }
            }
        }
        
        return message;
    }
}