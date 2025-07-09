package com.ruby.rubia_server.core.exception;

/**
 * Exception thrown when there's an error creating or managing message template revisions.
 * This is a recoverable exception that should be handled gracefully by the application.
 */
public class MessageTemplateRevisionException extends Exception {
    
    private final String templateId;
    private final String operation;
    
    public MessageTemplateRevisionException(String templateId, String operation, String message) {
        super(message);
        this.templateId = templateId;
        this.operation = operation;
    }
    
    public MessageTemplateRevisionException(String templateId, String operation, String message, Throwable cause) {
        super(message, cause);
        this.templateId = templateId;
        this.operation = operation;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public String getOperation() {
        return operation;
    }
}