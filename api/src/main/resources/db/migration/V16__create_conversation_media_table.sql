CREATE TABLE conversation_media (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    conversation_id UUID NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    media_type MediaType NOT NULL,
    mime_type VARCHAR(100),
    original_file_name VARCHAR(255),
    file_size_bytes BIGINT,
    checksum VARCHAR(64),
    uploaded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by_user_id UUID,
    uploaded_by_customer_id UUID,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id),
    CONSTRAINT fk_conversation
        FOREIGN KEY(conversation_id)
        REFERENCES conversations(id),
    CONSTRAINT fk_uploaded_by_user
        FOREIGN KEY(uploaded_by_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_uploaded_by_customer
        FOREIGN KEY(uploaded_by_customer_id)
        REFERENCES customers(id)
);
