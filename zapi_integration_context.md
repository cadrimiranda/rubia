# Z-API Integration Context - Rubia Chat

## Resumo das Implementações

Este documento resume todas as implementações Z-API realizadas no projeto Rubia Chat, incluindo integração, mensageria WebSocket, suporte a mídia e ativação via QR code.

## 1. Integração Base Z-API

### Backend - Java Spring Boot

**ZApiAdapter.java** (`/api/src/main/java/com/ruby/rubia_server/core/adapter/impl/ZApiAdapter.java`)
- Implementa `MessagingAdapter` seguindo Strategy Pattern
- Métodos principais:
  - `sendMessage()` - Envio de mensagens de texto
  - `sendMediaMessage()` - Envio de mídias por URL
  - `uploadFile()` - Upload de arquivos
  - `parseIncomingMessage()` - Parse de webhooks Z-API
- Configuração via `application.properties`:
  ```properties
  zapi.instance.url=${ZAPI_INSTANCE_URL}
  zapi.token=${ZAPI_TOKEN}
  zapi.webhook.token=${ZAPI_WEBHOOK_TOKEN}
  ```

**MessagingController.java** - Endpoints Z-API adicionados:
- `POST /api/messaging/webhook/zapi` - Webhook para mensagens recebidas
- `POST /api/messaging/send-image` - Envio de imagens por URL
- `POST /api/messaging/send-document` - Envio de documentos por URL
- `POST /api/messaging/upload-and-send` - Upload e envio direto
- `POST /api/messaging/send-file-base64` - Envio via base64

## 2. WebSocket Real-time Messaging

### Backend

**WebSocketConfig.java** (`/api/src/main/java/com/ruby/rubia_server/config/WebSocketConfig.java`)
- Configuração STOMP com broker `/topic` e `/queue`
- Endpoints: `/ws` para conexão, `/app` para envio
- Interceptor de autenticação JWT

**ChatWebSocketHandler.java** - Gerenciamento de sessões:
- Mapeamento `companyId -> Set<WebSocketSession>`
- Isolamento multi-tenant por empresa

**WebSocketNotificationService.java** - Notificações em tempo real:
- `notifyNewMessage()` - Nova mensagem recebida
- `notifyMessageStatusUpdate()` - Status de mensagem atualizado
- `notifyConversationUpdate()` - Conversa atualizada

### Frontend

**useWebSocket.ts** (`/client/src/hooks/useWebSocket.ts`)
- Hook para conexão WebSocket com STOMP
- Auto-reconexão e tratamento de erros
- Integração com Zustand store

## 3. Suporte a Mídia Z-API

### Backend - Novos Métodos ZApiAdapter

```java
// Envio de mídia por URL
public MessageResult sendImageByUrl(String to, String imageUrl, String caption)
public MessageResult sendDocumentByUrl(String to, String documentUrl, String fileName, String caption)
public MessageResult sendVideoByUrl(String to, String videoUrl, String caption)
public MessageResult sendAudioByUrl(String to, String audioUrl)

// Upload e envio direto
public MessageResult uploadAndSendFile(String to, MultipartFile file, String caption)

// Envio via base64
public MessageResult sendFileBase64(String to, String base64Data, String fileName, String caption)
```

**MediaValidator.java** - Validação de arquivos:
- Tipos permitidos: imagem, vídeo, áudio, documento
- Limites de tamanho por tipo
- Validação de MIME types

### Frontend - APIs Integradas

**mediaApi.ts** - Métodos Z-API adicionados:
```typescript
uploadForZApi(file: File, to: string, caption?: string)
sendImageUrl(to: string, imageUrl: string, caption?: string)
sendDocumentUrl(to: string, documentUrl: string, caption?: string)
sendFileBase64(to: string, file: File, caption?: string)
```

**BloodCenterChat.tsx** - Integração completa:
- Upload de mídia via Z-API
- Preview de arquivos antes do envio
- Validação no frontend
- Tratamento de erros e feedback visual

## 4. Ativação QR Code Z-API

### Backend - Entidades

**ZApiStatus.java** (`/api/src/main/java/com/ruby/rubia_server/core/entity/ZApiStatus.java`)
```java
@Data @Builder
public class ZApiStatus {
    private boolean connected;
    private String session;
    private boolean smartphoneConnected;
    private boolean needsQrCode;
    private String error;
    private Map<String, Object> rawResponse;
}
```

**QrCodeResult.java** e **PhoneCodeResult.java** - Results para ativação

**ZApiActivationService.java** - Service completo:
```java
public ZApiStatus getInstanceStatus()
public QrCodeResult getQrCodeBytes()
public QrCodeResult getQrCodeImage()
public PhoneCodeResult getPhoneCode(String phoneNumber)
public boolean restartInstance()
public boolean disconnectInstance()
```

**ZApiActivationController.java** - Endpoints REST:
- `GET /api/zapi/activation/status` - Status da instância
- `GET /api/zapi/activation/qr-code/image` - QR code base64
- `GET /api/zapi/activation/qr-code/bytes` - QR code PNG
- `GET /api/zapi/activation/phone-code/{phone}` - Código de telefone
- `POST /api/zapi/activation/restart` - Reiniciar instância
- `POST /api/zapi/activation/disconnect` - Desconectar
- `POST /api/zapi/activation/webhook/connected` - Webhook conectado
- `POST /api/zapi/activation/webhook/disconnected` - Webhook desconectado

### Frontend - Componente de Ativação

**ZApiActivation.tsx** (`/client/src/components/ZApiActivation.tsx`)
- Interface completa com Ant Design
- Dois métodos de ativação: QR code e código de telefone
- Polling automático de status (5s)
- Operações de restart/disconnect
- Feedback visual em tempo real

**Integração em App.tsx:**
```typescript
<Route path="/zapi-activation" element={
  <ProtectedRoute requiredRole="ADMIN">
    <ZApiActivation />
  </ProtectedRoute>
} />
```

## 5. Configurações e Propriedades

### application.properties
```properties
# Z-API Core
zapi.instance.url=${ZAPI_INSTANCE_URL:https://api.z-api.io/instances/SUA_INSTANCIA}
zapi.token=${ZAPI_TOKEN:seu_token_aqui}
zapi.webhook.token=${ZAPI_WEBHOOK_TOKEN:}

# Z-API Media
zapi.media.max-file-size=${ZAPI_MEDIA_MAX_FILE_SIZE:20971520}
zapi.media.allowed-types=${ZAPI_MEDIA_ALLOWED_TYPES:image/jpeg,image/png,application/pdf,video/mp4,audio/mpeg}

# File Upload
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB

# Logging Z-API
logging.level.com.ruby.rubia_server.core.adapter.impl.ZApiAdapter=DEBUG
logging.level.com.ruby.rubia_server.core.service.MessagingService=DEBUG
```

## 6. Fluxo de Mensagens Implementado

### Envio (Frontend → Backend → Z-API)
1. Usuario seleciona arquivo em `BloodCenterChat.tsx`
2. `mediaApi.uploadForZApi()` valida e envia para `/api/messaging/upload-and-send`
3. `MessagingController` processa via `ZApiAdapter.uploadAndSendFile()`
4. Z-API recebe arquivo e envia mensagem
5. WebSocket notifica frontend em tempo real

### Recebimento (Z-API → Backend → Frontend)
1. Z-API envia webhook para `/api/messaging/webhook/zapi`
2. `ZApiAdapter.parseIncomingMessage()` processa payload
3. `MessagingService.processIncomingMessage()` salva no banco
4. `WebSocketNotificationService` notifica frontend
5. Frontend atualiza UI via `useWebSocket.ts`

## 7. Regras de Negócio Implementadas

### Validação de Mídia
- **Imagens**: max 5MB (JPEG, PNG, GIF, WebP)
- **Vídeos**: max 50MB (MP4, WebM, QuickTime)
- **Áudios**: max 10MB (MP3, WAV, OGG, M4A)
- **Documentos**: max 25MB (PDF, DOC, XLS, PPT, TXT)

### Formatação de Telefone
- Números brasileiros: `55DDNNNNNNNNN`
- Remove caracteres especiais
- Valida formato antes do envio

### Multi-tenant WebSocket
- Sessões isoladas por `companyId`
- Notificações enviadas apenas para empresa correta
- Autenticação JWT obrigatória

### Tratamento de Erros
- Retry automático em falhas de envio
- Logs detalhados para debugging
- Fallback para endpoints alternativos
- Validação tanto no frontend quanto backend

## 8. Status da Implementação

### ✅ Completo
- [x] Integração base Z-API
- [x] WebSocket real-time
- [x] Suporte completo a mídia
- [x] Ativação via QR code
- [x] Validação de arquivos
- [x] Multi-tenant isolation
- [x] Tratamento de erros
- [x] Interface administrativa

### 🔧 Melhorias Futuras
- [ ] Monitoramento automático de conexão
- [ ] Dashboard de métricas Z-API
- [ ] Configuração dinâmica de instâncias
- [ ] Backup de mensagens offline
- [ ] Rate limiting para Z-API calls

## 9. Arquivos Principais Modificados/Criados

### Backend
```
/api/src/main/java/com/ruby/rubia_server/
├── core/adapter/impl/ZApiAdapter.java (modificado)
├── core/controller/MessagingController.java (modificado)
├── core/controller/ZApiActivationController.java (novo)
├── core/service/MessagingService.java (modificado)
├── core/service/ZApiActivationService.java (novo)
├── core/service/WebSocketNotificationService.java (novo)
├── core/entity/ZApiStatus.java (novo)
├── core/entity/QrCodeResult.java (novo)
├── core/entity/PhoneCodeResult.java (novo)
├── core/entity/IncomingMessage.java (modificado)
├── core/validator/MediaValidator.java (novo)
├── config/WebSocketConfig.java (novo)
├── config/ChatWebSocketHandler.java (novo)
└── config/WebSocketAuthInterceptor.java (novo)
```

### Frontend
```
/client/src/
├── components/BloodCenterChat.tsx (modificado)
├── components/ZApiActivation.tsx (novo)
├── hooks/useWebSocket.ts (novo)
├── api/services/mediaApi.ts (modificado)
├── api/services/messageApi.ts (modificado)
└── App.tsx (modificado)
```

Este contexto resume toda a implementação Z-API realizada, fornecendo uma base sólida para próximas implementações e manutenção do sistema.