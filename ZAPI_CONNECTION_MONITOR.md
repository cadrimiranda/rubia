# Sistema de Monitoramento de Conexão Z-API

Este documento descreve o sistema implementado para detectar e lidar com desconexões de instâncias WhatsApp via Z-API.

## Funcionalidades Implementadas

### 1. **Backend - Detecção de Desconexão**

#### Webhook Endpoints
- `POST /api/webhooks/z-api/disconnected/{instanceId}` - Recebe notificações de desconexão
- `POST /api/webhooks/z-api/connected/{instanceId}` - Recebe notificações de reconexão
- `POST /api/webhooks/z-api/status/{instanceId}` - Recebe atualizações de status

#### Verificação Periódica
- Task schedulada executa a cada 2 minutos
- Verifica status de todas as instâncias ativas via endpoint `/status` da Z-API
- Atualiza status no banco de dados automaticamente

#### Endpoints de Gerenciamento
- `GET /api/whatsapp-setup/{instanceId}/connection-status` - Verifica status atual da instância
- `POST /api/whatsapp-setup/{instanceId}/reconnect` - Inicia processo de reconexão

### 2. **Frontend - Interface de Reconexão**

#### Detecção Visual
- Tabela de instâncias mostra status com cores (vermelho para desconectado)
- Alertas automáticos quando instâncias desconectadas são detectadas
- Botões de ação específicos para instâncias desconectadas

#### Processo de Reconexão
- Botão "Reconectar" para instâncias desconectadas
- Modal com QR Code para escaneamento
- Feedback visual do processo de reconexão

## Configuração

### 1. **Variáveis de Ambiente**

Adicione ao seu arquivo `.env` ou configuração:

```bash
# Z-API Connection Monitor
Z_API_BASE_URL=https://api.z-api.io
Z_API_CLIENT_TOKEN=seu_client_token_aqui
Z_API_CONNECTION_MONITOR_ENABLED=true
Z_API_CONNECTION_MONITOR_CHECK_INTERVAL=120000
Z_API_WEBHOOK_BASE_URL=https://sua-aplicacao.com
```

### 2. **Configuração de Webhooks na Z-API**

Para cada instância, configure os webhooks:

#### Webhook de Desconexão:
```bash
PUT https://api.z-api.io/instances/{INSTANCE_ID}/token/{TOKEN}/update-webhook-disconnected

Headers:
{
  "Client-Token": "SEU_CLIENT_TOKEN"
}

Body:
{
  "value": "https://sua-aplicacao.com/api/webhooks/z-api/disconnected/{INSTANCE_ID}"
}
```

#### Webhook de Reconexão:
```bash
PUT https://api.z-api.io/instances/{INSTANCE_ID}/token/{TOKEN}/update-webhook-connected

Headers:
{
  "Client-Token": "SEU_CLIENT_TOKEN"
}

Body:
{
  "value": "https://sua-aplicacao.com/api/webhooks/z-api/connected/{INSTANCE_ID}"
}
```

## Fluxo de Funcionamento

### 1. **Detect Desconexão**
1. Z-API detecta que telefone foi desconectado
2. Envia webhook para `/api/webhooks/z-api/disconnected/{instanceId}`
3. Sistema atualiza status da instância para `DISCONNECTED`
4. WebSocket notifica frontend em tempo real
5. Interface mostra alerta e botão de reconexão

### 2. **Processo de Reconexão**
1. Usuário clica em "Reconectar"
2. Sistema verifica status atual na Z-API
3. Se já conectado: atualiza status local
4. Se desconectado: mostra modal com QR Code
5. Usuário escaneia QR Code com WhatsApp
6. Z-API envia webhook de reconexão
7. Sistema atualiza status para `CONNECTED`

### 3. **Monitoramento Contínuo**
1. Task schedulada executa a cada 2 minutos
2. Verifica status de todas as instâncias ativas
3. Compara com status no banco de dados
4. Atualiza discrepâncias e notifica via WebSocket

## Estrutura do Banco de Dados

A tabela `whatsapp_instances` foi estendida com:
- `last_connected_at` - Último momento de conexão
- `last_status_check` - Última verificação de status
- `error_message` - Mensagem de erro da desconexão

## Notificações WebSocket

### Estrutura da Notificação:
```json
{
  "type": "INSTANCE_STATUS_CHANGE",
  "instanceId": "3C4E1234567890AB",
  "status": "DISCONNECTED",
  "phoneNumber": "5511999999999",
  "displayName": "WhatsApp Vendas",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Canal: `/topic/instance-status`
- Enviado para usuários da empresa específica
- Frontend pode reagir em tempo real às mudanças

## Tratamento de Erros

### Cenários Tratados:
1. **Instância não encontrada** - Log de warning
2. **Falha na API Z-API** - Retry automático
3. **Webhook inválido** - Sempre retorna 200 para Z-API
4. **Timeout de conexão** - Configurável via properties

### Logs:
- `INFO` - Mudanças de status normais
- `WARN` - Instâncias não encontradas
- `ERROR` - Falhas de comunicação com Z-API
- `DEBUG` - Detalhes de verificações periódicas

## Monitoramento e Métricas

### Health Checks:
- Endpoint Actuator expõe métricas de conexão
- Número de instâncias ativas/desconectadas
- Última verificação de status

### Alertas Sugeridos:
- Alta taxa de desconexões
- Falhas recorrentes na API Z-API
- Instâncias desconectadas por mais de X minutos

## Próximos Passos

### Melhorias Sugeridas:
1. **Alertas por Email/SMS** - Notificar administradores
2. **Dashboard de Monitoramento** - Visualização centralizada
3. **Retry Automático** - Tentativas de reconexão automáticas
4. **Métricas Avançadas** - Tempo médio de desconexão, SLA
5. **Backup de Instâncias** - Failover automático entre instâncias

### Integrações:
- **Prometheus/Grafana** - Métricas detalhadas
- **Slack/Teams** - Notificações de equipe
- **PagerDuty** - Alertas críticos