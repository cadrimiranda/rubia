-- Adicionar novos valores ao enum MessageStatus
ALTER TYPE MessageStatus ADD VALUE 'DRAFT';
ALTER TYPE MessageStatus ADD VALUE 'SENDING';
ALTER TYPE MessageStatus ADD VALUE 'FAILED';