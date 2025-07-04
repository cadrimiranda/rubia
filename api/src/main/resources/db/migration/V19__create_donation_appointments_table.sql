CREATE TABLE donation_appointments (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    conversation_id UUID,
    external_appointment_id VARCHAR(255) NOT NULL,
    appointment_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    appointment_status DonationAppointmentStatus NOT NULL DEFAULT 'SCHEDULED',
    confirmation_url VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id),
    CONSTRAINT fk_customer
        FOREIGN KEY(customer_id)
        REFERENCES customers(id),
    CONSTRAINT fk_conversation
        FOREIGN KEY(conversation_id)
        REFERENCES conversations(id)
);
CREATE TRIGGER update_donation_appointments_updated_at BEFORE UPDATE ON donation_appointments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
