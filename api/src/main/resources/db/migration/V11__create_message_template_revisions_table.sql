CREATE TABLE message_template_revisions (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    revision_number INT NOT NULL,
    content TEXT NOT NULL,
    edited_by_user_id UUID,
    revision_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_template
        FOREIGN KEY(template_id)
        REFERENCES message_templates(id),
    CONSTRAINT fk_edited_by_user
        FOREIGN KEY(edited_by_user_id)
        REFERENCES users(id)
);
