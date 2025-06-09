# Rubia Chat Server - Setup Configuration

## 🚀 Configuração do Backend Multi-Tenant

### 1. **Configuração do Banco de Dados**

Primeiro, crie o banco PostgreSQL:

```sql
-- Conecte no PostgreSQL como superuser
CREATE DATABASE rubia_chat;
CREATE USER rubia_user WITH PASSWORD 'rubia_password';
GRANT ALL PRIVILEGES ON DATABASE rubia_chat TO rubia_user;
```

### 2. **Variáveis de Ambiente**

Copie o arquivo `.env.example` para `.env` e configure as variáveis:

```bash
cp .env.example .env
```

### 3. **Configurações Essenciais**

#### **🔐 Segurança (OBRIGATÓRIO)**

```bash
# IMPORTANTE: Gere uma chave JWT segura para produção
JWT_SECRET=SuaChaveSecuraComMinimo32Caracteres123456789
JWT_EXPIRATION=86400000  # 24 horas
```

#### **🗄️ Banco de Dados**

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/rubia_chat
DATABASE_USERNAME=rubia_user
DATABASE_PASSWORD=rubia_password
```

#### **🏢 Multi-Tenant**

```bash
# Para produção
COMPANY_DOMAIN=rubia.com

# Para desenvolvimento local
COMPANY_DOMAIN=localhost:8080
```

#### **🌐 CORS**

```bash
# Para desenvolvimento
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Para produção
CORS_ALLOWED_ORIGINS=https://*.rubia.com,https://rubia.com
```

### 4. **Configurações por Ambiente**

#### **🔧 Desenvolvimento Local**

```bash
# .env para desenvolvimento
DATABASE_URL=jdbc:postgresql://localhost:5432/rubia_chat_dev
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=devSecretKey123456789012345678901234567890
COMPANY_DOMAIN=localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
MESSAGING_PROVIDER=mock
LOGGING_LEVEL_COM_RUBY=DEBUG
SPRING_PROFILES_ACTIVE=dev
```

#### **🏭 Produção**

```bash
# .env para produção
DATABASE_URL=jdbc:postgresql://prod-host:5432/rubia_chat
DATABASE_USERNAME=rubia_prod_user
DATABASE_PASSWORD=SuaSenhaSeguraDeProd123
JWT_SECRET=SuaChaveJWTSuperSeguraParaProducao123456789
COMPANY_DOMAIN=rubia.com
CORS_ALLOWED_ORIGINS=https://*.rubia.com,https://rubia.com
MESSAGING_PROVIDER=twilio
SECURITY_REQUIRE_SSL=true
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_RUBY=INFO
SPRING_PROFILES_ACTIVE=prod
```

### 5. **Configuração do WhatsApp (Opcional)**

#### **Para Twilio:**

```bash
MESSAGING_PROVIDER=twilio
WHATSAPP_ACCOUNT_ID=seu_twilio_account_sid
WHATSAPP_AUTH_TOKEN=seu_twilio_auth_token
WHATSAPP_PHONE_NUMBER=whatsapp:+5511999999999
WHATSAPP_API_URL=https://api.twilio.com
```

#### **Para WhatsApp Cloud API:**

```bash
MESSAGING_PROVIDER=whatsapp-cloud
WHATSAPP_ACCESS_TOKEN=seu_whatsapp_access_token
WHATSAPP_PHONE_NUMBER_ID=seu_phone_number_id
WHATSAPP_WEBHOOK_VERIFY_TOKEN=seu_webhook_verify_token
```

### 6. **Configuração de Cache e Mensageria (Opcional)**

#### **Redis:**

```bash
# Descomente no application.properties e configure:
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
```

#### **RabbitMQ:**

```bash
# Descomente no application.properties e configure:
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VIRTUAL_HOST=/
```

### 7. **Comandos de Inicialização**

#### **Primeiro Uso:**

```bash
# 1. Configurar variáveis
cp .env.example .env
# Edite o .env com suas configurações

# 2. Compilar e rodar
./mvnw clean compile
./mvnw spring-boot:run
```

#### **Desenvolvimento:**

```bash
# Rodar com reload automático
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
```

#### **Produção:**

```bash
# Build para produção
./mvnw clean package -DskipTests
java -jar target/rubia-server-0.0.1-SNAPSHOT.jar
```

### 8. **Configuração Multi-Tenant**

#### **Como funciona:**

1. **Subdomain Detection**: `company1.rubia.com` → empresa `company1`
2. **JWT with Company**: Token inclui `companyId` e `companySlug`
3. **Database Isolation**: Todas as queries incluem `WHERE company_id = ?`

#### **Criando uma nova empresa:**

```sql
-- Inserir nova empresa
INSERT INTO companies (id, name, slug, is_active, plan_type, max_users) 
VALUES (
  gen_random_uuid(), 
  'Minha Empresa', 
  'minha-empresa', 
  true, 
  'BASIC', 
  10
);

-- Criar usuário admin para a empresa
INSERT INTO users (id, name, email, password_hash, role, company_id, is_online) 
VALUES (
  gen_random_uuid(),
  'Admin User',
  'admin@minha-empresa.com',
  '$2a$10$...',  -- Hash da senha
  'ADMIN',
  (SELECT id FROM companies WHERE slug = 'minha-empresa'),
  false
);
```

### 9. **URLs de Acesso**

#### **Desenvolvimento:**

```bash
# API Backend
http://localhost:8080

# Frontend com empresa
http://localhost:3000?company=company1
http://localhost:3000?company=company2
```

#### **Produção:**

```bash
# Cada empresa tem seu subdomínio
https://company1.rubia.com
https://company2.rubia.com
https://api.rubia.com  # Backend API
```

### 10. **Troubleshooting**

#### **Erro de CORS:**

```bash
# Adicione o domínio frontend no CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://meudominio.com
```

#### **Erro de JWT:**

```bash
# Verifique se a chave tem pelo menos 32 caracteres
JWT_SECRET=MinhaChaveDeNoMinimo32CaracteresParaSerSegura
```

#### **Erro de Database:**

```bash
# Verifique se o PostgreSQL está rodando
sudo systemctl status postgresql

# Teste a conexão
psql -h localhost -U rubia_user -d rubia_chat
```

#### **Erro de Company Detection:**

```bash
# Para desenvolvimento local, use query param
http://localhost:3000?company=minha-empresa

# Para produção, configure DNS apontando subdomínios para o servidor
company1.rubia.com → seu-servidor-ip
company2.rubia.com → seu-servidor-ip
```

## ✅ Checklist de Setup

- [ ] PostgreSQL instalado e rodando
- [ ] Banco `rubia_chat` criado
- [ ] Arquivo `.env` configurado
- [ ] JWT_SECRET com pelo menos 32 caracteres
- [ ] CORS configurado para frontend
- [ ] Primeira empresa criada no banco
- [ ] Usuário admin criado
- [ ] Backend rodando em `http://localhost:8080`
- [ ] Endpoints de saúde funcionando: `http://localhost:8080/actuator/health`

## 🎯 Próximos Passos

1. **Criar empresas no banco**
2. **Configurar DNS para subdomínios** (produção)
3. **Configurar WhatsApp provider** (se necessário)
4. **Configurar Redis/RabbitMQ** (se necessário)
5. **Setup de monitoramento** (logs, métricas)