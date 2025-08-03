-- Migration to create message enhancement audit table
-- This table tracks all AI message enhancements for auditing and analytics

CREATE TABLE message_enhancement_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ai_agent_id UUID NOT NULL REFERENCES ai_agents(id) ON DELETE CASCADE,
    conversation_id UUID, -- Optional - references conversations(id) but no FK constraint for flexibility
    
    -- Message content
    original_message TEXT NOT NULL,
    enhanced_message TEXT, -- NULL if enhancement failed
    
    -- AI configuration used
    temperament_used VARCHAR(50) NOT NULL,
    ai_model_used VARCHAR(100) NOT NULL,
    temperature_used DECIMAL(3,2) NOT NULL,
    max_tokens_used INTEGER,
    
    -- Performance metrics
    tokens_consumed INTEGER,
    response_time_ms BIGINT,
    
    -- Status tracking
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,
    
    -- Request metadata
    user_agent TEXT,
    ip_address VARCHAR(45), -- Supports both IPv4 and IPv6
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_message_enhancement_audit_company_id ON message_enhancement_audit(company_id);
CREATE INDEX idx_message_enhancement_audit_user_id ON message_enhancement_audit(user_id);
CREATE INDEX idx_message_enhancement_audit_ai_agent_id ON message_enhancement_audit(ai_agent_id);
CREATE INDEX idx_message_enhancement_audit_conversation_id ON message_enhancement_audit(conversation_id);
CREATE INDEX idx_message_enhancement_audit_created_at ON message_enhancement_audit(created_at);
CREATE INDEX idx_message_enhancement_audit_success ON message_enhancement_audit(success);
CREATE INDEX idx_message_enhancement_audit_temperament ON message_enhancement_audit(temperament_used);
CREATE INDEX idx_message_enhancement_audit_ai_model ON message_enhancement_audit(ai_model_used);

-- Composite indexes for common analytics queries
CREATE INDEX idx_message_enhancement_audit_company_date ON message_enhancement_audit(company_id, created_at);
CREATE INDEX idx_message_enhancement_audit_company_success ON message_enhancement_audit(company_id, success);
CREATE INDEX idx_message_enhancement_audit_agent_date ON message_enhancement_audit(ai_agent_id, created_at);

-- Comments for documentation
COMMENT ON TABLE message_enhancement_audit IS 'Audits all AI message enhancements for compliance, analytics and billing';
COMMENT ON COLUMN message_enhancement_audit.conversation_id IS 'Optional conversation context where enhancement was used';
COMMENT ON COLUMN message_enhancement_audit.tokens_consumed IS 'Actual or estimated tokens used by the AI model';
COMMENT ON COLUMN message_enhancement_audit.response_time_ms IS 'Time taken for the AI to process the enhancement request';
COMMENT ON COLUMN message_enhancement_audit.success IS 'Whether the enhancement completed successfully';
COMMENT ON COLUMN message_enhancement_audit.ip_address IS 'Client IP address for security auditing';