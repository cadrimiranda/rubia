-- Converter plan_type de enum para integer (ordinal)
-- BASIC = 0, PREMIUM = 1, ENTERPRISE = 2

-- Adicionar nova coluna temporária
ALTER TABLE companies ADD COLUMN plan_type_temp INTEGER;

-- Converter valores existentes
UPDATE companies SET plan_type_temp = CASE 
    WHEN plan_type = 'BASIC' THEN 0
    WHEN plan_type = 'PREMIUM' THEN 1
    WHEN plan_type = 'ENTERPRISE' THEN 2
    ELSE 0
END;

-- Remover coluna antiga
ALTER TABLE companies DROP COLUMN plan_type;

-- Renomear coluna temporária
ALTER TABLE companies RENAME COLUMN plan_type_temp TO plan_type;

-- Adicionar constraint para garantir valores válidos
ALTER TABLE companies ADD CONSTRAINT chk_plan_type CHECK (plan_type IN (0, 1, 2));

-- Definir valor padrão
ALTER TABLE companies ALTER COLUMN plan_type SET DEFAULT 0;

-- Tornar coluna não nula
ALTER TABLE companies ALTER COLUMN plan_type SET NOT NULL;