-- Converter role de enum para integer (ordinal)
-- ADMIN = 0, SUPERVISOR = 1, AGENT = 2

-- Adicionar nova coluna temporária
ALTER TABLE users ADD COLUMN role_temp INTEGER;

-- Converter valores existentes
UPDATE users SET role_temp = CASE 
    WHEN role = 'ADMIN' THEN 0
    WHEN role = 'SUPERVISOR' THEN 1
    WHEN role = 'AGENT' THEN 2
    ELSE 2 -- Default para AGENT
END;

-- Remover coluna antiga
ALTER TABLE users DROP COLUMN role;

-- Renomear coluna temporária
ALTER TABLE users RENAME COLUMN role_temp TO role;

-- Adicionar constraint para garantir valores válidos
ALTER TABLE users ADD CONSTRAINT chk_role CHECK (role IN (0, 1, 2));

-- Tornar coluna não nula
ALTER TABLE users ALTER COLUMN role SET NOT NULL;