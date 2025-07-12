-- Adicionar campos de endereço à tabela customers
ALTER TABLE customers 
ADD COLUMN address_street VARCHAR(255),
ADD COLUMN address_number VARCHAR(20),
ADD COLUMN address_complement VARCHAR(255),
ADD COLUMN address_postal_code VARCHAR(10),
ADD COLUMN address_city VARCHAR(100),
ADD COLUMN address_state VARCHAR(2);

-- Criar índice para busca por CEP (pode ser útil para relatórios por região)
CREATE INDEX idx_customers_postal_code ON customers(address_postal_code);

-- Criar índice para busca por cidade/estado
CREATE INDEX idx_customers_city_state ON customers(address_city, address_state);