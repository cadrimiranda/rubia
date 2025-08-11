package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.ChatWebSocketHandler;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatWebSocketHandler webSocketHandler;

    public void notifyNewMessage(MessageDTO message, ConversationDTO conversation) {
        try {
            NewMessageNotification notification = NewMessageNotification.builder()
                    .type("NEW_MESSAGE")
                    .message(message)
                    .conversation(conversation)
                    .build();

            sendToCompanyUsers(conversation.getCompanyId(), "/topic/messages", notification);
            
        } catch (Exception e) {
            log.error("Error sending new message notification: {}", e.getMessage(), e);
        }
    }

    public void notifyConversationUpdate(ConversationDTO conversation) {
        try {
            ConversationUpdateNotification notification = ConversationUpdateNotification.builder()
                    .type("CONVERSATION_UPDATE")
                    .conversation(conversation)
                    .build();

            sendToCompanyUsers(conversation.getCompanyId(), "/topic/conversations", notification);
            
        } catch (Exception e) {
            log.error("Error sending conversation update notification: {}", e.getMessage(), e);
        }
    }

    public void notifyTypingStatus(UUID conversationId, UUID companyId, String userName, boolean isTyping) {
        try {
            TypingNotification notification = TypingNotification.builder()
                    .type("TYPING_STATUS")
                    .conversationId(conversationId)
                    .userName(userName)
                    .isTyping(isTyping)
                    .build();

            sendToCompanyUsers(companyId, "/topic/typing", notification);
            
        } catch (Exception e) {
            log.error("Error sending typing notification: {}", e.getMessage(), e);
        }
    }

    private void sendToCompanyUsers(UUID companyId, String destination, Object notification) {
        var sessions = webSocketHandler.getUserSessions();
        log.info("📡 Trying to send to company {} - Total sessions: {}", companyId, sessions.size());
        
        // Group sessions by userId to avoid sending duplicates to same user
        var uniqueUsers = sessions.values().stream()
                .filter(session -> session.getCompanyId().equals(companyId))
                .collect(java.util.stream.Collectors.toMap(
                    session -> session.getUserId(),
                    session -> session,
                    (existing, replacement) -> existing // Keep first session for each user
                ));
        
        log.info("📡 Unique users for company {}: {}", companyId, uniqueUsers.size());
        
        uniqueUsers.values().forEach(session -> {
            try {
                // For Spring WebSocket user-based messaging, we need to use the principal name
                // The principal name should be the user's unique identifier that was set during authentication
                // The WebSocketUserPrincipal.getName() returns the userId as string
                String principalName = session.getUserId().toString();
                
                
                messagingTemplate.convertAndSendToUser(
                        principalName, 
                        destination, 
                        notification
                );
                
            } catch (Exception e) {
                log.error("❌ Failed to send notification to user {}: {}", 
                        session.getUsername(), e.getMessage(), e);
            }
        });
    }

    public void sendToChannel(String channel, Map<String, Object> notification) {
        try {
            
            
            // For company-specific channels, extract the company ID
            if (channel.startsWith("company-")) {
                String companyIdStr = channel.substring("company-".length());
                UUID companyId = UUID.fromString(companyIdStr);
                
                sendToCompanyUsers(companyId, "/topic/instance-status", notification);
                
            } else {
                // For other channels, send as broadcast
                
                messagingTemplate.convertAndSend("/topic/" + channel, notification);
                
            }
        } catch (Exception e) {
            log.error("❌ Error sending notification to channel {}: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Send unread count update to user
     */
    public void sendUnreadCountUpdate(UUID userId, UUID conversationId, Integer count) {
        try {
            log.debug("Sending unread count update to user {}: conversation {} has {} unread messages", 
                    userId, conversationId, count);
            
            String principalName = userId.toString();
            
            UnreadCountUpdateMessage message = UnreadCountUpdateMessage.builder()
                    .type("UNREAD_COUNT_UPDATE")
                    .conversationId(conversationId)
                    .userId(userId)
                    .count(count)
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                    principalName, 
                    "/topic/unread-counts", 
                    message
            );
            
        } catch (Exception e) {
            log.error("Error sending unread count update to user {}: {}", userId, e.getMessage(), e);
        }
    }

    public static class NewMessageNotification {
        private String type;
        private MessageDTO message;
        private ConversationDTO conversation;

        public static NewMessageNotificationBuilder builder() {
            return new NewMessageNotificationBuilder();
        }

        public String getType() { return type; }
        public MessageDTO getMessage() { return message; }
        public ConversationDTO getConversation() { return conversation; }

        public static class NewMessageNotificationBuilder {
            private String type;
            private MessageDTO message;
            private ConversationDTO conversation;

            public NewMessageNotificationBuilder type(String type) {
                this.type = type;
                return this;
            }

            public NewMessageNotificationBuilder message(MessageDTO message) {
                this.message = message;
                return this;
            }

            public NewMessageNotificationBuilder conversation(ConversationDTO conversation) {
                this.conversation = conversation;
                return this;
            }

            public NewMessageNotification build() {
                NewMessageNotification notification = new NewMessageNotification();
                notification.type = this.type;
                notification.message = this.message;
                notification.conversation = this.conversation;
                return notification;
            }
        }
    }

    public static class ConversationUpdateNotification {
        private String type;
        private ConversationDTO conversation;

        public static ConversationUpdateNotificationBuilder builder() {
            return new ConversationUpdateNotificationBuilder();
        }

        public String getType() { return type; }
        public ConversationDTO getConversation() { return conversation; }

        public static class ConversationUpdateNotificationBuilder {
            private String type;
            private ConversationDTO conversation;

            public ConversationUpdateNotificationBuilder type(String type) {
                this.type = type;
                return this;
            }

            public ConversationUpdateNotificationBuilder conversation(ConversationDTO conversation) {
                this.conversation = conversation;
                return this;
            }

            public ConversationUpdateNotification build() {
                ConversationUpdateNotification notification = new ConversationUpdateNotification();
                notification.type = this.type;
                notification.conversation = this.conversation;
                return notification;
            }
        }
    }

    public static class TypingNotification {
        private String type;
        private UUID conversationId;
        private String userName;
        private boolean isTyping;

        public static TypingNotificationBuilder builder() {
            return new TypingNotificationBuilder();
        }

        public String getType() { return type; }
        public UUID getConversationId() { return conversationId; }
        public String getUserName() { return userName; }
        public boolean isTyping() { return isTyping; }

        public static class TypingNotificationBuilder {
            private String type;
            private UUID conversationId;
            private String userName;
            private boolean isTyping;

            public TypingNotificationBuilder type(String type) {
                this.type = type;
                return this;
            }

            public TypingNotificationBuilder conversationId(UUID conversationId) {
                this.conversationId = conversationId;
                return this;
            }

            public TypingNotificationBuilder userName(String userName) {
                this.userName = userName;
                return this;
            }

            public TypingNotificationBuilder isTyping(boolean isTyping) {
                this.isTyping = isTyping;
                return this;
            }

            public TypingNotification build() {
                TypingNotification notification = new TypingNotification();
                notification.type = this.type;
                notification.conversationId = this.conversationId;
                notification.userName = this.userName;
                notification.isTyping = this.isTyping;
                return notification;
            }
        }
    }

    public static class UnreadCountUpdateMessage {
        private String type;
        private UUID conversationId;
        private UUID userId;
        private Integer count;

        public static UnreadCountUpdateMessageBuilder builder() {
            return new UnreadCountUpdateMessageBuilder();
        }

        public String getType() { return type; }
        public UUID getConversationId() { return conversationId; }
        public UUID getUserId() { return userId; }
        public Integer getCount() { return count; }

        public static class UnreadCountUpdateMessageBuilder {
            private String type;
            private UUID conversationId;
            private UUID userId;
            private Integer count;

            public UnreadCountUpdateMessageBuilder type(String type) {
                this.type = type;
                return this;
            }

            public UnreadCountUpdateMessageBuilder conversationId(UUID conversationId) {
                this.conversationId = conversationId;
                return this;
            }

            public UnreadCountUpdateMessageBuilder userId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public UnreadCountUpdateMessageBuilder count(Integer count) {
                this.count = count;
                return this;
            }

            public UnreadCountUpdateMessage build() {
                UnreadCountUpdateMessage message = new UnreadCountUpdateMessage();
                message.type = this.type;
                message.conversationId = this.conversationId;
                message.userId = this.userId;
                message.count = this.count;
                return message;
            }
        }
    }
}