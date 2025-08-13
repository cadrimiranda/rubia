-- Create message_drafts table for AI-generated draft messages
CREATE TABLE message_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    conversation_id UUID NOT NULL,
    content TEXT NOT NULL,
    ai_model VARCHAR(50),
    confidence DECIMAL(3,2) CHECK (confidence >= 0 AND confidence <= 1),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source_type VARCHAR(50),
    source_id UUID,
    created_by_id UUID,
    reviewed_by_id UUID,
    reviewed_at TIMESTAMP,
    original_message TEXT,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    
    CONSTRAINT fk_message_drafts_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_message_drafts_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_message_drafts_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT fk_message_drafts_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users(id),
    
    CONSTRAINT chk_message_drafts_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EDITED')),
    CONSTRAINT chk_message_drafts_source_type CHECK (source_type IN ('FAQ', 'TEMPLATE', 'AI_GENERATED', NULL))
);

-- Create indexes for better performance
CREATE INDEX idx_message_drafts_company_id ON message_drafts(company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_message_drafts_conversation_id ON message_drafts(conversation_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_message_drafts_status ON message_drafts(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_message_drafts_created_at ON message_drafts(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_message_drafts_reviewed_by ON message_drafts(reviewed_by_id) WHERE deleted_at IS NULL;

-- Composite indexes for common queries
CREATE INDEX idx_message_drafts_company_status ON message_drafts(company_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_message_drafts_conversation_status ON message_drafts(conversation_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_message_drafts_source ON message_drafts(source_type, source_id) WHERE deleted_at IS NULL;

-- Create trigger to update updated_at column
CREATE OR REPLACE FUNCTION update_message_drafts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_message_drafts_updated_at
    BEFORE UPDATE ON message_drafts
    FOR EACH ROW
    EXECUTE FUNCTION update_message_drafts_updated_at();

-- Add comments for documentation
COMMENT ON TABLE message_drafts IS 'AI-generated draft messages for operator review and approval';
COMMENT ON COLUMN message_drafts.confidence IS 'AI confidence score from 0.0 to 1.0';
COMMENT ON COLUMN message_drafts.status IS 'Draft status: PENDING, APPROVED, REJECTED, or EDITED';
COMMENT ON COLUMN message_drafts.source_type IS 'Source type: FAQ, TEMPLATE, or AI_GENERATED';
COMMENT ON COLUMN message_drafts.source_id IS 'ID of the source FAQ or template used to generate this draft';
COMMENT ON COLUMN message_drafts.original_message IS 'Original user message that triggered this draft generation';