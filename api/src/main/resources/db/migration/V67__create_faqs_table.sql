-- Create FAQs table
CREATE TABLE faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    usage_count INTEGER DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id UUID NOT NULL,
    last_edited_by_user_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_faqs_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_faqs_created_by_user
        FOREIGN KEY(created_by_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_faqs_last_edited_by_user
        FOREIGN KEY(last_edited_by_user_id)
        REFERENCES users(id)
);

-- Create FAQ keywords table (for ElementCollection)
CREATE TABLE faq_keywords (
    faq_id UUID NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    CONSTRAINT fk_faq_keywords_faq
        FOREIGN KEY(faq_id)
        REFERENCES faqs(id)
        ON DELETE CASCADE,
    PRIMARY KEY (faq_id, keyword)
);

-- Create FAQ triggers table (for ElementCollection)
CREATE TABLE faq_triggers (
    faq_id UUID NOT NULL,
    trigger_phrase VARCHAR(200) NOT NULL,
    CONSTRAINT fk_faq_triggers_faq
        FOREIGN KEY(faq_id)
        REFERENCES faqs(id)
        ON DELETE CASCADE,
    PRIMARY KEY (faq_id, trigger_phrase)
);

-- Create indexes for better performance
CREATE INDEX idx_faqs_company_id ON faqs(company_id);
CREATE INDEX idx_faqs_is_active ON faqs(is_active);
CREATE INDEX idx_faqs_created_at ON faqs(created_at DESC);
CREATE INDEX idx_faqs_usage_count ON faqs(usage_count DESC);
CREATE INDEX idx_faqs_success_rate ON faqs(success_rate DESC);
CREATE INDEX idx_faqs_deleted_at ON faqs(deleted_at);
CREATE INDEX idx_faqs_company_active_not_deleted ON faqs(company_id, is_active) WHERE deleted_at IS NULL;

-- Indexes for search functionality
CREATE INDEX idx_faqs_question_gin ON faqs USING gin(to_tsvector('portuguese', question));
CREATE INDEX idx_faqs_answer_gin ON faqs USING gin(to_tsvector('portuguese', answer));
CREATE INDEX idx_faq_keywords_keyword ON faq_keywords(keyword);
CREATE INDEX idx_faq_triggers_trigger ON faq_triggers(trigger_phrase);

-- Add unique constraint to prevent duplicate questions per company
CREATE UNIQUE INDEX idx_faqs_unique_question_per_company 
ON faqs(company_id, LOWER(question)) 
WHERE deleted_at IS NULL;

-- Create trigger for updated_at
CREATE TRIGGER update_faqs_updated_at 
    BEFORE UPDATE ON faqs 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE faqs IS 'Frequently Asked Questions for AI chatbot responses';
COMMENT ON COLUMN faqs.question IS 'The question text';
COMMENT ON COLUMN faqs.answer IS 'The answer text that AI can use as reference';
COMMENT ON COLUMN faqs.usage_count IS 'Number of times this FAQ was used by AI';
COMMENT ON COLUMN faqs.success_rate IS 'Success rate percentage when this FAQ was used';
COMMENT ON COLUMN faqs.is_active IS 'Whether this FAQ is active and can be used by AI';
COMMENT ON COLUMN faqs.deleted_at IS 'Soft delete timestamp';
COMMENT ON TABLE faq_keywords IS 'Keywords that help identify when this FAQ is relevant';
COMMENT ON TABLE faq_triggers IS 'Trigger phrases that automatically activate this FAQ';