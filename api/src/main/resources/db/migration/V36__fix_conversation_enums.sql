-- Converter status e channel de enum para integer (ordinal)
-- ConversationStatus: ENTRADA = 0, ESPERANDO = 1, FINALIZADOS = 2
-- Channel: WHATSAPP = 0, INSTAGRAM = 1, FACEBOOK = 2, WEB_CHAT = 3, EMAIL = 4

-- 1. Converter status de enum para integer
ALTER TABLE conversations ADD COLUMN status_temp INTEGER;

-- Converter valores existentes (se houver)
UPDATE conversations SET status_temp = CASE 
    WHEN status = 'OPEN' THEN 0 -- ENTRADA
    WHEN status = 'PENDING' THEN 1 -- ESPERANDO  
    WHEN status = 'CLOSED' THEN 2 -- FINALIZADOS
    ELSE 0 -- Default para ENTRADA
END;

-- Remover coluna antiga
ALTER TABLE conversations DROP COLUMN status;

-- Renomear coluna temporária
ALTER TABLE conversations RENAME COLUMN status_temp TO status;

-- Adicionar constraint para garantir valores válidos
ALTER TABLE conversations ADD CONSTRAINT chk_status CHECK (status IN (0, 1, 2));

-- Tornar coluna não nula
ALTER TABLE conversations ALTER COLUMN status SET NOT NULL;

-- Definir valor padrão
ALTER TABLE conversations ALTER COLUMN status SET DEFAULT 0;

-- 2. Converter channel de enum para integer
ALTER TABLE conversations ADD COLUMN channel_temp INTEGER;

-- Converter valores existentes (se houver)
UPDATE conversations SET channel_temp = CASE 
    WHEN channel = 'WHATSAPP' THEN 0
    ELSE 0 -- Default para WHATSAPP
END;

-- Remover coluna antiga
ALTER TABLE conversations DROP COLUMN channel;

-- Renomear coluna temporária
ALTER TABLE conversations RENAME COLUMN channel_temp TO channel;

-- Adicionar constraint para garantir valores válidos
ALTER TABLE conversations ADD CONSTRAINT chk_channel CHECK (channel IN (0, 1, 2, 3, 4));

-- Tornar coluna não nula
ALTER TABLE conversations ALTER COLUMN channel SET NOT NULL;

-- Definir valor padrão
ALTER TABLE conversations ALTER COLUMN channel SET DEFAULT 0;

-- 3. Converter conversation_type de enum para integer
ALTER TABLE conversations ADD COLUMN conversation_type_temp INTEGER;

-- Converter valores existentes (se houver)
UPDATE conversations SET conversation_type_temp = CASE 
    WHEN conversation_type = 'ONE_TO_ONE' THEN 0
    WHEN conversation_type = 'GROUP_CHAT' THEN 1
    ELSE 0 -- Default para ONE_TO_ONE
END;

-- Remover coluna antiga
ALTER TABLE conversations DROP COLUMN conversation_type;

-- Renomear coluna temporária
ALTER TABLE conversations RENAME COLUMN conversation_type_temp TO conversation_type;

-- Adicionar constraint para garantir valores válidos
ALTER TABLE conversations ADD CONSTRAINT chk_conversation_type CHECK (conversation_type IN (0, 1));

-- Tornar coluna não nula
ALTER TABLE conversations ALTER COLUMN conversation_type SET NOT NULL;

-- Definir valor padrão
ALTER TABLE conversations ALTER COLUMN conversation_type SET DEFAULT 0;