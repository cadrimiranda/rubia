-- Add revision_type column to message_template_revisions table
-- Using integer to match EnumType.ORDINAL mapping: 0=CREATE, 1=EDIT, 2=DELETE, 3=RESTORE
ALTER TABLE message_template_revisions
ADD COLUMN revision_type INTEGER NOT NULL DEFAULT 1;

-- Update existing records to have appropriate revision types
-- The first revision for each template should be CREATE (ordinal 0)
UPDATE message_template_revisions
SET revision_type = 0
WHERE revision_number = 1;

-- All other existing revisions remain as EDIT (ordinal 1, default)
-- DELETE (ordinal 2) and RESTORE (ordinal 3) types will be set by the application going forward