-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    department_id UUID REFERENCES departments(id),
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'SUPERVISOR', 'AGENT')),
    avatar_url TEXT,
    is_online BOOLEAN DEFAULT false,
    last_seen TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create trigger for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_department_id ON users(department_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_online ON users(is_online);
CREATE INDEX idx_users_department_online ON users(department_id, is_online);

-- Insert default admin user
INSERT INTO users (name, email, password_hash, role, is_online) VALUES
    ('Admin', 'admin@rubia.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', false);
-- Password is 'password' hashed with BCrypt