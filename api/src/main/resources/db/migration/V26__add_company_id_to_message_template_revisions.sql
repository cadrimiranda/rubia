ALTER TABLE message_template_revisions 
ADD COLUMN company_id UUID NOT NULL;

-- Add foreign key constraint
ALTER TABLE message_template_revisions 
ADD CONSTRAINT fk_message_template_revisions_company_id 
FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;