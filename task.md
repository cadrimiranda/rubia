⏺ Plano: Implementação de Comunicação em Tempo Real

🎯 Objetivo

Implementar notificação em tempo real para que o frontend receba automaticamente novas mensagens quando chegam via webhook do WhatsApp.

🏗️ Arquitetura Proposta

WebSocket + Spring Boot + React - Solução mais robusta e eficiente

📋 Fases de Implementação

Fase 1: Backend - Configuração WebSocket (Spring Boot)

Adicionar dependências WebSocket

- spring-boot-starter-websocket
- Configurar CORS para WebSocket
  Criar configuração WebSocket
- WebSocketConfig.java - Configurar endpoints e interceptors
- WebSocketHandler.java - Gerenciar conexões e mensagens
  Implementar serviço de notificação
- WebSocketNotificationService.java - Enviar mensagens para clientes conectados
- Integrar com MessagingService.processIncomingMessage()
  Segurança e autenticação
- Autenticar conexões WebSocket via JWT
- Associar sessões WebSocket a usuários/empresas
  Fase 2: Frontend - Cliente WebSocket (React)

Criar hook customizado

- useWebSocket.ts - Gerenciar conexão WebSocket
- Reconexão automática em caso de queda
  Integrar com Zustand store
- Atualizar useChatStore.ts para receber mensagens via WebSocket
- Manter sincronização entre WebSocket e estado local
  Componentes de notificação
- Indicadores visuais para novas mensagens
- Sons de notificação (opcional)
  Fase 3: Integração e Testes

Conectar webhook → WebSocket

- Modificar processIncomingMessage() para enviar notificação WebSocket
- Filtrar destinatários por empresa/conversa
  Testes de integração
- Simular webhooks de mensagens
- Verificar notificação em tempo real no frontend
  Tratamento de erros
- Fallback para polling em caso de falha WebSocket
- Logs e monitoramento
  Fase 4: Melhorias e Otimizações

Escalabilidade

- Redis para gerenciar sessões WebSocket (futuro)
- Load balancing sticky sessions
  Funcionalidades extras
- Status "usuário digitando"
- Confirmação de leitura em tempo real
- Presença online/offline
  🛠️ Tecnologias

Backend: Spring WebSocket, SockJS, STOMP
Frontend: WebSocket API nativo, React hooks
Protocolo: WebSocket sobre HTTP/HTTPS
⏱️ Estimativa

Fase 1: 2-3 dias
Fase 2: 2 dias
Fase 3: 1-2 dias
Fase 4: 1-2 dias
Total: 6-9 dias
🔧 Arquivos a Criar/Modificar

Backend

WebSocketConfig.java (novo)
ChatWebSocketHandler.java (novo)
WebSocketNotificationService.java (novo)
MessagingService.java (modificar)
SecurityConfig.java (modificar para WebSocket)
Frontend

hooks/useWebSocket.ts (novo)
store/useChatStore.ts (modificar)
components/BloodCenterChat.tsx (modificar)
utils/websocketClient.ts (novo)
