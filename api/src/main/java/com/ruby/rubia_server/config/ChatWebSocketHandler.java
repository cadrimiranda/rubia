package com.ruby.rubia_server.config;

import com.ruby.rubia_server.auth.UserInfo;
import com.ruby.rubia_server.auth.WebSocketUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler {

    private final Map<String, UserSessionInfo> userSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Authentication authentication = (Authentication) headerAccessor.getUser();
        if (authentication != null && authentication.getPrincipal() instanceof WebSocketUserPrincipal principal) {
            UserInfo userInfo = principal.getUserInfo();
            UserSessionInfo sessionInfo = UserSessionInfo.builder()
                    .sessionId(sessionId)
                    .userId(userInfo.getId())
                    .companyId(userInfo.getCompanyId())
                    .username(userInfo.getName())
                    .build();
            
            userSessions.put(sessionId, sessionInfo);
            log.info("User connected: {} (session: {}, company: {})", 
                    userInfo.getName(), sessionId, userInfo.getCompanyId());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        UserSessionInfo sessionInfo = userSessions.remove(sessionId);
        if (sessionInfo != null) {
            log.info("User disconnected: {} (session: {})", 
                    sessionInfo.getUsername(), sessionId);
        }
    }

    @MessageMapping("/join")
    public void handleJoinMessage(@Payload Map<String, Object> payload, 
                                  SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        UserSessionInfo sessionInfo = userSessions.get(sessionId);
        
        if (sessionInfo != null) {
            log.info("User {} joined chat (company: {})", 
                    sessionInfo.getUsername(), sessionInfo.getCompanyId());
        }
    }

    public Map<String, UserSessionInfo> getUserSessions() {
        return userSessions;
    }

    public static class UserSessionInfo {
        private String sessionId;
        private UUID userId;
        private UUID companyId;
        private String username;

        public static UserSessionInfoBuilder builder() {
            return new UserSessionInfoBuilder();
        }

        public String getSessionId() { return sessionId; }
        public UUID getUserId() { return userId; }
        public UUID getCompanyId() { return companyId; }
        public String getUsername() { return username; }

        public static class UserSessionInfoBuilder {
            private String sessionId;
            private UUID userId;
            private UUID companyId;
            private String username;

            public UserSessionInfoBuilder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }

            public UserSessionInfoBuilder userId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public UserSessionInfoBuilder companyId(UUID companyId) {
                this.companyId = companyId;
                return this;
            }

            public UserSessionInfoBuilder username(String username) {
                this.username = username;
                return this;
            }

            public UserSessionInfo build() {
                UserSessionInfo sessionInfo = new UserSessionInfo();
                sessionInfo.sessionId = this.sessionId;
                sessionInfo.userId = this.userId;
                sessionInfo.companyId = this.companyId;
                sessionInfo.username = this.username;
                return sessionInfo;
            }
        }
    }
}