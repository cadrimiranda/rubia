-- Add revision_type column to message_template_revisions table
ALTER TABLE message_template_revisions
ADD COLUMN revision_type RevisionType NOT NULL DEFAULT 'EDIT';

-- Update existing records to have appropriate revision types
-- The first revision for each template should be CREATE
UPDATE message_template_revisions
SET revision_type = 'CREATE'
WHERE revision_number = 1;

-- All other existing revisions remain as EDIT (default)
-- DELETE and RESTORE types will be set by the application going forward