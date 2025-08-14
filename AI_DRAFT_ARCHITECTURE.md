# Arquitetura: Sistema de Mensagens Automáticas em DRAFT

## Pergunta Original
Como fazer com que meu agente IA possa criar mensagens automáticas em DRAFT no sistema de chat corporativo Rubia, permitindo que operadores revisem e aprovem antes do envio?

## Sistema Atual (Rubia)
- **Frontend**: React 19 + TypeScript + Ant Design + Zustand
- **Backend**: Spring Boot 3.5 + Java 24 + PostgreSQL + Redis + RabbitMQ + WebSocket
- **Fluxo atual**: entrada → esperando → finalizados
- **Estado gerenciado**: Zustand store (useChatStore.ts)

## Arquitetura Proposta

### 1. Estrutura de Dados

#### MessageDraft (Nova Entidade)
```java
@Entity
public class MessageDraft {
    private Long id;
    private Long conversationId;
    private String content;
    private String aiModel; // "openai-gpt4", "claude-3"
    private Double confidence; // 0.0 - 1.0
    private DraftStatus status; // PENDING, APPROVED, REJECTED, EDITED
    private LocalDateTime createdAt;
    private String createdBy; // "AI_AGENT"
    private String reviewedBy; // operador que revisou
}
```

#### Enum DraftStatus
```java
public enum DraftStatus {
    PENDING,    // Aguardando revisão
    APPROVED,   // Aprovado para envio
    REJECTED,   // Rejeitado pelo operador
    EDITED      // Editado pelo operador
}
```

### 2. Componentes Backend

#### AIDraftService.java
```java
@Service
public class AIDraftService {
    
    // Processa nova mensagem e gera draft
    public MessageDraft generateDraftResponse(Long conversationId, String userMessage);
    
    // Analisa contexto da conversa
    private String buildConversationContext(Long conversationId);
    
    // Determina se deve gerar draft automático
    private boolean shouldGenerateDraft(Conversation conversation);
    
    // Processa aprovação de draft
    public Message approveDraft(Long draftId, String operatorId);
}
```

#### OpenAIService.java
```java
@Service
public class OpenAIService {
    
    // Chama API OpenAI/Claude
    public AIResponse generateResponse(String context, String prompt);
    
    // Configura prompts específicos por tipo de conversa
    private String buildSystemPrompt(ConversationType type);
}
```

#### DraftController.java
```java
@RestController
@RequestMapping("/api/drafts")
public class DraftController {
    
    @GetMapping("/conversation/{conversationId}")
    public List<MessageDraft> getDraftsByConversation(Long conversationId);
    
    @PostMapping("/{draftId}/approve")
    public ResponseEntity<Message> approveDraft(Long draftId);
    
    @PostMapping("/{draftId}/reject")
    public ResponseEntity<Void> rejectDraft(Long draftId);
    
    @PutMapping("/{draftId}/edit")
    public ResponseEntity<MessageDraft> editDraft(Long draftId, String newContent);
}
```

### 3. Fluxo de Execução

#### Trigger Event (RabbitMQ)
1. **Nova mensagem cliente** → `message.received` event
2. **AIDraftListener** processa evento
3. **Análise de contexto** da conversa
4. **Decisão**: Gerar draft ou não
5. **Chamada IA** (OpenAI/Claude)
6. **Salvar draft** no PostgreSQL
7. **WebSocket notification** para operadores

#### Fluxo Detalhado
```
Cliente envia mensagem
       ↓
[RabbitMQ] message.received
       ↓
AIDraftService.generateDraftResponse()
       ↓
Analisa histórico da conversa (PostgreSQL)
       ↓
Monta contexto + prompt para IA
       ↓
OpenAIService.generateResponse()
       ↓
Salva MessageDraft (status: PENDING)
       ↓
WebSocket: notifica operadores
       ↓
Frontend: mostra draft panel
       ↓
Operador: aprova/edita/rejeita
       ↓
Se aprovado: envia mensagem real
```

### 4. Componentes Frontend

#### DraftPanel.tsx
```tsx
interface DraftPanelProps {
  conversationId: number;
  drafts: MessageDraft[];
}

// Painel lateral mostrando drafts pendentes
// Botões: Aprovar, Editar, Rejeitar
// Indicador de confiança da IA
```

#### DraftIndicator.tsx
```tsx
// Ícone/badge na lista de conversas
// Mostra quantos drafts pendentes existem
// Cor baseada na urgência/confiança
```

#### Zustand Store Updates
```typescript
interface ChatStore {
  // Estado existente...
  drafts: Record<number, MessageDraft[]>; // por conversationId
  
  // Ações
  addDraft: (draft: MessageDraft) => void;
  approveDraft: (draftId: number) => void;
  rejectDraft: (draftId: number) => void;
  editDraft: (draftId: number, content: string) => void;
}
```

### 5. Configurações e Triggers

#### Quando Gerar Drafts
- Primeira mensagem do cliente (boas-vindas)
- Perguntas frequentes detectadas
- Conversas sem resposta há X minutos
- Palavras-chave específicas do domínio
- Conversas em status "entrada" há mais de Y tempo

#### Configuração IA
```yaml
# application.yml
ai:
  enabled: true
  provider: openai # ou claude
  model: gpt-4-turbo
  auto-draft:
    confidence-threshold: 0.7
    max-drafts-per-conversation: 3
    timeout-minutes: 5
```

### 6. Banco de Dados (Migrations)

#### V10__create_message_drafts.sql
```sql
CREATE TABLE message_drafts (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    ai_model VARCHAR(50) NOT NULL,
    confidence DECIMAL(3,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL,
    reviewed_by VARCHAR(50),
    reviewed_at TIMESTAMP,
    
    FOREIGN KEY (conversation_id) REFERENCES conversations(id)
);

CREATE INDEX idx_message_drafts_conversation ON message_drafts(conversation_id);
CREATE INDEX idx_message_drafts_status ON message_drafts(status);
```

### 7. Integração com Estado Atual

#### Modificações no ChatStore
- Adicionar drafts ao estado global
- WebSocket listeners para draft events
- Métodos para gerenciar draft lifecycle

#### UI/UX Improvements
- Badge de draft na lista de conversas
- Painel lateral para drafts pendentes
- Botão toggle para habilitar/desabilitar IA
- Histórico de drafts aprovados/rejeitados

### 8. Considerações de Segurança

- Sanitização de conteúdo gerado pela IA
- Rate limiting para chamadas de IA
- Logs de audit para ações de draft
- Permissões específicas para operadores

### 9. Monitoramento

- Métricas Prometheus: drafts gerados/aprovados/rejeitados
- Tempo médio de resposta da IA
- Taxa de aprovação por modelo
- Performance do sistema com IA ativa

## Benefícios da Solução

1. **Eficiência**: Reduz tempo de resposta inicial
2. **Qualidade**: Operador sempre revisa antes do envio
3. **Escalabilidade**: IA aprende com padrões aprovados
4. **Flexibilidade**: Configurável por tipo de conversa
5. **Auditoria**: Histórico completo de decisões

## Próximos Passos

1. Implementar entities e repositories
2. Configurar integração OpenAI/Claude
3. Criar componentes React para drafts
4. Implementar WebSocket events
5. Testes e ajustes de prompts
6. Deploy e monitoramento