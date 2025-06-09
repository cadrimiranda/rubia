-- V10: Add medical fields to users table for blood center functionality

ALTER TABLE users 
ADD COLUMN birth_date DATE,
ADD COLUMN weight DOUBLE PRECISION,
ADD COLUMN height DOUBLE PRECISION,
ADD COLUMN address TEXT;

-- Add comments for clarity
COMMENT ON COLUMN users.birth_date IS 'Date of birth of the user/donor';
COMMENT ON COLUMN users.weight IS 'Weight in kilograms';
COMMENT ON COLUMN users.height IS 'Height in centimeters';
COMMENT ON COLUMN users.address IS 'Full address of the user/donor';