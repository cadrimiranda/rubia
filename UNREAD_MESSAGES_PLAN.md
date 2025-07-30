# 📋 Plano para Implementação de Contagem de Mensagens Não Lidas

## 🎯 Objetivo
Implementar um sistema preciso de contagem de mensagens não lidas que funcione tanto em tempo real (WebSocket) quanto após refresh da página (persistência).

## 🏗️ Arquitetura da Solução

### **1. Backend - Database & Repository Layer**

#### **1.1 Criação de tabela de controle de leitura**
```sql
-- Nova tabela para rastrear mensagens lidas por usuário
CREATE TABLE message_read_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    read_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(message_id, user_id) -- Evita duplicatas
);

-- Índices para performance
CREATE INDEX idx_message_read_status_conversation_user ON message_read_status(conversation_id, user_id);
CREATE INDEX idx_message_read_status_message ON message_read_status(message_id);
CREATE INDEX idx_message_read_status_company ON message_read_status(company_id);
```

#### **1.2 Atualização da entidade Message**
```java
// Adicionar campo derivado para facilitar queries
@Column(name = "is_read_by_agent")
private Boolean isReadByAgent = false; // Cache para otimização

// Método helper
public boolean isReadByUser(UUID userId) {
    return readStatuses.stream()
        .anyMatch(rs -> rs.getUserId().equals(userId));
}
```

#### **1.3 Repository methods**
```java
// MessageReadStatusRepository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, UUID> {
    
    // Marcar mensagem como lida
    @Modifying
    @Query(value = "INSERT INTO message_read_status (message_id, user_id, conversation_id, company_id) " +
                   "VALUES (:messageId, :userId, :conversationId, :companyId) " +
                   "ON CONFLICT (message_id, user_id) DO NOTHING", nativeQuery = true)
    void markAsRead(@Param("messageId") UUID messageId, 
                   @Param("userId") UUID userId,
                   @Param("conversationId") UUID conversationId,
                   @Param("companyId") UUID companyId);

    // Marcar todas as mensagens de uma conversa como lidas
    @Modifying
    @Query(value = "INSERT INTO message_read_status (message_id, user_id, conversation_id, company_id) " +
                   "SELECT m.id, :userId, m.conversation_id, :companyId " +
                   "FROM messages m " +
                   "WHERE m.conversation_id = :conversationId " +
                   "AND m.sender_type != 'AGENT' " + // Não marcar próprias mensagens
                   "AND NOT EXISTS (SELECT 1 FROM message_read_status mrs WHERE mrs.message_id = m.id AND mrs.user_id = :userId) " +
                   "ON CONFLICT (message_id, user_id) DO NOTHING", nativeQuery = true)
    void markAllAsReadInConversation(@Param("conversationId") UUID conversationId,
                                   @Param("userId") UUID userId,
                                   @Param("companyId") UUID companyId);

    // Contar mensagens não lidas por conversa
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.senderType != 'AGENT' " + // Não contar próprias mensagens
           "AND NOT EXISTS (SELECT mrs FROM MessageReadStatus mrs WHERE mrs.messageId = m.id AND mrs.userId = :userId)")
    long countUnreadInConversation(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    // Contar total de mensagens não lidas por usuário
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.company.id = :companyId " +
           "AND m.senderType != 'AGENT' " +
           "AND NOT EXISTS (SELECT mrs FROM MessageReadStatus mrs WHERE mrs.messageId = m.id AND mrs.userId = :userId)")
    long countTotalUnreadForUser(@Param("userId") UUID userId, @Param("companyId") UUID companyId);
}
```

### **2. Backend - Service Layer**

#### **2.1 MessageReadService**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class MessageReadService {
    
    private final MessageReadStatusRepository readStatusRepository;
    private final WebSocketNotificationService webSocketService;

    public void markAsRead(UUID messageId, UUID userId, UUID conversationId, UUID companyId) {
        readStatusRepository.markAsRead(messageId, userId, conversationId, companyId);
        
        // Notificar via WebSocket sobre mudança de status
        webSocketService.notifyUnreadCountChanged(conversationId, userId, 
            readStatusRepository.countUnreadInConversation(conversationId, userId));
    }

    public void markAllAsRead(UUID conversationId, UUID userId, UUID companyId) {
        readStatusRepository.markAllAsReadInConversation(conversationId, userId, companyId);
        
        // Notificar que não há mais mensagens não lidas
        webSocketService.notifyUnreadCountChanged(conversationId, userId, 0L);
    }

    public long getUnreadCount(UUID conversationId, UUID userId) {
        return readStatusRepository.countUnreadInConversation(conversationId, userId);
    }

    public Map<UUID, Long> getUnreadCountsByConversation(List<UUID> conversationIds, UUID userId) {
        return conversationIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> readStatusRepository.countUnreadInConversation(id, userId)
            ));
    }
}
```

#### **2.2 Atualização do ConversationService**
```java
// Modificar toDTO para incluir contagem real
private ConversationDTO toDTO(Conversation conversation, UUID currentUserId) {
    // ... código existente ...
    
    // Calcular contagem real de mensagens não lidas
    Long unreadCount = 0L;
    if (currentUserId != null) {
        unreadCount = messageReadService.getUnreadCount(conversation.getId(), currentUserId);
    }
    
    return ConversationDTO.builder()
        // ... outros campos ...
        .unreadCount(unreadCount)
        .build();
}
```

### **3. Backend - Controller & API Endpoints**

#### **3.1 Novos endpoints**
```java
@RestController
@RequestMapping("/api/messages")
public class MessageReadController {

    @PostMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID messageId, 
                                          @RequestParam UUID conversationId) {
        UUID currentUserId = getCurrentUserId();
        UUID companyId = getCurrentCompanyId();
        
        messageReadService.markAsRead(messageId, currentUserId, conversationId, companyId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversations/{conversationId}/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID conversationId) {
        UUID currentUserId = getCurrentUserId();
        UUID companyId = getCurrentCompanyId();
        
        messageReadService.markAllAsRead(conversationId, currentUserId, companyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations/{conversationId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable UUID conversationId) {
        UUID currentUserId = getCurrentUserId();
        Long count = messageReadService.getUnreadCount(conversationId, currentUserId);
        return ResponseEntity.ok(count);
    }
}
```

### **4. Frontend - State Management**

#### **4.1 Zustand Store para Unread Count**
```typescript
// unreadStore.ts
interface UnreadState {
  unreadCounts: Record<string, number>; // conversationId -> count
  totalUnread: number;
  
  setUnreadCount: (conversationId: string, count: number) => void;
  markAsRead: (conversationId: string, messageId?: string) => void;
  markAllAsRead: (conversationId: string) => void;
  incrementUnread: (conversationId: string) => void;
}

export const useUnreadStore = create<UnreadState>((set, get) => ({
  unreadCounts: {},
  totalUnread: 0,

  setUnreadCount: (conversationId, count) => set(state => {
    const newCounts = { ...state.unreadCounts, [conversationId]: count };
    const totalUnread = Object.values(newCounts).reduce((sum, c) => sum + c, 0);
    return { unreadCounts: newCounts, totalUnread };
  }),

  markAsRead: (conversationId, messageId) => {
    // Implementar lógica de marcar como lida
    // Chamar API e atualizar estado local
  },

  markAllAsRead: (conversationId) => {
    get().setUnreadCount(conversationId, 0);
    // Chamar API
  },

  incrementUnread: (conversationId) => {
    const current = get().unreadCounts[conversationId] || 0;
    get().setUnreadCount(conversationId, current + 1);
  }
}));
```

### **5. Frontend - UI Integration**

#### **5.1 Intersection Observer para auto-read**
```typescript
// hooks/useAutoMarkAsRead.ts
export const useAutoMarkAsRead = (conversationId: string) => {
  const observer = useRef<IntersectionObserver>();
  
  const observeMessage = useCallback((messageElement: HTMLElement, messageId: string) => {
    if (!observer.current) {
      observer.current = new IntersectionObserver(
        (entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              // Marcar como lida após 2 segundos de visualização
              setTimeout(() => {
                if (entry.isIntersecting) {
                  markMessageAsRead(messageId, conversationId);
                }
              }, 2000);
            }
          });
        },
        { threshold: 0.8 } // 80% da mensagem visível
      );
    }
    
    observer.current.observe(messageElement);
  }, [conversationId]);

  return { observeMessage };
};
```

#### **5.2 WebSocket handlers para unread updates**
```typescript
// Adicionar no useWebSocket.ts
const handleUnreadCountUpdate = useCallback((data: {
  conversationId: string;
  unreadCount: number;
}) => {
  const { setUnreadCount } = useUnreadStore.getState();
  setUnreadCount(data.conversationId, data.unreadCount);
}, []);

// Adicionar subscription
client.subscribe('/user/topic/unread-updates', handleUnreadCountUpdate);
```

## 🚀 Fases de Implementação

### **Fase 1: Backend Foundation (2-3 dias)**
1. ✅ Criar migration para tabela `message_read_status`
2. ✅ Implementar entidade `MessageReadStatus`
3. ✅ Criar `MessageReadStatusRepository` com queries otimizadas
4. ✅ Implementar `MessageReadService`
5. ✅ Atualizar `ConversationService.toDTO()` para incluir contagem real

### **Fase 2: API Endpoints (1 dia)**
1. ✅ Implementar `MessageReadController`
2. ✅ Adicionar endpoints para marcar como lida
3. ✅ Adicionar validações e tratamento de erros
4. ✅ Documentar APIs

### **Fase 3: Frontend State Management (1-2 dias)**
1. ✅ Criar `useUnreadStore` com Zustand
2. ✅ Implementar hooks para marcar mensagens como lidas
3. ✅ Atualizar componentes para usar novo store
4. ✅ Implementar auto-read com Intersection Observer

### **Fase 4: Real-time Updates (1 dia)**
1. ✅ Atualizar WebSocket handlers
2. ✅ Implementar notificações de mudança de unread count
3. ✅ Sincronizar estado entre abas/dispositivos

### **Fase 5: UI/UX Polish (1 dia)**
1. ✅ Adicionar indicadores visuais de mensagens não lidas
2. ✅ Implementar badge de contagem total
3. ✅ Adicionar animações e feedback visual
4. ✅ Implementar "Mark all as read" no header da conversa

## 🧪 Testes & Validação

### **Cenários de Teste**
1. **Contagem inicial**: Ao carregar página, contagens devem estar corretas
2. **Nova mensagem**: Contador deve incrementar em tempo real
3. **Marcar como lida**: Contador deve decrementar
4. **Múltiplas abas**: Sincronização entre abas abertas
5. **Refresh de página**: Persistência de estado
6. **Performance**: Queries otimizadas para grandes volumes

### **Métricas de Performance**
- Queries de contagem < 50ms
- Updates em tempo real < 100ms latência
- Suporte para 1000+ conversas simultâneas

## 📊 Considerações de Performance

1. **Índices otimizados**: Criação de índices compostos para queries frequentes
2. **Cache em memória**: Redis para contagens frequentemente acessadas
3. **Batch operations**: Agrupar marcações de leitura
4. **Lazy loading**: Carregar contagens sob demanda
5. **Debounce**: Evitar chamadas excessivas de API

## 🔄 Fluxo de Dados

### **Mensagem Nova Recebida**
1. WebSocket recebe nova mensagem
2. Frontend incrementa contador local
3. Backend registra mensagem como "não lida" para todos os agentes
4. Notificação em tempo real enviada para agentes conectados

### **Marcar como Lida**
1. Usuário visualiza mensagem (Intersection Observer)
2. API call para marcar como lida
3. Backend atualiza tabela `message_read_status`
4. WebSocket notifica mudança de contador
5. Frontend atualiza UI em tempo real

### **Sincronização Entre Abas**
1. Ação em uma aba (marcar como lida)
2. WebSocket notifica todas as abas abertas
3. Estado sincronizado automaticamente

## 📋 Checklist de Implementação

### Backend
- [ ] Migration da tabela `message_read_status`
- [ ] Entidade `MessageReadStatus`
- [ ] Repository com queries otimizadas
- [ ] Service layer (`MessageReadService`)
- [ ] Controller com endpoints REST
- [ ] WebSocket notifications
- [ ] Atualização do `ConversationService`

### Frontend
- [ ] Store Zustand para unread counts
- [ ] API client para endpoints de leitura
- [ ] Hook `useAutoMarkAsRead`
- [ ] Integração com WebSocket
- [ ] Componentes UI atualizados
- [ ] Badges de contagem
- [ ] Botão "Mark all as read"

### Testes
- [ ] Testes unitários para service layer
- [ ] Testes de integração para APIs
- [ ] Testes E2E para fluxo completo
- [ ] Testes de performance
- [ ] Testes de sincronização WebSocket

Este plano garante uma implementação robusta, performática e escalável do sistema de contagem de mensagens não lidas! 🎯