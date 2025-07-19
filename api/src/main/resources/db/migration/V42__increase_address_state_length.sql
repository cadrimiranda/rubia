-- Increase address_state column length from VARCHAR(2) to VARCHAR(20)
-- to accommodate both state abbreviations and full state names during imports
ALTER TABLE customers 
ALTER COLUMN address_state TYPE VARCHAR(20);