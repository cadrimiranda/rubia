package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationMediaDTO {

    @Size(max = 2000, message = "File URL must not exceed 2000 characters")
    private String fileUrl;

    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    private String mimeType;

    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    private String originalFileName;

    private Long fileSizeBytes;

    @Size(max = 64, message = "Checksum must not exceed 64 characters")
    private String checksum;
}