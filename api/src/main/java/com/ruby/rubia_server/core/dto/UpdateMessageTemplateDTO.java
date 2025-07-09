package com.ruby.rubia_server.core.dto;

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
public class UpdateMessageTemplateDTO {

    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    private String content;

    @Size(max = 50, message = "Tone must not exceed 50 characters")
    private String tone;
}