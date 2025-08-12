# Sistema de Notificações Persistentes

Este documento descreve o sistema de notificações persistentes implementado para mensagens de chat no Rubia.

## 📋 Funcionalidades

✅ **Notificações Persistentes**: As notificações são salvas no `localStorage` e persistem entre:
- Recarregamentos da página
- Logout/Login
- Fechamento/abertura do navegador

✅ **Contadores por Conversa**: Cada conversa mantém um contador independente de mensagens não lidas

✅ **Integração WebSocket**: Notificações são criadas automaticamente quando mensagens chegam via WebSocket

✅ **Limpeza Automática**: Notificações são removidas quando o usuário visualiza a conversa

✅ **Multi-usuário**: Cada usuário tem suas próprias notificações isoladas

## 🏗️ Arquitetura

### Componentes Principais

1. **`notificationService`** (`src/services/notificationService.ts`)
   - Serviço singleton para gerenciar notificações
   - Persiste dados no localStorage
   - Notifica componentes sobre mudanças

2. **`useNotifications`** (`src/hooks/useNotifications.ts`)
   - Hook React para usar notificações em componentes
   - Sincroniza com o serviço e re-renderiza quando necessário

3. **`DonorSidebar`** (modificado)
   - Exibe contadores de notificação
   - Usa notificações persistentes em vez de apenas `donor.unread`

4. **`WebSocketEventHandlers`** (modificado)
   - Cria notificações quando mensagens chegam de outros usuários
   - Não cria notificação se a conversa estiver ativa

5. **`BloodCenterChat`** (modificado)
   - Remove notificações quando usuário seleciona uma conversa

## 📊 Estrutura de Dados

### ConversationNotification
```typescript
interface ConversationNotification {
  conversationId: string;   // ID da conversa
  donorId: string;          // ID do doador/cliente
  donorName: string;        // Nome do doador/cliente  
  lastMessageId: string;    // ID da última mensagem
  timestamp: number;        // Timestamp da última notificação
  count: number;            // Número de mensagens não lidas
}
```

### Armazenamento localStorage
```typescript
{
  "rubia_message_notifications": {
    "user-123": {
      "conversation-456": {
        "conversationId": "conversation-456",
        "donorId": "donor-789",
        "donorName": "João Silva",
        "lastMessageId": "msg-999",
        "timestamp": 1703123456789,
        "count": 3
      }
    }
  }
}
```

## 🔄 Fluxo de Funcionamento

### 1. Nova Mensagem Chega
```
WebSocket → handleNewMessage() → notificationService.addNotification() → localStorage
                                                                     ↓
                                           notifica listeners → useNotifications → re-render
```

### 2. Usuário Visualiza Conversa
```
DonorSelect → handleDonorSelect() → removeNotification() → localStorage
                                                      ↓
                                   notifica listeners → useNotifications → re-render
```

### 3. Exibição no DonorSidebar
```
DonorSidebar → useNotifications() → getNotificationCount() → exibe contador
```

## 🎯 Como Usar

### Exemplo Básico
```typescript
import { useNotifications } from '../hooks/useNotifications';

function MeuComponente() {
  const { getNotificationCount, addNotification, removeNotification } = useNotifications();
  
  // Obter contador de uma conversa
  const count = getNotificationCount('conversation-123');
  
  // Adicionar notificação
  const handleNewMessage = () => {
    addNotification(
      'conversation-123',
      'donor-456', 
      'João Silva',
      'msg-789',
      Date.now()
    );
  };
  
  // Remover notificação
  const handleViewConversation = () => {
    removeNotification('conversation-123');
  };
}
```

### Exemplo no DonorSidebar
```typescript
// Exibir contador se houver notificações
{((donor.conversationId && getNotificationCount(donor.conversationId) > 0) || donor.unread > 0) && (
  <div className="notification-badge">
    {donor.conversationId ? getNotificationCount(donor.conversationId) || donor.unread : donor.unread}
  </div>
)}
```

## 🧪 Testando

1. **Componente de Teste**: Use `<NotificationTest />` para testar manualmente
2. **Teste de Persistência**:
   - Adicione notificações
   - Recarregue a página (F5)
   - Faça logout/login
   - Verifique se persistiram

3. **Teste de Integração**:
   - Abra duas abas do chat
   - Envie mensagem de uma aba
   - Verifique se notificação aparece na outra aba

## 🔧 Configuração

### localStorage Key
O sistema usa a chave `rubia_message_notifications` no localStorage.

### Limpeza de Dados
Para limpar todas as notificações:
```typescript
const { clearAllNotifications } = useNotifications();
clearAllNotifications();
```

## 📈 Benefícios

1. **UX Melhorado**: Usuários não perdem notificações importantes
2. **Persistência**: Funciona mesmo com instabilidade de rede
3. **Performance**: Dados locais = carregamento instantâneo
4. **Isolamento**: Cada usuário tem suas próprias notificações
5. **Flexibilidade**: Fácil de estender para novos tipos de notificação

## 🚀 Próximos Passos

- [ ] Implementar notificações no título da página
- [ ] Adicionar som diferenciado por tipo de mensagem
- [ ] Implementar notificações push (service worker)
- [ ] Dashboard de notificações centralizadas
- [ ] Configurações de notificação por usuário