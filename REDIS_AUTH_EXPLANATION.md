# Autenticação Redis vs JWT - Explicação Completa

## 🔐 **Dois Tipos de Autenticação Diferentes**

### **1. Redis Auth (Infraestrutura)**
```
App Spring Boot ──[PASSWORD]──> Redis Server
                   (senha fixa)
```

### **2. JWT Auth (Usuário)**  
```
Frontend ──[JWT TOKEN]──> API Spring Boot ──[PASSWORD]──> Redis
         (token usuário)                    (senha fixa)
```

## 📋 **Configuração Redis Auth**

### **redis.conf**
```conf
# Senha do Redis (infraestrutura)
requirepass MySuperSecretRedisPassword123!

# Opcional: usuário específico
user rubia_app on >MySuperSecretRedisPassword123! ~rubia:* +@all
```

### **application.properties**
```properties
# Conexão Spring Boot → Redis
spring.data.redis.host=redis-server
spring.data.redis.port=6379
spring.data.redis.password=MySuperSecretRedisPassword123!
spring.data.redis.username=rubia_app  # Opcional

# Configurações de pool
spring.data.redis.jedis.pool.max-active=20
spring.data.redis.jedis.pool.max-idle=10
```

### **Como Spring Boot Conecta**
```java
@Configuration
public class RedisConfig {
    
    @Value("${spring.data.redis.password}")
    private String redisPassword;
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("redis-host", 6379));
        
        // Spring Boot autentica automaticamente com a senha
        return factory;
    }
}
```

## 🔑 **JWT do Usuário (Mesmo do Sistema)**

### **No Frontend (React)**
```javascript
// Login normal do usuário
const response = await fetch('/api/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password })
});

const { token } = await response.json();
localStorage.setItem('authToken', token);

// Usar MESMO token para campanhas
const campaignResponse = await fetch('/api/secure/campaigns/123/start-messaging', {
  headers: {
    'Authorization': `Bearer ${token}`  // Mesmo JWT!
  }
});
```

### **JWT Payload (Exemplo)**
```json
{
  "sub": "user-uuid-123",
  "email": "admin@empresa.com", 
  "company_id": "company-uuid-456",
  "roles": ["CAMPAIGN_MANAGER", "ADMIN"],
  "exp": 1640995200,
  "iat": 1640908800
}
```

### **No Backend (Validação)**
```java
@PostMapping("/{campaignId}/start-messaging")
@PreAuthorize("hasRole('CAMPAIGN_MANAGER')")
public ResponseEntity<?> startCampaignMessaging(
        @PathVariable UUID campaignId,
        Authentication authentication) {  // JWT já validado pelo Spring Security
    
    // Extrair dados do JWT
    JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
    String userId = jwtToken.getToken().getClaimAsString("sub");
    String companyId = jwtToken.getToken().getClaimAsString("company_id");
    List<String> roles = jwtToken.getToken().getClaimAsStringList("roles");
    
    // Validar se usuário pode acessar esta campanha
    if (!campaignBelongsToCompany(campaignId, companyId)) {
        return ResponseEntity.status(403).body("Access denied");
    }
    
    // App usa senha fixa para conectar no Redis
    secureCampaignQueueService.enqueueCampaign(campaignId, companyId, userId);
    
    return ResponseEntity.ok("Campaign started");
}
```

## 🔧 **Implementação Segura Completa**

### **1. Docker Compose Seguro**
```yaml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    command: redis-server /usr/local/etc/redis/redis.conf
    volumes:
      - ./redis.conf:/usr/local/etc/redis/redis.conf:ro
      - redis_data:/data
    networks:
      - internal
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    
  app:
    image: rubia-api:latest
    environment:
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    networks:
      - internal
      - public
    depends_on:
      - redis

networks:
  internal:
    driver: bridge
    internal: true  # Redis não acessível externamente
  public:
    driver: bridge

volumes:
  redis_data:
```

### **2. Configuração de Segurança**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/secure/**").authenticated()  // JWT obrigatório
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");
        
        JwtAuthenticationConverter authenticationConverter = 
            new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        return authenticationConverter;
    }
}
```

### **3. Extração de Dados do JWT**
```java
@Component
public class JwtUtils {
    
    public String extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getToken().getClaimAsString("sub");
        }
        throw new SecurityException("Invalid authentication type");
    }
    
    public String extractCompanyId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getToken().getClaimAsString("company_id");
        }
        throw new SecurityException("Company ID not found in token");
    }
    
    public List<String> extractRoles(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getToken().getClaimAsStringList("roles");
        }
        return List.of();
    }
}
```

## 🚀 **Deploy com Segurança Dupla**

### **Variáveis de Ambiente**
```bash
# Redis (Infraestrutura)
export REDIS_PASSWORD=$(openssl rand -base64 32)

# JWT (Aplicação) - MESMO do sistema existente
export JWT_SECRET="sua-chave-jwt-existente"
export JWT_PUBLIC_KEY="sua-chave-publica-existente"

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

### **Teste de Segurança**
```bash
# 1. Login normal (obtém JWT)
TOKEN=$(curl -X POST /api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@empresa.com","password":"senha"}' \
  | jq -r '.token')

# 2. Usar MESMO token para campanha
curl -H "Authorization: Bearer $TOKEN" \
  /api/secure/campaigns/123/start-messaging

# 3. Verificar que Redis está protegido
redis-cli -h redis-server -p 6379 ping
# (error) NOAUTH Authentication required.

redis-cli -h redis-server -p 6379 -a $REDIS_PASSWORD ping
# PONG (funciona com senha)
```

## 📊 **Fluxo de Autenticação Completo**

```
1. Usuário faz login no frontend
   Frontend ──[email/password]──> /api/auth/login

2. Sistema valida e retorna JWT
   Backend ──[JWT com company_id, roles]──> Frontend

3. Frontend usa JWT para chamadas
   Frontend ──[Authorization: Bearer JWT]──> /api/secure/campaigns/*

4. Spring Security valida JWT
   @PreAuthorize verifica roles do token

5. App acessa Redis com senha fixa
   Spring Boot ──[redis password]──> Redis Server

6. Dados isolados por company_id
   Redis key: "rubia:campaign:queue:company-uuid-456"
```

## 🔐 **Resumo das Proteções**

| **Nível** | **Tipo** | **Proteção** | **Escopo** |
|-----------|----------|--------------|------------|
| **Infraestrutura** | Redis Auth | Senha fixa | Toda a aplicação |
| **Aplicação** | JWT Auth | Token do usuário | Por usuário/empresa |
| **Dados** | Namespace | company_id | Por empresa |
| **Network** | Docker | Internal network | Infraestrutura |
| **API** | Roles | @PreAuthorize | Por endpoint |

## ✅ **Resposta Direta**

**Pergunta:** "É o mesmo JWT de login do sistema?"

**Resposta:** **SIM!** É exatamente o mesmo JWT que o usuário usa para fazer login no sistema. A autenticação do Redis é separada - é uma senha de infraestrutura que o Spring Boot usa automaticamente para conectar.

**Dois níveis:**
1. **JWT do usuário** → Controla quem pode fazer o quê na API
2. **Senha do Redis** → Protege a conexão Spring Boot ↔ Redis

O usuário só precisa do JWT normal dele. A senha do Redis é transparente.