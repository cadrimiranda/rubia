-- Populate conversation_last_message table with existing data
INSERT INTO conversation_last_message (conversation_id, last_message_date, last_message_id, last_message_content, created_at, updated_at)
SELECT 
    c.id as conversation_id,
    COALESCE(latest_message.created_at, c.created_at) as last_message_date,
    latest_message.id as last_message_id,
    latest_message.content as last_message_content,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM conversations c
LEFT JOIN (
    SELECT DISTINCT ON (m.conversation_id) 
        m.id,
        m.conversation_id,
        m.content,
        m.created_at
    FROM messages m
    ORDER BY m.conversation_id, m.created_at DESC
) latest_message ON c.id = latest_message.conversation_id
ON CONFLICT (conversation_id) DO NOTHING;