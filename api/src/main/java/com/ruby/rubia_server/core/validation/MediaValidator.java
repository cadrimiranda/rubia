package com.ruby.rubia_server.core.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class MediaValidator {
    
    @Value("${zapi.media.max-file-size:20971520}")
    private long maxFileSize;
    
    @Value("#{'${zapi.media.allowed-types:image/jpeg,image/png,application/pdf,video/mp4,audio/mpeg}'.split(',')}")
    private List<String> allowedTypes;
    
    public boolean isValidMedia(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        
        if (file.getSize() > maxFileSize) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && allowedTypes.contains(contentType);
    }
    
    public String getValidationError(MultipartFile file) {
        if (file.isEmpty()) {
            return "Arquivo vazio";
        }
        
        if (file.getSize() > maxFileSize) {
            return "Arquivo muito grande. Máximo: " + (maxFileSize / 1024 / 1024) + "MB";
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            return "Tipo de arquivo não permitido";
        }
        
        return null;
    }
    
    public boolean isValidBase64(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return false;
        }
        
        return base64Data.matches("^data:[a-zA-Z0-9]+/[a-zA-Z0-9]+;base64,.*");
    }
}