package com.ruby.rubia_server.core.exception;

/**
 * Exception thrown when a message template transaction fails due to revision creation issues.
 * This exception causes a transaction rollback to maintain data consistency.
 */
public class MessageTemplateTransactionException extends RuntimeException {
    
    private final String templateId;
    private final String operation;
    
    public MessageTemplateTransactionException(String templateId, String operation, String message) {
        super(message);
        this.templateId = templateId;
        this.operation = operation;
    }
    
    public MessageTemplateTransactionException(String templateId, String operation, String message, Throwable cause) {
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