package com.ruby.rubia_server.auth;

import java.security.Principal;

/**
 * Principal class for WebSocket authentication that uses userId as the principal name
 * This ensures Spring WebSocket's user-based messaging works correctly
 */
public class WebSocketUserPrincipal implements Principal {
    
    private final UserInfo userInfo;
    
    public WebSocketUserPrincipal(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
    
    @Override
    public String getName() {
        // Return userId as the principal name for WebSocket user-based messaging
        return userInfo.getId().toString();
    }
    
    public UserInfo getUserInfo() {
        return userInfo;
    }
}