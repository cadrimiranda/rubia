ALTER TABLE user_ai_agents 
ADD COLUMN company_id UUID NOT NULL;

ALTER TABLE user_ai_agents 
ADD CONSTRAINT fk_user_ai_agents_company 
FOREIGN KEY (company_id) REFERENCES companies(id);