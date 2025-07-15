-- Adicionar campos email, CPF, RG e fator RH à tabela customers
ALTER TABLE customers 
ADD COLUMN email VARCHAR(255),
ADD COLUMN cpf VARCHAR(14),
ADD COLUMN rg VARCHAR(20),
ADD COLUMN rh_factor VARCHAR(10);

-- Criar índice único para email por empresa (permite mesmo email em empresas diferentes)
CREATE UNIQUE INDEX idx_customers_email_company ON customers(email, company_id) WHERE email IS NOT NULL;

-- Criar índice único para CPF por empresa (permite mesmo CPF em empresas diferentes para casos de migração)
CREATE UNIQUE INDEX idx_customers_cpf_company ON customers(cpf, company_id) WHERE cpf IS NOT NULL;

-- Criar índice para RG (pode haver duplicatas em estados diferentes)
CREATE INDEX idx_customers_rg ON customers(rg) WHERE rg IS NOT NULL;

-- Criar índice para fator RH para consultas rápidas por tipo sanguíneo
CREATE INDEX idx_customers_rh_factor ON customers(rh_factor) WHERE rh_factor IS NOT NULL;