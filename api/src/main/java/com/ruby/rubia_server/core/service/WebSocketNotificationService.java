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
            log.info("Sent new message notification for conversation {} to company {}", 
                    conversation.getId(), conversation.getCompanyId());
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
            log.info("Sent conversation update notification for conversation {} to company {}", 
                    conversation.getId(), conversation.getCompanyId());
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
            log.debug("Sent typing notification for conversation {} by user {}", 
                    conversationId, userName);
        } catch (Exception e) {
            log.error("Error sending typing notification: {}", e.getMessage(), e);
        }
    }

    private void sendToCompanyUsers(UUID companyId, String destination, Object notification) {
        webSocketHandler.getUserSessions().values().stream()
                .filter(session -> session.getCompanyId().equals(companyId))
                .forEach(session -> {
                    try {
                        messagingTemplate.convertAndSendToUser(
                                session.getSessionId(), 
                                destination, 
                                notification
                        );
                    } catch (Exception e) {
                        log.warn("Failed to send notification to session {}: {}", 
                                session.getSessionId(), e.getMessage());
                    }
                });
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
}