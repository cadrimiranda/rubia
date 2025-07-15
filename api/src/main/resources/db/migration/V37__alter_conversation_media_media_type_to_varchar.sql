-- Change media_type column from MediaType enum to VARCHAR to avoid Hibernate compatibility issues
ALTER TABLE conversation_media ALTER COLUMN media_type TYPE VARCHAR(20);