package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMediaDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID conversationId;
    private String fileUrl;
    private MediaType mediaType;
    private String mimeType;
    private String originalFileName;
    private Long fileSizeBytes;
    private String checksum;
    private UUID uploadedByUserId;
    private String uploadedByUserName;
    private UUID uploadedByCustomerId;
    private String uploadedByCustomerName;
    private LocalDateTime uploadedAt;
}