# Plano de Testes - Sistema de Campanhas WhatsApp

## 📋 Contexto

Com base na resolução do problema de Thread Pool Exhaustion documentado em MEMORY.md, este documento define os testes necessários para validar a arquitetura CompletableFuture + Semaphore implementada.

---

## 🧪 Categorias de Testes

### 1. **Testes de Unidade - CampaignMessagingService**

#### 1.1 Teste de Exponential Backoff com Jitter
```java
@Test
void shouldCalculateRetryDelayWithJitter() {
    // Validar que delay aumenta exponencialmente
    // Verificar que jitter está dentro do range esperado
    // Testar limite máximo de 30s
}
```

#### 1.2 Teste de CompletableFuture Chains
```java
@Test
void shouldHandleAsyncMessageSending() {
    // Mockar performActualSend()
    // Validar chain de CompletableFuture
    // Verificar tratamento de exceções
}
```

#### 1.3 Teste de Retry Logic
```java
@Test
void shouldRetryFailedMessagesWithBackoff() {
    // Simular falhas consecutivas
    // Validar número máximo de tentativas
    // Verificar delays entre tentativas
}
```

#### 1.4 Teste de Validação de Contatos
```java
@Test
void shouldValidateContactsBeforeSending() {
    // Contatos inválidos devem retornar false
    // Validar campos obrigatórios
    // Testar números de telefone malformados
}
```

### 2. **Testes de Integração - Redis + CompletableFuture**

#### 2.1 Teste de Reprocessamento via Redis
```java
@Test
@Transactional
void shouldReAddFailedMessagesToRedisQueue() {
    // Simular falha de envio
    // Verificar item adicionado ao Redis
    // Validar delay scheduling
}
```

#### 2.2 Teste de Processamento Assíncrono
```java
@Test
void shouldProcessMultipleCampaignsAsynchronously() {
    // Criar múltiplas campanhas
    // Verificar processamento paralelo
    // Validar independência entre campanhas
}
```

#### 2.3 Teste de DelaySchedulingService
```java
@Test
void shouldScheduleTasksWithCorrectDelay() {
    // Mockar ScheduledExecutor
    // Validar agendamento com delay
    // Verificar execução do callback
}
```

### 3. **Testes de Performance**

#### 3.1 Load Test - 50 Mensagens Simultâneas
```java
@Test
void shouldHandle50ConcurrentMessages() {
    // Criar 50 CampaignContact
    // Enviar simultaneamente
    // Validar que todas são processadas
    // Medir tempo total vs Thread.sleep original
}
```

#### 3.2 Teste de Throughput com Semaphore
```java
@Test
void shouldRespectConcurrencyLimitsWithSemaphore() {
    // Criar 100 mensagens
    // Verificar máximo 50 simultâneas
    // Validar que outras aguardam na fila
}
```

#### 3.3 Teste de Não-Bloqueio de Threads
```java
@Test
void shouldNotBlockThreadsOnRetry() {
    // Simular retry com delay longo
    // Verificar que thread é liberada imediatamente
    // Validar que outras mensagens continuam processando
}
```

### 4. **Testes de Resiliência**

#### 4.1 Teste de Recuperação de Mensagens Travadas
```java
@Test
void shouldRecoverStuckMessages() {
    // Simular mensagem "travada" no Redis
    // Aguardar timeout de recuperação
    // Verificar reprocessamento automático
}
```

#### 4.2 Teste de Graceful Shutdown
```java
@Test
void shouldHandleGracefulShutdownWithActiveTasks() {
    // Iniciar processamento de mensagens
    // Chamar @PreDestroy shutdown()
    // Verificar que tarefas ativas são aguardadas
    // Validar que mensagens são preservadas na fila
}
```

#### 4.3 Teste de Backpressure
```java
@Test
void shouldHandleBackpressureUnderHighLoad() {
    // Saturar sistema com mais de 50 mensagens
    // Verificar devolução de itens à fila
    // Validar logs de sistema sobrecarregado
}
```

#### 4.4 Teste de Falhas de API Externa
```java
@Test
void shouldHandleWhatsAppApiFailures() {
    // Mockar falhas da ZAPI
    // Verificar retry automático
    // Testar diferentes tipos de erro (timeout, 500, rate limit)
}
```

### 5. **Testes de Configuração**

#### 5.1 Teste de Configuração de Semaphores
```java
@Test
void shouldConfigureSemaphoresCorrectly() {
    // Verificar concurrencyLimiter com 50 permits
    // Validar queueProcessingLimiter com 10 permits
    // Testar comportamento com permits esgotados
}
```

#### 5.2 Teste de ScheduledExecutor
```java
@Test
void shouldConfigureScheduledExecutorWith4Threads() {
    // Verificar pool size = 4
    // Testar removeOnCancelPolicy = true
    // Validar agendamento de múltiplas tarefas
}
```

#### 5.3 Teste de Properties de Retry
```java
@Test
void shouldReadCampaignPropertiesCorrectly() {
    // Verificar maxRetries
    // Validar retryDelayMs
    // Testar limites configurados
}
```

---

## 🎯 Testes Críticos (Prioridade Alta)

### Cenário 1: Thread Pool Exhaustion (Problema Original)
```java
@Test
void shouldNotExhaustThreadPoolOnRetries() {
    // Simular 4 contatos com falhas
    // Verificar que todos 4 são processados
    // Validar tempo < 180s
    // Confirmar que nenhuma thread fica bloqueada
}
```

### Cenário 2: Processamento de Fila Contínuo
```java
@Test
void shouldProcessQueueContinuouslyWithoutBlocking() {
    // Adicionar itens continuamente à fila
    // Verificar processamento sem travamento
    // Validar que sistema não para após X mensagens
}
```

### Cenário 3: Recuperação de Falhas
```java
@Test
void shouldRecoverFromSystemFailures() {
    // Simular crash durante processamento
    // Reiniciar sistema
    // Verificar reprocessamento de mensagens pendentes
}
```

---

## 📊 Métricas de Validação

### Performance Targets
- **Throughput**: > 50 mensagens/minuto
- **Latência**: < 5s por mensagem (sem retry)
- **Concorrência**: 50 mensagens simultâneas
- **Recuperação**: < 60s para mensagens travadas

### Reliability Targets
- **Taxa de sucesso**: > 95% (considerando retries)
- **Zero thread blocking**: Confirmado via profiling
- **Graceful shutdown**: < 45s para finalizar tarefas ativas
- **Backpressure**: Sistema não deve travar sob sobrecarga

---

## 🔧 Ferramentas de Teste

### Framework de Testes
- **JUnit 5**: Testes unitários e integração
- **Mockito**: Mock de dependências externas
- **TestContainers**: Redis/PostgreSQL para testes
- **Spring Boot Test**: Context loading e transações

### Testes de Performance
- **JMeter**: Load testing da API
- **Micrometer**: Métricas em tempo real
- **VisualVM/JProfiler**: Profiling de threads

### Monitoramento
- **Actuator**: Health checks e métricas
- **Prometheus**: Coleta de métricas
- **Grafana**: Dashboard de monitoring (opcional)

---

## 🚀 Plano de Execução

### Fase 1: Testes Fundamentais (1-2 dias)
1. Testes de unidade do CampaignMessagingService
2. Validação de CompletableFuture chains
3. Teste do problema original (Thread Pool Exhaustion)

### Fase 2: Testes de Integração (2-3 dias)
1. Redis + DelaySchedulingService
2. Processamento de fila completo
3. Recuperação automática de mensagens

### Fase 3: Testes de Performance (1-2 dias)
1. Load testing com 50+ mensagens
2. Validação de throughput e latência
3. Testes de backpressure

### Fase 4: Testes de Resiliência (1-2 dias)
1. Graceful shutdown
2. Falhas de API externa
3. Cenários de crash e recuperação

---

## ✅ Critérios de Aceitação

### Must Have
- [ ] Zero thread blocking confirmado
- [ ] Processamento de 50+ mensagens simultâneas
- [ ] Recuperação automática funcional
- [ ] Graceful shutdown implementado
- [ ] Taxa de sucesso > 95%

### Should Have
- [ ] Métricas de monitoring ativas
- [ ] Load test com 100+ mensagens
- [ ] Dashboard de acompanhamento
- [ ] Documentação de troubleshooting

### Could Have
- [ ] Integração com Grafana
- [ ] Alertas automáticos
- [ ] Testes de stress extremo
- [ ] Benchmarks comparativos