# Z-API - Ativação via QR Code

Guia completo para implementar ativação de instâncias Z-API via QR code diretamente na sua aplicação, sem necessidade de acessar o site da Z-API.

## Endpoints Z-API para Ativação

### Base URL
```
https://api.z-api.io/instances/{instance_id}/token/{token}
```

### Autenticação
```
Authorization: Bearer {token}
Content-Type: application/json
```

## Métodos de Ativação Disponíveis

### 1. QR Code em Bytes
```bash
GET https://api.z-api.io/instances/{instance}/token/{token}/qr-code
```

### 2. QR Code como Imagem Base64
```bash
GET https://api.z-api.io/instances/{instance}/token/{token}/qr-code/image
```

### 3. Código de Telefone (Alternativa ao QR)
```bash
GET https://api.z-api.io/instances/{instance}/token/{token}/phone-code/{phone}
```

## Implementação Java - ZApiActivationService

### ZApiActivationService.java
```java
package com.ruby.rubia_server.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class ZApiActivationService {

    @Value("${zapi.instance.url}")
    private String instanceUrl;

    @Value("${zapi.token}")
    private String token;

    private final RestTemplate restTemplate;

    public ZApiActivationService() {
        this.restTemplate = new RestTemplate();
    }

    public ZApiStatus getInstanceStatus() {
        try {
            String url = instanceUrl + "/token/" + token + "/status";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseStatusResponse(response.getBody());
            } else {
                return ZApiStatus.error("Failed to get status");
            }

        } catch (Exception e) {
            log.error("Error getting Z-API status: {}", e.getMessage(), e);
            return ZApiStatus.error("Error: " + e.getMessage());
        }
    }

    public QrCodeResult getQrCodeBytes() {
        try {
            String url = instanceUrl + "/token/" + token + "/qr-code";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return QrCodeResult.success(response.getBody(), "bytes");
            } else {
                return QrCodeResult.error("Failed to get QR code bytes");
            }

        } catch (Exception e) {
            log.error("Error getting QR code bytes: {}", e.getMessage(), e);
            return QrCodeResult.error("Error: " + e.getMessage());
        }
    }

    public QrCodeResult getQrCodeImage() {
        try {
            String url = instanceUrl + "/token/" + token + "/qr-code/image";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String base64Image = (String) response.getBody().get("image");
                return QrCodeResult.success(base64Image, "base64");
            } else {
                return QrCodeResult.error("Failed to get QR code image");
            }

        } catch (Exception e) {
            log.error("Error getting QR code image: {}", e.getMessage(), e);
            return QrCodeResult.error("Error: " + e.getMessage());
        }
    }

    public PhoneCodeResult getPhoneCode(String phoneNumber) {
        try {
            String url = instanceUrl + "/token/" + token + "/phone-code/" + phoneNumber;
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String code = (String) response.getBody().get("code");
                return PhoneCodeResult.success(code, phoneNumber);
            } else {
                return PhoneCodeResult.error("Failed to get phone code");
            }

        } catch (Exception e) {
            log.error("Error getting phone code: {}", e.getMessage(), e);
            return PhoneCodeResult.error("Error: " + e.getMessage());
        }
    }

    public boolean restartInstance() {
        try {
            String url = instanceUrl + "/token/" + token + "/restart";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error restarting instance: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean disconnectInstance() {
        try {
            String url = instanceUrl + "/token/" + token + "/disconnect";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error disconnecting instance: {}", e.getMessage(), e);
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ZApiStatus parseStatusResponse(Map<String, Object> response) {
        String connected = (String) response.get("connected");
        String session = (String) response.get("session");
        String smartphoneConnected = (String) response.get("smartphoneConnected");
        
        return ZApiStatus.builder()
            .connected("true".equals(connected))
            .session(session)
            .smartphoneConnected("true".equals(smartphoneConnected))
            .needsQrCode(!("true".equals(connected)))
            .rawResponse(response)
            .build();
    }
}
```

## Entidades de Resposta

### ZApiStatus.java
```java
package com.ruby.rubia_server.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZApiStatus {
    private boolean connected;
    private String session;
    private boolean smartphoneConnected;
    private boolean needsQrCode;
    private String error;
    private Map<String, Object> rawResponse;

    public static ZApiStatus error(String error) {
        return ZApiStatus.builder()
            .connected(false)
            .needsQrCode(true)
            .error(error)
            .build();
    }
}
```

### QrCodeResult.java
```java
package com.ruby.rubia_server.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResult {
    private boolean success;
    private Object data;
    private String type;
    private String error;

    public static QrCodeResult success(Object data, String type) {
        return QrCodeResult.builder()
            .success(true)
            .data(data)
            .type(type)
            .build();
    }

    public static QrCodeResult error(String error) {
        return QrCodeResult.builder()
            .success(false)
            .error(error)
            .build();
    }
}
```

### PhoneCodeResult.java
```java
package com.ruby.rubia_server.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneCodeResult {
    private boolean success;
    private String code;
    private String phoneNumber;
    private String error;

    public static PhoneCodeResult success(String code, String phoneNumber) {
        return PhoneCodeResult.builder()
            .success(true)
            .code(code)
            .phoneNumber(phoneNumber)
            .build();
    }

    public static PhoneCodeResult error(String error) {
        return PhoneCodeResult.builder()
            .success(false)
            .error(error)
            .build();
    }
}
```

## Controller para Ativação

### ZApiActivationController.java
```java
package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.service.ZApiActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/zapi/activation")
@RequiredArgsConstructor
@Slf4j
public class ZApiActivationController {

    private final ZApiActivationService activationService;

    @GetMapping("/status")
    public ResponseEntity<ZApiStatus> getStatus() {
        ZApiStatus status = activationService.getInstanceStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/qr-code/bytes")
    public ResponseEntity<byte[]> getQrCodeBytes() {
        QrCodeResult result = activationService.getQrCodeBytes();
        
        if (result.isSuccess()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", "qrcode.png");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body((byte[]) result.getData());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/qr-code/image")
    public ResponseEntity<QrCodeResult> getQrCodeImage() {
        QrCodeResult result = activationService.getQrCodeImage();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/phone-code/{phone}")
    public ResponseEntity<PhoneCodeResult> getPhoneCode(@PathVariable String phone) {
        PhoneCodeResult result = activationService.getPhoneCode(phone);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restart")
    public ResponseEntity<Map<String, Object>> restartInstance() {
        boolean success = activationService.restartInstance();
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Instance restarted successfully" : "Failed to restart instance"
        ));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectInstance() {
        boolean success = activationService.disconnectInstance();
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Instance disconnected successfully" : "Failed to disconnect instance"
        ));
    }

    @PostMapping("/webhook/connected")
    public ResponseEntity<String> handleConnectedWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Z-API instance connected: {}", payload);
            
            String instanceId = (String) payload.get("instanceId");
            Boolean connected = (Boolean) payload.get("connected");
            
            if (Boolean.TRUE.equals(connected)) {
                log.info("Instance {} successfully connected to WhatsApp", instanceId);
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing connected webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/webhook/disconnected")
    public ResponseEntity<String> handleDisconnectedWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Z-API instance disconnected: {}", payload);
            
            String instanceId = (String) payload.get("instanceId");
            String error = (String) payload.get("error");
            
            log.warn("Instance {} disconnected. Error: {}", instanceId, error);
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing disconnected webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
```

## Frontend React - Componente de Ativação

### ZApiActivation.tsx
```typescript
interface ZApiStatus {
  connected: boolean;
  session: string;
  smartphoneConnected: boolean;
  needsQrCode: boolean;
  error?: string;
}

interface QrCodeResult {
  success: boolean;
  data: string;
  type: string;
  error?: string;
}

interface PhoneCodeResult {
  success: boolean;
  code: string;
  phoneNumber: string;
  error?: string;
}

const ZApiActivation: React.FC = () => {
  const [status, setStatus] = useState<ZApiStatus | null>(null);
  const [qrCodeImage, setQrCodeImage] = useState<string>('');
  const [phoneCode, setPhoneCode] = useState<PhoneCodeResult | null>(null);
  const [phoneNumber, setPhoneNumber] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [activationMethod, setActivationMethod] = useState<'qr' | 'phone'>('qr');

  const API_BASE = '/api/zapi/activation';

  const checkStatus = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/status`);
      const data: ZApiStatus = await response.json();
      setStatus(data);
    } catch (error) {
      console.error('Error checking status:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadQrCode = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/qr-code/image`);
      const data: QrCodeResult = await response.json();
      
      if (data.success) {
        setQrCodeImage(data.data);
      } else {
        console.error('Error loading QR code:', data.error);
      }
    } catch (error) {
      console.error('Error loading QR code:', error);
    } finally {
      setLoading(false);
    }
  };

  const generatePhoneCode = async () => {
    if (!phoneNumber) return;
    
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/phone-code/${phoneNumber}`);
      const data: PhoneCodeResult = await response.json();
      setPhoneCode(data);
    } catch (error) {
      console.error('Error generating phone code:', error);
    } finally {
      setLoading(false);
    }
  };

  const restartInstance = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/restart`, { method: 'POST' });
      const data = await response.json();
      
      if (data.success) {
        await checkStatus();
      }
    } catch (error) {
      console.error('Error restarting instance:', error);
    } finally {
      setLoading(false);
    }
  };

  const disconnectInstance = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/disconnect`, { method: 'POST' });
      const data = await response.json();
      
      if (data.success) {
        await checkStatus();
        setQrCodeImage('');
        setPhoneCode(null);
      }
    } catch (error) {
      console.error('Error disconnecting instance:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkStatus();
  }, []);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (status?.needsQrCode && activationMethod === 'qr') {
      loadQrCode();
      interval = setInterval(checkStatus, 5000);
    }
    
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [status?.needsQrCode, activationMethod]);

  return (
    <div className="max-w-md mx-auto p-6 bg-white rounded-lg shadow-lg">
      <h2 className="text-2xl font-bold mb-6 text-center">
        Ativação Z-API
      </h2>

      <div className="mb-6">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium">Status:</span>
          <span className={`px-2 py-1 rounded text-xs font-semibold ${
            status?.connected 
              ? 'bg-green-100 text-green-800' 
              : 'bg-red-100 text-red-800'
          }`}>
            {status?.connected ? 'Conectado' : 'Desconectado'}
          </span>
        </div>
        
        <button
          onClick={checkStatus}
          disabled={loading}
          className="w-full py-2 px-4 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50"
        >
          {loading ? 'Verificando...' : 'Verificar Status'}
        </button>
      </div>

      {status?.needsQrCode && (
        <div className="mb-6">
          <div className="flex mb-4">
            <button
              onClick={() => setActivationMethod('qr')}
              className={`flex-1 py-2 px-4 rounded-l ${
                activationMethod === 'qr' 
                  ? 'bg-blue-500 text-white' 
                  : 'bg-gray-200'
              }`}
            >
              QR Code
            </button>
            <button
              onClick={() => setActivationMethod('phone')}
              className={`flex-1 py-2 px-4 rounded-r ${
                activationMethod === 'phone' 
                  ? 'bg-blue-500 text-white' 
                  : 'bg-gray-200'
              }`}
            >
              Código
            </button>
          </div>

          {activationMethod === 'qr' && (
            <div className="text-center">
              {qrCodeImage ? (
                <div>
                  <img 
                    src={`data:image/png;base64,${qrCodeImage}`}
                    alt="QR Code para ativação"
                    className="mx-auto mb-4 border rounded"
                  />
                  <p className="text-sm text-gray-600 mb-4">
                    Escaneie o QR code com o WhatsApp
                  </p>
                  <button
                    onClick={loadQrCode}
                    disabled={loading}
                    className="py-2 px-4 bg-green-500 text-white rounded hover:bg-green-600 disabled:opacity-50"
                  >
                    {loading ? 'Carregando...' : 'Gerar Novo QR'}
                  </button>
                </div>
              ) : (
                <button
                  onClick={loadQrCode}
                  disabled={loading}
                  className="py-2 px-4 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50"
                >
                  {loading ? 'Carregando...' : 'Carregar QR Code'}
                </button>
              )}
            </div>
          )}

          {activationMethod === 'phone' && (
            <div>
              <div className="mb-4">
                <input
                  type="text"
                  placeholder="Número do telefone (5511999999999)"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full py-2 px-3 border rounded focus:outline-none focus:border-blue-500"
                />
              </div>
              
              <button
                onClick={generatePhoneCode}
                disabled={loading || !phoneNumber}
                className="w-full py-2 px-4 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50 mb-4"
              >
                {loading ? 'Gerando...' : 'Gerar Código'}
              </button>
              
              {phoneCode?.success && (
                <div className="p-4 bg-green-50 border border-green-200 rounded">
                  <p className="text-sm font-medium mb-2">Código gerado:</p>
                  <p className="text-2xl font-bold text-center text-green-800 mb-2">
                    {phoneCode.code}
                  </p>
                  <p className="text-xs text-gray-600">
                    Digite este código no WhatsApp em "Conectar com número de telefone"
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {status?.connected && (
        <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded">
          <p className="text-green-800 font-medium mb-2">✅ WhatsApp Conectado</p>
          <p className="text-sm text-gray-600">
            Smartphone: {status.smartphoneConnected ? 'Conectado' : 'Desconectado'}
          </p>
        </div>
      )}

      <div className="flex space-x-2">
        <button
          onClick={restartInstance}
          disabled={loading}
          className="flex-1 py-2 px-4 bg-yellow-500 text-white rounded hover:bg-yellow-600 disabled:opacity-50"
        >
          Reiniciar
        </button>
        
        <button
          onClick={disconnectInstance}
          disabled={loading}
          className="flex-1 py-2 px-4 bg-red-500 text-white rounded hover:bg-red-600 disabled:opacity-50"
        >
          Desconectar
        </button>
      </div>
    </div>
  );
};

export default ZApiActivation;
```

## Configuração de Webhooks

### application.yml
```yaml
zapi:
  instance:
    url: ${ZAPI_INSTANCE_URL:https://api.z-api.io/instances/SUA_INSTANCIA}
  token: ${ZAPI_TOKEN:seu_token_aqui}
  webhooks:
    connected: ${ZAPI_WEBHOOK_CONNECTED:https://seu-dominio.com/api/zapi/activation/webhook/connected}
    disconnected: ${ZAPI_WEBHOOK_DISCONNECTED:https://seu-dominio.com/api/zapi/activation/webhook/disconnected}

logging:
  level:
    com.ruby.rubia_server.core.service.ZApiActivationService: DEBUG
```

## Scripts para Configurar Webhooks

### configure-webhooks.sh
```bash
#!/bin/bash

INSTANCE=${ZAPI_INSTANCE_ID}
TOKEN=${ZAPI_TOKEN}
BASE_URL=${WEBHOOK_BASE_URL:-"https://seu-dominio.com"}

echo "Configurando webhooks Z-API..."

# Webhook para conexão
curl -X PUT "https://api.z-api.io/instances/${INSTANCE}/token/${TOKEN}/update-webhook-connected" \
  -H "Content-Type: application/json" \
  -d "{
    \"value\": \"${BASE_URL}/api/zapi/activation/webhook/connected\"
  }"

echo "Webhook connected configurado"

# Webhook para desconexão
curl -X PUT "https://api.z-api.io/instances/${INSTANCE}/token/${TOKEN}/update-webhook-disconnected" \
  -H "Content-Type: application/json" \
  -d "{
    \"value\": \"${BASE_URL}/api/zapi/activation/webhook/disconnected\"
  }"

echo "Webhook disconnected configurado"

echo "Webhooks configurados com sucesso!"
```

## Comandos de Teste

### Verificar Status
```bash
curl -X GET "http://localhost:8080/api/zapi/activation/status"
```

### Obter QR Code (Base64)
```bash
curl -X GET "http://localhost:8080/api/zapi/activation/qr-code/image"
```

### Obter QR Code (Bytes)
```bash
curl -X GET "http://localhost:8080/api/zapi/activation/qr-code/bytes" \
  --output qrcode.png
```

### Gerar Código de Telefone
```bash
curl -X GET "http://localhost:8080/api/zapi/activation/phone-code/5511999999999"
```

### Reiniciar Instância
```bash
curl -X POST "http://localhost:8080/api/zapi/activation/restart"
```

### Desconectar Instância
```bash
curl -X POST "http://localhost:8080/api/zapi/activation/disconnect"
```

### Teste Direto Z-API
```bash
# Status
curl -X GET "https://api.z-api.io/instances/{instance}/token/{token}/status"

# QR Code
curl -X GET "https://api.z-api.io/instances/{instance}/token/{token}/qr-code/image"

# Código de telefone
curl -X GET "https://api.z-api.io/instances/{instance}/token/{token}/phone-code/5511999999999"
```

## Scripts de Automação

### check-activation.sh
```bash
#!/bin/bash

API_BASE_URL=${API_BASE_URL:-"http://localhost:8080"}

echo "Verificando status de ativação Z-API..."

status=$(curl -s "${API_BASE_URL}/api/zapi/activation/status")
echo "Status: $status"

connected=$(echo "$status" | jq -r '.connected')
needs_qr=$(echo "$status" | jq -r '.needsQrCode')

if [ "$connected" = "true" ]; then
    echo "✅ Z-API conectado com sucesso!"
elif [ "$needs_qr" = "true" ]; then
    echo "⚠️  Z-API precisa de ativação via QR code"
    echo "Gerando QR code..."
    
    curl -s "${API_BASE_URL}/api/zapi/activation/qr-code/bytes" \
      --output qrcode.png
    
    echo "QR code salvo em qrcode.png"
    echo "Escaneie com o WhatsApp para ativar"
else
    echo "❌ Erro no status da Z-API"
fi
```

### auto-reconnect.sh
```bash
#!/bin/bash

API_BASE_URL=${API_BASE_URL:-"http://localhost:8080"}
CHECK_INTERVAL=${CHECK_INTERVAL:-30}

echo "Iniciando monitoramento Z-API..."
echo "Verificando a cada ${CHECK_INTERVAL} segundos"

while true; do
    status=$(curl -s "${API_BASE_URL}/api/zapi/activation/status")
    connected=$(echo "$status" | jq -r '.connected // false')
    
    if [ "$connected" != "true" ]; then
        echo "$(date): Z-API desconectado, tentando reconectar..."
        
        restart_result=$(curl -s -X POST "${API_BASE_URL}/api/zapi/activation/restart")
        restart_success=$(echo "$restart_result" | jq -r '.success // false')
        
        if [ "$restart_success" = "true" ]; then
            echo "$(date): Reinício bem-sucedido"
        else
            echo "$(date): Falha no reinício, necessária ativação manual"
        fi
    else
        echo "$(date): Z-API conectado ✅"
    fi
    
    sleep "$CHECK_INTERVAL"
done
```

## Monitoramento e Logs

### ZApiMonitoringService.java
```java
@Service
@Slf4j
public class ZApiMonitoringService {

    private final ZApiActivationService activationService;
    
    @Scheduled(fixedRate = 60000) // Check every minute
    public void monitorConnection() {
        try {
            ZApiStatus status = activationService.getInstanceStatus();
            
            if (!status.isConnected()) {
                log.warn("Z-API instance is disconnected. Status: {}", status);
                
                if (status.getError() != null) {
                    log.error("Z-API error: {}", status.getError());
                }
                
                // Tentar reconexão automática
                boolean restarted = activationService.restartInstance();
                if (restarted) {
                    log.info("Z-API instance restart initiated");
                } else {
                    log.error("Failed to restart Z-API instance");
                }
            } else {
                log.debug("Z-API instance is connected and healthy");
            }
            
        } catch (Exception e) {
            log.error("Error monitoring Z-API connection: {}", e.getMessage(), e);
        }
    }
}
```

## Troubleshooting

### Problemas Comuns

1. **QR Code não carrega**
   - Verificar se token e instância estão corretos
   - Verificar conectividade com Z-API

2. **Conexão não persiste**
   - Verificar se WhatsApp multi-dispositivos está ativado
   - Verificar otimização de bateria no celular

3. **Webhooks não funcionam**
   - Verificar se URL é HTTPS
   - Verificar configuração dos webhooks

### Logs Úteis
```bash
# Ver logs de ativação
grep -i "ZApiActivation" /var/log/rubia-server/application.log | tail -20

# Ver logs de webhook
grep -i "webhook" /var/log/rubia-server/application.log | grep -i "z-api" | tail -10

# Ver erros específicos
grep -i "error" /var/log/rubia-server/application.log | grep -i "z-api" | tail -10
```

## Resumo de Funcionalidades

✅ **Implementação Completa na Aplicação** - Não precisa acessar site Z-API
✅ **3 Métodos de Ativação** - QR code bytes, base64 e código de telefone  
✅ **Monitoramento Automático** - Status em tempo real
✅ **Reconexão Automática** - Restart sem nova ativação
✅ **Webhooks Configurados** - Notificações de conexão/desconexão
✅ **Interface React** - UI completa para ativação
✅ **Scripts de Automação** - Facilita deploy e monitoramento

A ativação é feita diretamente na sua aplicação usando os endpoints da Z-API, sem necessidade de acessar o painel web deles.