-- Alter campaign date columns from timestamp to date
-- This migration changes start_date and end_date from timestamp to date type
-- since campaigns typically work with dates rather than specific times

ALTER TABLE campaigns 
    ALTER COLUMN start_date TYPE DATE,
    ALTER COLUMN end_date TYPE DATE;