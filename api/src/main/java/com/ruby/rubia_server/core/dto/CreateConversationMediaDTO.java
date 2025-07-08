package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationMediaDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotBlank(message = "File URL is required")
    @Size(max = 2000, message = "File URL must not exceed 2000 characters")
    private String fileUrl;

    @NotNull(message = "Media type is required")
    private MediaType mediaType;

    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    private String mimeType;

    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    private String originalFileName;

    private Long fileSizeBytes;

    @Size(max = 64, message = "Checksum must not exceed 64 characters")
    private String checksum;

    private UUID uploadedByUserId;

    private UUID uploadedByCustomerId;
}