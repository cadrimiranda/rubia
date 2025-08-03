# Configurações de Campanha WHAPI - Sistema Rubia

## ⚙️ Configurações Implementadas

### Delays Entre Mensagens
- **Modo Conservador (Padrão)**: 30-60 segundos randomizado
- **Modo Experiente**: 5-10 segundos randomizado
- **Atual**: Sistema usa modo conservador por segurança

### Lotes de Envio
- **Tamanho do Lote**: 20 mensagens
- **Pausa Entre Lotes**: 60 minutos
- **Motivo**: Evita detecção como spam pelo WhatsApp

### Limites Diários
- **Máximo Recomendado**: 6 horas de envio por dia
- **Status**: Não implementado ainda (TODO)

## 📊 Cálculo de Tempo Estimado

### Para 100 Contatos (Modo Conservador):
- **5 lotes** de 20 mensagens cada
- **Tempo por lote**: ~17-33 minutos
- **Pausas entre lotes**: 4 x 60 minutos = 240 minutos
- **Tempo total**: ~5-6 horas

### Para 100 Contatos (Modo Experiente):
- **5 lotes** de 20 mensagens cada  
- **Tempo por lote**: ~2-3 minutos
- **Pausas entre lotes**: 4 x 60 minutos = 240 minutos
- **Tempo total**: ~4-4.5 horas

## 🛡️ Medidas de Proteção Implementadas

### 1. Intervalos Randomizados
```java
// Evita padrões detectáveis pelo anti-spam
int randomDelay = minDelay + (int)(Math.random() * (maxDelay - minDelay));
```

### 2. Processamento em Lotes
```java
// Pausa obrigatória a cada 20 mensagens
if (currentBatchCount >= BATCH_SIZE) {
    Thread.sleep(BATCH_PAUSE_MINUTES * 60 * 1000);
}
```

### 3. Logs Detalhados
- Rastreamento de sucessos/falhas
- Tempo de pausa entre lotes
- Estatísticas da campanha

## 🎯 Boas Práticas Seguidas

### ✅ Implementado
- [x] Delays randomizados entre mensagens
- [x] Pausas obrigatórias entre lotes
- [x] Processamento assíncrono
- [x] Controle de erros robusto
- [x] Logs para auditoria
- [x] Status tracking por contato

### ⏳ Próximos Passos
- [ ] Controle de 6 horas máximas por dia
- [ ] Integração com módulo de aquecimento WHAPI
- [ ] Configuração de proxy residencial
- [ ] Monitoramento de taxa de resposta
- [ ] Auto-pausa em caso de baixo engajamento

## 🔧 Como Ajustar Configurações

### No CampaignMessagingService.java:
```java
// Ajustar tamanho dos lotes (padrão: 20)
private static final int BATCH_SIZE = 20;

// Ajustar pausa entre lotes (padrão: 60 min)
private static final int BATCH_PAUSE_MINUTES = 60;

// Ajustar delays (modo conservador)
private static final int CONSERVATIVE_MIN_DELAY = 30000; // 30s
private static final int CONSERVATIVE_MAX_DELAY = 60000; // 60s
```

### Trocar para Modo Experiente:
```java
// Na linha do getRandomDelay(), trocar true por false
int randomDelay = getRandomDelay(false); // modo experiente
```

## 📈 Monitoramento Recomendado

### Métricas WHAPI Críticas:
1. **Taxa de Resposta**: Meta >30%
2. **Taxa de Entrega**: Meta >95%
3. **Reclamações**: Meta = 0
4. **Bloqueios**: Meta = 0

### Ações Automáticas:
- **Taxa resposta <30%**: Pausar campanha
- **Primeira reclamação**: Pausar imediatamente
- **Mensagens não entregues**: Investigar número

## 🚨 Sinais de Alerta

### Pare Imediatamente Se:
- Taxa de resposta < 10%
- Múltiplas reclamações
- Mensagens não sendo entregues
- Números começando a bloquear

### Recuperação:
1. Pausar por 24-48h
2. Analisar o que deu errado
3. Ajustar estratégia
4. Retomar com volume menor

---

**Baseado em**: Documentação oficial WHAPI para prevenção de ban
**Atualizado em**: Janeiro 2025