# Debug de Detecção de Desconexão - Z-API

## Problema Relatado
Sua instância está desconectada na Z-API, mas o sistema não mostra o QR Code para reconexão.

## Passos para Diagnóstico

### 1. **Verificar Logs do Sistema**

Inicie a aplicação backend e observe os logs para mensagens como:
```
🔄 Updating instance {instanceId} from status check. Current status: {status}
📊 Z-API status result for instance {instanceId}: connected={true/false}, error={erro}
⚠️  Instance {instanceId} status CHANGED: {status_antigo} -> {status_novo}
```

### 2. **Forçar Verificação Manual**

Na interface do sistema:
1. Vá para a tela de gerenciamento de instâncias WhatsApp
2. Encontre sua instância na tabela
3. Clique no botão **"Forçar Status"** (ícone de sincronização)
4. Observe as mensagens que aparecem

### 3. **Verificar Configuração Z-API**

Confirme se as seguintes variáveis estão configuradas:

```bash
# No seu .env ou application.properties
Z_API_BASE_URL=https://api.z-api.io
Z_API_CLIENT_TOKEN=seu_client_token_aqui
```

### 4. **Testar Endpoint Diretamente**

Teste o endpoint de health check:
```bash
GET /api/whatsapp-setup/health-check
```

Deve retornar algo como:
```json
{
  "success": true,
  "totalInstances": 1,
  "connectedInstances": 0,
  "disconnectedInstances": 1,
  "monitoringActive": true
}
```

### 5. **Verificar Status na Z-API**

Teste manualmente o endpoint da Z-API:
```bash
GET https://api.z-api.io/instances/{SUA_INSTANCIA}/token/{SEU_TOKEN}/status

Headers:
Client-Token: {SEU_CLIENT_TOKEN}
```

## Cenários Possíveis

### ✅ **Cenário 1: Sistema detecta corretamente**
- Logs mostram mudança de status para `DISCONNECTED`
- Interface mostra alerta vermelho com botão "Reconectar"
- Botão "Reconectar" abre modal com QR Code

### ❌ **Cenário 2: Sistema não detecta**
- Status permanece `CONNECTED` no banco de dados
- Interface não mostra alertas ou botões de reconexão
- **Solução**: Usar botão "Forçar Status" para sincronizar

### ❌ **Cenário 3: Erro de configuração**
- Logs mostram erros de conexão com Z-API
- Mensagens como "Failed to check status"
- **Solução**: Verificar `CLIENT_TOKEN` e URLs da Z-API

### ❌ **Cenário 4: Instância não encontrada**
- Logs mostram "Instance not found in database"
- **Solução**: Verificar se `instanceId` no banco coincide com Z-API

## Solução Rápida

1. **Clique em "Forçar Status"** na sua instância
2. Se aparecer como `DISCONNECTED`, clique em **"Reconectar"**
3. Modal deve abrir com QR Code
4. Escaneie com seu WhatsApp

## Logs Esperados (Sucesso)

```
🔍 Checking Z-API status for instance 3E48B40A3... at URL: https://api.z-api.io/instances/3E48B40A3.../token/.../status
📊 Z-API status result for instance 3E48B40A3...: connected=false, error=Phone disconnected from WhatsApp
📈 Status comparison for instance 3E48B40A3...: CONNECTED -> DISCONNECTED
⚠️  Instance 3E48B40A3... status CHANGED: CONNECTED -> DISCONNECTED
```

## Se Nada Funcionar

1. **Restart do Sistema**: Reinicie backend e frontend
2. **Verificar Banco**: Consulte tabela `whatsapp_instances` diretamente
3. **Logs de Rede**: Verifique se há bloqueios de firewall para Z-API
4. **Client Token**: Confirme se o token da Z-API está válido

## Comando SQL Útil

Para verificar status no banco:
```sql
SELECT instance_id, status, last_status_check, error_message 
FROM whatsapp_instances 
WHERE is_active = true;
```

## Próximos Passos

Depois de forçar a verificação:
- Se status mudar para `DISCONNECTED`: QR Code deve aparecer
- Se status permanecer `CONNECTED`: Problema pode ser na comunicação com Z-API
- Se der erro: Verificar configurações de API