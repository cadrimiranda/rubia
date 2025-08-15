package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    
    private UUID id;
    private UUID companyId;
    private UUID customerId;
    private String customerName;
    private String customerPhone;
    private LocalDate customerBirthDate;
    private String customerBloodType;
    private LocalDate customerLastDonationDate;
    private Integer customerHeight;
    private Double customerWeight;
    private UUID assignedUserId;
    private String assignedUserName;
    private UUID campaignId;
    private String campaignName;
    private ConversationStatus status;
    private Channel channel;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MessageDTO lastMessage;
    private Long unreadCount;
    private String chatLid;
    private Boolean aiAutoResponseEnabled;
    private Integer aiMessageLimit;
    private Integer aiMessagesUsed;
    private LocalDateTime aiLimitReachedAt;
}