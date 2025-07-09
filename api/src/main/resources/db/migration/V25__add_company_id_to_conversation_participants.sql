ALTER TABLE conversation_participants 
ADD COLUMN company_id UUID NOT NULL;

-- Add foreign key constraint
ALTER TABLE conversation_participants 
ADD CONSTRAINT fk_conversation_participants_company_id 
FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;