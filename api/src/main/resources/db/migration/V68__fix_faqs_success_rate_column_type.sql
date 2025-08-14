-- Fix success_rate column type in faqs table to match Hibernate expectations
ALTER TABLE faqs ALTER COLUMN success_rate TYPE DOUBLE PRECISION;