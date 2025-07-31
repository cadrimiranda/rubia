# Guia de Integração Z-API - Rubia Server

Este guia fornece instruções detalhadas para integrar a Z-API ao sistema Rubia Server, mantendo a arquitetura existente baseada no padrão Strategy com adapters.

## Estrutura do Projeto

```
api/src/main/java/com/ruby/rubia_server/core/
├── adapter/
│   ├── MessagingAdapter.java
│   └── impl/
│       ├── TwilioAdapter.java (existente)
│       └── ZApiAdapter.java (novo)
├── controller/
│   └── MessagingController.java (modificar)
├── service/
│   └── MessagingService.java (modificar)
└── entity/
    ├── MessageResult.java (existente)
    └── IncomingMessage.java (existente)
```

## Passo 1: Criar Interface Base (se não existir)

### MessagingAdapter.java
```java
// api/src/main/java/com/ruby/rubia_server/core/adapter/MessagingAdapter.java
package com.ruby.rubia_server.core.adapter;

import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;

public interface MessagingAdapter {
    MessageResult sendMessage(String to, String message);
    MessageResult sendMediaMessage(String to, String mediaUrl, String caption);
    IncomingMessage parseIncomingMessage(Object webhookPayload);
    boolean validateWebhook(Object payload, String signature);
    String getProviderName();
}
```

## Passo 2: Implementar ZApiAdapter

### ZApiAdapter.java
```java
// api/src/main/java/com/ruby/rubia_server/core/adapter/impl/ZApiAdapter.java
package com.ruby.rubia_server.core.adapter.impl;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Component
@Slf4j
public class ZApiAdapter implements MessagingAdapter {

    @Value("${zapi.instance.url}")
    private String instanceUrl;

    @Value("${zapi.token}")
    private String token;

    @Value("${zapi.webhook.token:}")
    private String webhookToken;

    private final RestTemplate restTemplate;

    public ZApiAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public MessageResult sendMessage(String to, String message) {
        try {
            log.info("Sending Z-API message to: {}", to);

            String url = instanceUrl + "/send-text";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", formatPhoneNumber(to));
            requestBody.put("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = (String) responseBody.get("messageId");
                
                log.info("Z-API message sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send message via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending message via Z-API: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    @Override
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
        try {
            log.info("Sending Z-API media message to: {}", to);

            String url = instanceUrl + "/send-file-url";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", formatPhoneNumber(to));
            requestBody.put("url", mediaUrl);
            if (caption != null && !caption.trim().isEmpty()) {
                requestBody.put("caption", caption);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = (String) responseBody.get("messageId");
                
                log.info("Z-API media message sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send media message via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending media message via Z-API: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    @Override
    public IncomingMessage parseIncomingMessage(Object webhookPayload) {
        try {
            log.info("Parsing Z-API incoming message");

            if (!(webhookPayload instanceof Map)) {
                throw new IllegalArgumentException("Invalid payload format");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) webhookPayload;

            String messageId = (String) payload.get("messageId");
            String phone = (String) payload.get("phone");
            String fromMe = (String) payload.get("fromMe");
            
            // Z-API pode ter diferentes estruturas dependendo do tipo de webhook
            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            String messageBody = null;
            String mediaUrl = null;
            String mediaType = null;

            if (message != null) {
                messageBody = (String) message.get("conversation");
                
                // Para mensagens de mídia
                Map<String, Object> imageMessage = (Map<String, Object>) message.get("imageMessage");
                Map<String, Object> videoMessage = (Map<String, Object>) message.get("videoMessage");
                Map<String, Object> documentMessage = (Map<String, Object>) message.get("documentMessage");

                if (imageMessage != null) {
                    mediaUrl = (String) imageMessage.get("url");
                    mediaType = "image";
                    messageBody = (String) imageMessage.get("caption");
                } else if (videoMessage != null) {
                    mediaUrl = (String) videoMessage.get("url");
                    mediaType = "video";
                    messageBody = (String) videoMessage.get("caption");
                } else if (documentMessage != null) {
                    mediaUrl = (String) documentMessage.get("url");
                    mediaType = "document";
                    messageBody = (String) documentMessage.get("caption");
                }
            }

            // Parse timestamp
            Long timestamp = (Long) payload.get("timestamp");
            LocalDateTime messageTime = LocalDateTime.now();

            return IncomingMessage.builder()
                .messageId(messageId)
                .from(phone)
                .to(null) // Z-API geralmente não fornece o "to" em webhooks
                .body(messageBody)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .timestamp(messageTime)
                .provider("z-api")
                .rawPayload(payload)
                .build();

        } catch (Exception e) {
            log.error("Error parsing Z-API incoming message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse Z-API incoming message", e);
        }
    }

    @Override
    public boolean validateWebhook(Object payload, String signature) {
        // Z-API pode usar diferentes métodos de validação
        // Implementar conforme documentação da Z-API
        if (webhookToken != null && !webhookToken.isEmpty()) {
            // Verificar se o token está presente na requisição
            return true; // Implementar validação real conforme necessário
        }
        return true; // Se não há token configurado, aceitar
    }

    @Override
    public String getProviderName() {
        return "z-api";
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        // Remove caracteres não numéricos
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        
        // Z-API geralmente espera números no formato brasileiro sem +
        if (digitsOnly.startsWith("55")) {
            return digitsOnly;
        } else if (digitsOnly.startsWith("+55")) {
            return digitsOnly.substring(1);
        } else {
            // Assumir que é um número brasileiro
            return "55" + digitsOnly;
        }
    }
}
```

## Passo 3: Configurar Propriedades

### application.yml
```yaml
# api/src/main/resources/application.yml
zapi:
  instance:
    url: ${ZAPI_INSTANCE_URL:https://api.z-api.io/instances/SUA_INSTANCIA}
  token: ${ZAPI_TOKEN:seu_token_aqui}
  webhook:
    token: ${ZAPI_WEBHOOK_TOKEN:} # Token opcional para validação de webhook

# Configurar logs para debug
logging:
  level:
    com.ruby.rubia_server.core.adapter.impl.ZApiAdapter: DEBUG
    com.ruby.rubia_server.core.service.MessagingService: DEBUG
```

### application.properties (alternativa)
```properties
# api/src/main/resources/application.properties
zapi.instance.url=${ZAPI_INSTANCE_URL:https://api.z-api.io/instances/SUA_INSTANCIA}
zapi.token=${ZAPI_TOKEN:seu_token_aqui}
zapi.webhook.token=${ZAPI_WEBHOOK_TOKEN:}

# Logs
logging.level.com.ruby.rubia_server.core.adapter.impl.ZApiAdapter=DEBUG
logging.level.com.ruby.rubia_server.core.service.MessagingService=DEBUG
```

## Passo 4: Atualizar MessagingController

### Adicionar endpoint para webhook Z-API
```java
// Adicione este método no MessagingController existente
@PostMapping("/webhook/zapi")
public ResponseEntity<String> handleZApiWebhook(
        @RequestBody Map<String, Object> payload,
        @RequestHeader(value = "Authorization", required = false) String authorization) {
    
    try {
        log.info("Received Z-API webhook: {}", payload);
        
        // Verificar se é uma mensagem válida
        String fromMe = (String) payload.get("fromMe");
        if ("true".equals(fromMe)) {
            // Ignorar mensagens enviadas por nós mesmos
            return ResponseEntity.ok("OK");
        }

        // Validar webhook se necessário
        if (!messagingService.validateWebhook(payload, authorization)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        IncomingMessage message = messagingService.parseIncomingMessage(payload);
        messagingService.processIncomingMessage(message);
        
        return ResponseEntity.ok("OK");
        
    } catch (Exception e) {
        log.error("Error processing Z-API webhook: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
}
```

## Passo 5: Atualizar MessagingService

### Adicionar suporte para Z-API no processamento de mensagens
```java
// Adicione estes métodos no MessagingService existente

public Company findCompanyByZApiInstance(String instanceId) {
    // Implementar lógica para encontrar empresa baseada na instância Z-API
    // Por enquanto, retornar a primeira empresa ativa
    return companyRepository.findAll().stream()
        .filter(Company::getIsActive)
        .findFirst()
        .orElse(null);
}

// Modifique o método processIncomingMessage existente
public void processIncomingMessage(IncomingMessage incomingMessage) {
    try {
        log.info("Processing incoming message from: {} via {}", 
            incomingMessage.getFrom(), incomingMessage.getProvider());
        
        String fromNumber = extractPhoneNumber(incomingMessage.getFrom());
        
        Company company;
        if ("z-api".equals(incomingMessage.getProvider())) {
            // Para Z-API, determinar empresa pela instância ou configuração
            company = findCompanyByZApiInstance("default");
        } else {
            // Lógica existente para Twilio
            String toNumber = extractPhoneNumber(incomingMessage.getTo());
            company = findCompanyByWhatsAppNumber(toNumber);
        }
        
        if (company == null) {
            log.warn("No company found for incoming message from: {}", fromNumber);
            return;
        }
        
        // Resto da lógica permanece igual...
        Customer customer;
        try {
            CustomerDTO customerDTO = customerService.findByPhoneAndCompany(fromNumber, company.getId());
            customer = Customer.builder()
                .id(customerDTO.getId())
                .phone(customerDTO.getPhone())
                .name(customerDTO.getName())
                .company(company)
                .build();
        } catch (IllegalArgumentException e) {
            customer = createCustomerFromWhatsApp(fromNumber, company);
        }
        
        ConversationDTO conversation = findOrCreateConversation(customer);
        messageService.createFromIncomingMessage(incomingMessage, conversation.getId());
        
        log.info("Successfully processed incoming message for conversation: {}", 
            conversation.getId());
        
    } catch (Exception e) {
        log.error("Error processing incoming message: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to process incoming message", e);
    }
}
```

## Passo 6: Configurar Variáveis de Ambiente

### Arquivo .env (para desenvolvimento)
```bash
# .env
ZAPI_INSTANCE_URL=https://api.z-api.io/instances/SUA_INSTANCIA_AQUI
ZAPI_TOKEN=seu_token_da_zapi_aqui
ZAPI_WEBHOOK_TOKEN=token_opcional_para_validacao
```

### Configuração no sistema
```bash
# Linux/Mac
export ZAPI_INSTANCE_URL="https://api.z-api.io/instances/SUA_INSTANCIA"
export ZAPI_TOKEN="seu_token_aqui"
export ZAPI_WEBHOOK_TOKEN="seu_webhook_token"

# Windows
set ZAPI_INSTANCE_URL=https://api.z-api.io/instances/SUA_INSTANCIA
set ZAPI_TOKEN=seu_token_aqui
set ZAPI_WEBHOOK_TOKEN=seu_webhook_token
```

## Passo 7: Configurar Webhook na Z-API

### URL do webhook para configurar
```
https://seu-dominio.com/api/messaging/webhook/zapi
```

### Configurar via API da Z-API
```bash
curl -X POST "https://api.z-api.io/instances/SUA_INSTANCIA/webhook" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "webhook": "https://seu-dominio.com/api/messaging/webhook/zapi",
    "events": ["message", "status"]
  }'
```

## Passo 8: Comandos para Teste

### Teste de envio de mensagem
```bash
curl -X POST "http://localhost:8080/api/messaging/send" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "5511999999999",
    "message": "Teste via Z-API"
  }'
```

### Trocar para provider Z-API
```bash
curl -X POST "http://localhost:8080/api/messaging/switch-provider" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "z-api"
  }'
```

### Verificar status atual
```bash
curl -X GET "http://localhost:8080/api/messaging/status"
```

### Teste de envio direto via Z-API
```bash
curl -X POST "https://api.z-api.io/instances/SUA_INSTANCIA/send-text" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "5511999999999",
    "message": "Teste direto Z-API"
  }'
```

## Passo 9: Scripts de Automação

### Script para trocar provider (switch-to-zapi.sh)
```bash
#!/bin/bash
# switch-to-zapi.sh

API_BASE_URL=${API_BASE_URL:-"http://localhost:8080"}

echo "Switching to Z-API provider..."

response=$(curl -s -X POST "${API_BASE_URL}/api/messaging/switch-provider" \
  -H "Content-Type: application/json" \
  -d '{"provider": "z-api"}')

echo "Response: $response"

# Verificar status
status=$(curl -s -X GET "${API_BASE_URL}/api/messaging/status")
echo "Current status: $status"
```

### Script para envio de mensagem (send-message.sh)
```bash
#!/bin/bash
# send-message.sh

API_BASE_URL=${API_BASE_URL:-"http://localhost:8080"}
TO="$1"
MESSAGE="$2"

if [ -z "$TO" ] || [ -z "$MESSAGE" ]; then
    echo "Usage: $0 <phone_number> <message>"
    echo "Example: $0 5511999999999 'Hello World'"
    exit 1
fi

echo "Sending message to: $TO"
echo "Message: $MESSAGE"

curl -X POST "${API_BASE_URL}/api/messaging/send" \
  -H "Content-Type: application/json" \
  -d "{
    \"to\": \"${TO}\",
    \"message\": \"${MESSAGE}\"
  }"
```

### Script para verificar logs (check-logs.sh)
```bash
#!/bin/bash
# check-logs.sh

# Verificar logs da aplicação
echo "=== Z-API Adapter Logs ==="
grep -i "z-api" /var/log/rubia-server/application.log | tail -20

echo ""
echo "=== Messaging Service Logs ==="
grep -i "messaging" /var/log/rubia-server/application.log | tail -20

echo ""
echo "=== Error Logs ==="
grep -i "error" /var/log/rubia-server/application.log | grep -i "z-api" | tail -10
```

## Passo 10: Troubleshooting

### Problemas Comuns

#### 1. Token inválido
```bash
# Verificar se o token está correto
curl -X GET "https://api.z-api.io/instances/SUA_INSTANCIA/status" \
  -H "Authorization: Bearer SEU_TOKEN"
```

#### 2. Webhook não funciona
```bash
# Verificar se o webhook está configurado
curl -X GET "https://api.z-api.io/instances/SUA_INSTANCIA/webhook" \
  -H "Authorization: Bearer SEU_TOKEN"
```

#### 3. Mensagem não enviada
- Verificar formato do número de telefone
- Verificar se a instância Z-API está conectada
- Verificar logs da aplicação

#### 4. Logs para debug
```bash
# Habilitar logs detalhados
export LOGGING_LEVEL_COM_RUBY_RUBIA_SERVER_CORE_ADAPTER_IMPL_ZAPIADAPTER=DEBUG
export LOGGING_LEVEL_COM_RUBY_RUBIA_SERVER_CORE_SERVICE_MESSAGINGSERVICE=DEBUG
```

### Comandos úteis para debug
```bash
# Ver status da aplicação
curl -s "http://localhost:8080/actuator/health" | jq '.'

# Ver métricas (se configurado)
curl -s "http://localhost:8080/actuator/metrics" | jq '.'

# Testar conectividade com Z-API
curl -s "https://api.z-api.io/instances/SUA_INSTANCIA/status" \
  -H "Authorization: Bearer SEU_TOKEN" | jq '.'
```

## Estrutura Final

Após implementar todos os passos, sua estrutura deve estar assim:

```
api/src/main/java/com/ruby/rubia_server/core/
├── adapter/
│   ├── MessagingAdapter.java ✓
│   └── impl/
│       ├── TwilioAdapter.java ✓
│       └── ZApiAdapter.java ✓ (novo)
├── controller/
│   └── MessagingController.java ✓ (modificado)
├── service/
│   └── MessagingService.java ✓ (modificado)
├── entity/
│   ├── MessageResult.java ✓
│   └── IncomingMessage.java ✓
└── resources/
    └── application.yml ✓ (modificado)
```

## Próximos Passos

1. **Implementar**: Copie os códigos fornecidos
2. **Configurar**: Defina as variáveis de ambiente
3. **Testar**: Use os comandos de teste fornecidos
4. **Monitorar**: Configure logs e monitoring
5. **Deploy**: Atualize sua aplicação em produção

---

**Nota**: Substitua `SUA_INSTANCIA` e `SEU_TOKEN` pelos valores reais da sua conta Z-API antes de usar os comandos.