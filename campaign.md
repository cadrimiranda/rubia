📋 Fluxo do Sistema de Campanhas WhatsApp

1. Criação da Campanha

- Endpoint: POST /api/campaigns/process (CampaignController.java:73-148)
- Recebe arquivo Excel/CSV com contatos
- Processa via CampaignProcessingService
- Cria campanha + contacts no banco

2. Enfileiramento Automático

- Service: SecureCampaignQueueService.enqueueCampaign() (SecureCampaignQueueService.java:91-149)
- Adiciona campanha ao Redis (fila segura)
- Cada contato vira um SecureCampaignQueueItem com timestamp agendado
- Usa Sorted Set do Redis para ordenação temporal

3. Processamento Automático

- Scheduler: @Scheduled(fixedDelay = 30000) roda a cada 30s (SecureCampaignQueueService.java:154-215)
- Usa lock distribuído no Redis para evitar concorrência
- Busca itens prontos: rangeByScore(QUEUE_KEY, 0, now.toEpochSecond())
- Processa até 10 itens por ciclo

4. Envio via Z-API

Fluxo de envio:
SecureCampaignQueueService.processSecureQueueItem()
→ CampaignMessagingService.sendSingleMessageAsync()
→ CampaignDelaySchedulingService.scheduleMessageSend()
→ MessagingService.sendMessage()
→ ZApiAdapter.sendMessage()
→ Z-API HTTP call

5. Integração Z-API

- Sim, usa Z-API adapter (ZApiAdapter.java:97-145)
- Endpoint: {zapi-url}/send-text
- Headers: client-token, Content-Type: application/json
- Body: {"phone": "+5511999999999", "message": "texto"}
- Retorna messageId em caso de sucesso

6. Controle de Fluxo

- Delays aleatórios: minDelayMs - maxDelayMs (configurável)
- Lotes: batchSize mensagens + pausa de batchPauseMinutes
- Retry automático: maxRetries tentativas com retryDelayMs
- Randomização opcional da ordem dos contatos

7. Monitoramento

- Status: PENDING → SENT/FAILED
- Estatísticas em tempo real via Redis
- WebSocket para updates no frontend
- Logs detalhados de cada envio

🔧 Configuração Típica

campaign.queue.provider=redis
campaign.messaging.min-delay-ms=5000
campaign.messaging.max-delay-ms=15000
campaign.messaging.batch-size=50
campaign.messaging.batch-pause-minutes=10
campaign.messaging.max-retries=3
