# Z-API - Conversação com Imagem e Documento

Guia prático para implementar envio e recebimento de mídia (imagens, documentos, vídeos) usando Z-API no sistema Rubia Server.

## Endpoints Z-API para Mídia

### Base URL
```
https://api.z-api.io/instances/{instance_id}
```

### Autenticação
```
Authorization: Bearer {token}
Content-Type: application/json
```

## Envio de Mídia

### 1. Enviar Imagem por URL
```bash
curl -X POST "https://api.z-api.io/instances/{instance}/send-file-url" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "5511999999999",
    "url": "https://example.com/image.jpg",
    "caption": "Legenda da imagem"
  }'
```

### 2. Enviar Documento por URL
```bash
curl -X POST "https://api.z-api.io/instances/{instance}/send-file-url" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "5511999999999",
    "url": "https://example.com/documento.pdf",
    "caption": "Documento importante",
    "fileName": "relatorio.pdf"
  }'
```

### 3. Enviar Arquivo Base64
```bash
curl -X POST "https://api.z-api.io/instances/{instance}/send-file-base64" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "5511999999999",
    "base64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQ...",
    "fileName": "imagem.jpg",
    "caption": "Imagem enviada"
  }'
```

### 4. Upload e Envio de Arquivo
```bash
# Primeiro fazer upload
curl -X POST "https://api.z-api.io/instances/{instance}/upload-file" \
  -H "Authorization: Bearer {token}" \
  -F "file=@/path/to/file.jpg"

# Resposta do upload
{
  "url": "https://api.z-api.io/instances/{instance}/download/{file_id}",
  "fileName": "file.jpg",
  "fileId": "abc123"
}

# Depois enviar usando a URL
curl -X POST "https://api.z-api.io/instances/{instance}/send-file-url" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "5511999999999",
    "url": "https://api.z-api.io/instances/{instance}/download/{file_id}",
    "caption": "Arquivo enviado"
  }'
```

## Implementação no ZApiAdapter

### Métodos Adicionais para ZApiAdapter.java
```java
public MessageResult sendImageByUrl(String to, String imageUrl, String caption) {
    return sendMediaByUrl(to, imageUrl, caption, "image");
}

public MessageResult sendDocumentByUrl(String to, String documentUrl, String caption, String fileName) {
    try {
        String url = instanceUrl + "/send-file-url";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", formatPhoneNumber(to));
        requestBody.put("url", documentUrl);
        if (caption != null && !caption.trim().isEmpty()) {
            requestBody.put("caption", caption);
        }
        if (fileName != null && !fileName.trim().isEmpty()) {
            requestBody.put("fileName", fileName);
        }

        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String messageId = (String) response.getBody().get("messageId");
            return MessageResult.success(messageId, "sent", "z-api");
        } else {
            return MessageResult.error("Failed to send document via Z-API", "z-api");
        }

    } catch (Exception e) {
        return MessageResult.error("Error sending document: " + e.getMessage(), "z-api");
    }
}

public MessageResult sendFileBase64(String to, String base64Data, String fileName, String caption) {
    try {
        String url = instanceUrl + "/send-file-base64";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", formatPhoneNumber(to));
        requestBody.put("base64", base64Data);
        requestBody.put("fileName", fileName);
        if (caption != null && !caption.trim().isEmpty()) {
            requestBody.put("caption", caption);
        }

        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String messageId = (String) response.getBody().get("messageId");
            return MessageResult.success(messageId, "sent", "z-api");
        } else {
            return MessageResult.error("Failed to send file via Z-API", "z-api");
        }

    } catch (Exception e) {
        return MessageResult.error("Error sending file: " + e.getMessage(), "z-api");
    }
}

public String uploadFile(MultipartFile file) {
    try {
        String url = instanceUrl + "/upload-file";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("url");
        } else {
            throw new RuntimeException("Failed to upload file to Z-API");
        }

    } catch (Exception e) {
        throw new RuntimeException("Error uploading file: " + e.getMessage(), e);
    }
}

private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);
    return headers;
}

private MessageResult sendMediaByUrl(String to, String mediaUrl, String caption, String mediaType) {
    try {
        String url = instanceUrl + "/send-file-url";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", formatPhoneNumber(to));
        requestBody.put("url", mediaUrl);
        if (caption != null && !caption.trim().isEmpty()) {
            requestBody.put("caption", caption);
        }

        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String messageId = (String) response.getBody().get("messageId");
            return MessageResult.success(messageId, "sent", "z-api");
        } else {
            return MessageResult.error("Failed to send " + mediaType + " via Z-API", "z-api");
        }

    } catch (Exception e) {
        return MessageResult.error("Error sending " + mediaType + ": " + e.getMessage(), "z-api");
    }
}
```

## Recebimento de Mídia via Webhook

### Estrutura do Webhook para Imagem
```json
{
  "messageId": "3EB0796DC6B777...",
  "phone": "5511999999999",
  "fromMe": false,
  "momment": 1640995200,
  "status": "RECEIVED",
  "chatName": "Nome do Contato",
  "senderPhoto": "https://...",
  "senderName": "Nome do Contato",
  "participantPhone": null,
  "photo": "https://...",
  "broadcast": false,
  "type": "ReceivedCallback",
  "instanceId": "instance123",
  "message": {
    "imageMessage": {
      "url": "https://api.z-api.io/instances/{instance}/download/{file_id}",
      "mimeType": "image/jpeg",
      "caption": "Legenda da imagem",
      "jpegThumbnail": "/9j/4AAQSkZJRgABAQAAAQ..."
    }
  }
}
```

### Estrutura do Webhook para Documento
```json
{
  "messageId": "3EB0796DC6B777...",
  "phone": "5511999999999",
  "fromMe": false,
  "momment": 1640995200,
  "status": "RECEIVED",
  "chatName": "Nome do Contato",
  "senderPhoto": "https://...",
  "senderName": "Nome do Contato",
  "participantPhone": null,
  "photo": "https://...",
  "broadcast": false,
  "type": "ReceivedCallback",
  "instanceId": "instance123",
  "message": {
    "documentMessage": {
      "url": "https://api.z-api.io/instances/{instance}/download/{file_id}",
      "mimeType": "application/pdf",
      "title": "documento.pdf",
      "fileName": "relatorio_vendas.pdf",
      "caption": "Relatório mensal",
      "pageCount": 5,
      "fileLength": 204800
    }
  }
}
```

### Estrutura do Webhook para Vídeo
```json
{
  "messageId": "3EB0796DC6B777...",
  "phone": "5511999999999",
  "fromMe": false,
  "momment": 1640995200,
  "status": "RECEIVED",
  "chatName": "Nome do Contato",
  "senderPhoto": "https://...",
  "senderName": "Nome do Contato",
  "participantPhone": null,
  "photo": "https://...",
  "broadcast": false,
  "type": "ReceivedCallback",
  "instanceId": "instance123",
  "message": {
    "videoMessage": {
      "url": "https://api.z-api.io/instances/{instance}/download/{file_id}",
      "mimeType": "video/mp4",
      "caption": "Vídeo demonstrativo",
      "seconds": 30,
      "jpegThumbnail": "/9j/4AAQSkZJRgABAQAAAQ..."
    }
  }
}
```

## Processamento Melhorado no parseIncomingMessage

### Atualização do método parseIncomingMessage
```java
@Override
public IncomingMessage parseIncomingMessage(Object webhookPayload) {
    try {
        if (!(webhookPayload instanceof Map)) {
            throw new IllegalArgumentException("Invalid payload format");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) webhookPayload;

        String messageId = (String) payload.get("messageId");
        String phone = (String) payload.get("phone");
        String fromMe = (String) payload.get("fromMe");
        
        if ("true".equals(fromMe)) {
            return null; // Ignorar mensagens enviadas por nós
        }

        Map<String, Object> message = (Map<String, Object>) payload.get("message");
        String messageBody = null;
        String mediaUrl = null;
        String mediaType = null;
        String fileName = null;
        String mimeType = null;

        if (message != null) {
            // Mensagem de texto
            messageBody = (String) message.get("conversation");
            
            // Mensagem de imagem
            Map<String, Object> imageMessage = (Map<String, Object>) message.get("imageMessage");
            if (imageMessage != null) {
                mediaUrl = (String) imageMessage.get("url");
                mediaType = "image";
                mimeType = (String) imageMessage.get("mimeType");
                messageBody = (String) imageMessage.get("caption");
            }
            
            // Mensagem de vídeo
            Map<String, Object> videoMessage = (Map<String, Object>) message.get("videoMessage");
            if (videoMessage != null) {
                mediaUrl = (String) videoMessage.get("url");
                mediaType = "video";
                mimeType = (String) videoMessage.get("mimeType");
                messageBody = (String) videoMessage.get("caption");
            }
            
            // Mensagem de documento
            Map<String, Object> documentMessage = (Map<String, Object>) message.get("documentMessage");
            if (documentMessage != null) {
                mediaUrl = (String) documentMessage.get("url");
                mediaType = "document";
                mimeType = (String) documentMessage.get("mimeType");
                fileName = (String) documentMessage.get("fileName");
                messageBody = (String) documentMessage.get("caption");
            }
            
            // Mensagem de áudio
            Map<String, Object> audioMessage = (Map<String, Object>) message.get("audioMessage");
            if (audioMessage != null) {
                mediaUrl = (String) audioMessage.get("url");
                mediaType = "audio";
                mimeType = (String) audioMessage.get("mimeType");
            }
        }

        Long timestamp = (Long) payload.get("momment");
        LocalDateTime messageTime = timestamp != null ? 
            LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC) : 
            LocalDateTime.now();

        return IncomingMessage.builder()
            .messageId(messageId)
            .from(phone)
            .to(null)
            .body(messageBody)
            .mediaUrl(mediaUrl)
            .mediaType(mediaType)
            .fileName(fileName)
            .mimeType(mimeType)
            .timestamp(messageTime)
            .provider("z-api")
            .rawPayload(payload)
            .build();

    } catch (Exception e) {
        log.error("Error parsing Z-API incoming message: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to parse Z-API incoming message", e);
    }
}
```

## Controller para Upload e Envio

### Endpoints no MessagingController
```java
@PostMapping("/send-image")
public ResponseEntity<MessageResult> sendImage(
        @RequestParam String to,
        @RequestParam String imageUrl,
        @RequestParam(required = false) String caption) {
    
    MessageResult result = messagingService.sendImageByUrl(to, imageUrl, caption);
    return ResponseEntity.ok(result);
}

@PostMapping("/send-document")
public ResponseEntity<MessageResult> sendDocument(
        @RequestParam String to,
        @RequestParam String documentUrl,
        @RequestParam(required = false) String caption,
        @RequestParam(required = false) String fileName) {
    
    MessageResult result = messagingService.sendDocumentByUrl(to, documentUrl, caption, fileName);
    return ResponseEntity.ok(result);
}

@PostMapping("/upload-and-send")
public ResponseEntity<MessageResult> uploadAndSend(
        @RequestParam String to,
        @RequestParam MultipartFile file,
        @RequestParam(required = false) String caption) {
    
    try {
        String fileUrl = messagingService.uploadFile(file);
        MessageResult result = messagingService.sendMediaByUrl(to, fileUrl, caption);
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        MessageResult error = MessageResult.error("Upload failed: " + e.getMessage(), "z-api");
        return ResponseEntity.badRequest().body(error);
    }
}

@PostMapping("/send-file-base64")
public ResponseEntity<MessageResult> sendFileBase64(
        @RequestBody Map<String, String> request) {
    
    String to = request.get("to");
    String base64Data = request.get("base64");
    String fileName = request.get("fileName");
    String caption = request.get("caption");
    
    MessageResult result = messagingService.sendFileBase64(to, base64Data, fileName, caption);
    return ResponseEntity.ok(result);
}
```

## Testes com cURL

### Enviar Imagem
```bash
curl -X POST "http://localhost:8080/api/messaging/send-image" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "to=5511999999999" \
  -d "imageUrl=https://example.com/image.jpg" \
  -d "caption=Imagem de teste"
```

### Enviar Documento
```bash
curl -X POST "http://localhost:8080/api/messaging/send-document" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "to=5511999999999" \
  -d "documentUrl=https://example.com/documento.pdf" \
  -d "caption=Documento importante" \
  -d "fileName=relatorio.pdf"
```

### Upload e Envio
```bash
curl -X POST "http://localhost:8080/api/messaging/upload-and-send" \
  -F "to=5511999999999" \
  -F "file=@/path/to/file.jpg" \
  -F "caption=Arquivo carregado"
```

### Enviar Base64
```bash
curl -X POST "http://localhost:8080/api/messaging/send-file-base64" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "5511999999999",
    "base64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQ...",
    "fileName": "imagem.jpg",
    "caption": "Imagem codificada"
  }'
```

## Download de Arquivos Recebidos

### Endpoint para Download
```java
@GetMapping("/download/{fileId}")
public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
    try {
        String downloadUrl = instanceUrl + "/download/" + fileId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
            downloadUrl, HttpMethod.GET, request, byte[].class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", fileId);
            
            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(response.getBody());
        } else {
            return ResponseEntity.notFound().build();
        }
        
    } catch (Exception e) {
        return ResponseEntity.badRequest().build();
    }
}
```

### Download via cURL
```bash
curl -X GET "http://localhost:8080/api/messaging/download/{fileId}" \
  -H "Authorization: Bearer {token}" \
  -o arquivo_baixado.jpg
```

## Webhook Testing

### Simular Webhook de Imagem
```bash
curl -X POST "http://localhost:8080/api/messaging/webhook/zapi" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "3EB0796DC6B777",
    "phone": "5511999999999",
    "fromMe": false,
    "momment": 1640995200,
    "status": "RECEIVED",
    "message": {
      "imageMessage": {
        "url": "https://api.z-api.io/instances/test/download/abc123",
        "mimeType": "image/jpeg",
        "caption": "Teste de imagem"
      }
    }
  }'
```

### Simular Webhook de Documento
```bash
curl -X POST "http://localhost:8080/api/messaging/webhook/zapi" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "3EB0796DC6B777",
    "phone": "5511999999999",
    "fromMe": false,
    "momment": 1640995200,
    "status": "RECEIVED",
    "message": {
      "documentMessage": {
        "url": "https://api.z-api.io/instances/test/download/def456",
        "mimeType": "application/pdf",
        "fileName": "documento.pdf",
        "caption": "Documento recebido"
      }
    }
  }'
```

## Configuração de Mídia

### application.yml
```yaml
zapi:
  instance:
    url: ${ZAPI_INSTANCE_URL}
  token: ${ZAPI_TOKEN}
  webhook:
    token: ${ZAPI_WEBHOOK_TOKEN}
  media:
    max-file-size: 20MB
    allowed-types:
      - image/jpeg
      - image/png
      - application/pdf
      - video/mp4
      - audio/mpeg

spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 25MB
```

## Validação de Mídia

### MediaValidator.java
```java
@Component
public class MediaValidator {
    
    @Value("${zapi.media.max-file-size:20971520}") // 20MB
    private long maxFileSize;
    
    @Value("#{'${zapi.media.allowed-types}'.split(',')}")
    private List<String> allowedTypes;
    
    public boolean isValidMedia(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        
        if (file.getSize() > maxFileSize) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && allowedTypes.contains(contentType);
    }
    
    public String getValidationError(MultipartFile file) {
        if (file.isEmpty()) {
            return "Arquivo vazio";
        }
        
        if (file.getSize() > maxFileSize) {
            return "Arquivo muito grande. Máximo: " + (maxFileSize / 1024 / 1024) + "MB";
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            return "Tipo de arquivo não permitido";
        }
        
        return null;
    }
}
```

## Scripts de Automação

### send-media.sh
```bash
#!/bin/bash

API_BASE_URL=${API_BASE_URL:-"http://localhost:8080"}
TO="$1"
FILE_PATH="$2"
CAPTION="$3"

if [ -z "$TO" ] || [ -z "$FILE_PATH" ]; then
    echo "Usage: $0 <phone> <file_path> [caption]"
    exit 1
fi

if [ ! -f "$FILE_PATH" ]; then
    echo "File not found: $FILE_PATH"
    exit 1
fi

echo "Uploading and sending: $FILE_PATH to $TO"

curl -X POST "${API_BASE_URL}/api/messaging/upload-and-send" \
  -F "to=${TO}" \
  -F "file=@${FILE_PATH}" \
  -F "caption=${CAPTION}"
```

### test-media-webhook.sh
```bash
#!/bin/bash

API_BASE_URL=${API_BASE_URL:-"http://localhost:8080"}

echo "Testing image webhook..."
curl -X POST "${API_BASE_URL}/api/messaging/webhook/zapi" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "test123",
    "phone": "5511999999999",
    "fromMe": false,
    "momment": '$(date +%s)',
    "status": "RECEIVED",
    "message": {
      "imageMessage": {
        "url": "https://via.placeholder.com/300.jpg",
        "mimeType": "image/jpeg",
        "caption": "Teste de imagem via webhook"
      }
    }
  }'

echo -e "\n\nTesting document webhook..."
curl -X POST "${API_BASE_URL}/api/messaging/webhook/zapi" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "test456",
    "phone": "5511999999999",
    "fromMe": false,
    "momment": '$(date +%s)',
    "status": "RECEIVED",
    "message": {
      "documentMessage": {
        "url": "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
        "mimeType": "application/pdf",
        "fileName": "teste.pdf",
        "caption": "Documento de teste"
      }
    }
  }'
```

## Troubleshooting

### Problemas Comuns

1. **Arquivo muito grande**: Verificar configuração `spring.servlet.multipart.max-file-size`
2. **Tipo não suportado**: Adicionar MIME type em `zapi.media.allowed-types`
3. **URL de download inválida**: Verificar token e ID do arquivo
4. **Webhook não processa mídia**: Verificar estrutura do payload no `parseIncomingMessage`

### Debug de Mídia
```bash
# Verificar upload
curl -X POST "https://api.z-api.io/instances/{instance}/upload-file" \
  -H "Authorization: Bearer {token}" \
  -F "file=@test.jpg" \
  -v

# Verificar download
curl -X GET "https://api.z-api.io/instances/{instance}/download/{file_id}" \
  -H "Authorization: Bearer {token}" \
  -v
```