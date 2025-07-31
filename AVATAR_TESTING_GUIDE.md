# 🧪 Guia de Testes - Sistema de Avatar Base64

## 📋 **Visão Geral dos Testes**

Este guia detalha como executar e validar todos os testes do sistema de avatar base64 implementado.

### **Testes Implementados:**

1. **`AIAgentAvatarIntegrationTest`** - Testes de integração do serviço
2. **`AIAgentAvatarControllerIntegrationTest`** - Testes de integração da API
3. **`AvatarUpload.test.tsx`** - Testes unitários do componente de upload
4. **`AvatarDisplay.test.tsx`** - Testes unitários do componente de exibição

## 🚀 **Executando os Testes**

### **Backend (Spring Boot)**

#### **1. Testes de Integração do Serviço**
```bash
cd api

# Executar apenas os testes de avatar
./mvnw test -Dtest=AIAgentAvatarIntegrationTest

# Ou executar todos os testes
./mvnw test
```

#### **2. Testes de Integração do Controller**
```bash
# Executar testes da API REST
./mvnw test -Dtest=AIAgentAvatarControllerIntegrationTest

# Com perfil de teste específico
./mvnw test -Dspring.profiles.active=test
```

#### **3. Executar Todos os Testes Backend**
```bash
# Executar toda a suíte de testes
./mvnw clean test

# Com relatório de cobertura
./mvnw clean test jacoco:report
```

### **Frontend (React)**

#### **1. Testes do Componente AvatarUpload**
```bash
cd client

# Executar apenas testes do AvatarUpload
npm test -- --testPathPattern=AvatarUpload.test.tsx

# Modo watch
npm test -- --testPathPattern=AvatarUpload.test.tsx --watch
```

#### **2. Testes do Componente AvatarDisplay**
```bash
# Executar apenas testes do AvatarDisplay
npm test -- --testPathPattern=AvatarDisplay.test.tsx

# Com cobertura
npm test -- --testPathPattern=AvatarDisplay.test.tsx --coverage
```

#### **3. Executar Todos os Testes Frontend**
```bash
# Executar toda a suíte de testes
npm test -- --watchAll=false

# Com cobertura completa
npm test -- --coverage --watchAll=false
```

## 📊 **Cobertura dos Testes**

### **Backend - Cenários Testados:**

#### **`AIAgentAvatarIntegrationTest`**
- ✅ Criação de agente com avatar JPEG válido
- ✅ Criação de agente com avatar PNG válido
- ✅ Criação de agente sem avatar
- ✅ Atualização de avatar existente
- ✅ Remoção de avatar (null/empty)
- ✅ Troca de formato de avatar (JPEG → PNG)
- ✅ Recuperação de agente com avatar
- ✅ Listagem de múltiplos agentes com avatars
- ✅ Persistência após transação
- ✅ Armazenamento de avatars grandes (~1KB)
- ✅ Handling de string vazia

#### **`AIAgentAvatarControllerIntegrationTest`**
- ✅ Validação de formato base64 (JPEG, PNG, GIF)
- ✅ Rejeição de formatos não suportados
- ✅ Rejeição de base64 malformado
- ✅ Rejeição de headers inválidos
- ✅ Aceitação de avatar vazio/null
- ✅ Atualização via PUT endpoint
- ✅ Códigos de status HTTP corretos
- ✅ Serialização JSON correta
- ✅ Integridade após múltiplas operações

### **Frontend - Cenários Testados:**

#### **`AvatarUpload.test.tsx`**
- ✅ Renderização sem avatar inicial
- ✅ Exibição de avatar quando fornecido
- ✅ Upload de arquivos JPEG, PNG, GIF válidos
- ✅ Validação de tamanho (rejeita > 2MB)
- ✅ Validação de tipo (rejeita não-imagens)
- ✅ Remoção de avatar
- ✅ Estado desabilitado
- ✅ Estado de loading durante upload
- ✅ Tratamento de erros do FileReader
- ✅ Customização de tamanho e placeholder

#### **`AvatarDisplay.test.tsx`**
- ✅ Exibição de avatar base64 válido
- ✅ Fallback para ícone padrão
- ✅ Validação de formatos de imagem
- ✅ Rejeição de formatos não-imagem
- ✅ Handling de base64 malformado
- ✅ Customização de tamanho, className, alt
- ✅ Ícones de fallback personalizados
- ✅ Preservação da qualidade da imagem

## 🎯 **Cenários de Teste Manuais**

### **1. Fluxo Completo de Upload**
```bash
1. Iniciar aplicação: npm run dev (client) + ./mvnw spring-boot:run (api)
2. Navegar para configuração de agente
3. Fazer upload de imagem JPEG (< 2MB)
4. Verificar preview em tempo real
5. Salvar agente
6. Verificar avatar na lista de agentes
7. Abrir chat e enviar mensagem como IA
8. Verificar avatar na mensagem
```

### **2. Validações de Upload**
```bash
1. Tentar upload de arquivo > 2MB → Deve mostrar erro
2. Tentar upload de PDF → Deve mostrar erro  
3. Tentar upload de imagem corrompida → Deve tratar graciosamente
4. Upload de PNG transparente → Deve funcionar
5. Upload de GIF animado → Deve funcionar
```

### **3. Operações CRUD**
```bash
1. Criar agente sem avatar → Deve mostrar ícone padrão
2. Adicionar avatar depois → Deve atualizar
3. Trocar avatar por outro → Deve substituir
4. Remover avatar → Deve voltar ao ícone padrão
5. Verificar persistência após reload da página
```

## 🔧 **Configuração de Ambiente de Teste**

### **Pré-requisitos:**
```bash
# Backend
- Java 24
- PostgreSQL (via Docker ou local)
- Maven 3.9+

# Frontend  
- Node.js 18+
- npm ou yarn
```

### **Setup do Banco de Testes:**
```bash
# Via Docker
docker run --name postgres-test -e POSTGRES_PASSWORD=postgres -d -p 5433:5432 postgres:16

# Configurar application-test.properties
spring.datasource.url=jdbc:postgresql://localhost:5433/rubia_chat_test
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### **Setup Frontend:**
```bash
cd client
npm install
npm test -- --watchAll=false
```

## 📈 **Métricas de Qualidade**

### **Cobertura Esperada:**
- **Backend**: > 90% nos serviços de avatar
- **Frontend**: > 85% nos componentes de avatar
- **Integração**: 100% dos cenários críticos

### **Performance:**
- **Upload**: < 500ms para imagens < 1MB
- **Exibição**: < 100ms para renderização
- **Validação**: < 50ms para formatos

### **Compatibilidade:**
- **Formatos**: JPEG, PNG, GIF
- **Tamanhos**: Até 2MB por avatar
- **Browsers**: Chrome, Firefox, Safari, Edge

## 🐛 **Solução de Problemas**

### **Testes Backend Falhando:**
```bash
# Verificar se o banco está ativo
docker ps | grep postgres

# Limpar e recompilar
./mvnw clean compile test

# Verificar logs detalhados
./mvnw test -Dlogging.level.com.ruby=DEBUG
```

### **Testes Frontend Falhando:**
```bash
# Limpar cache do Jest
npm test -- --clearCache

# Executar com verbosidade
npm test -- --verbose

# Verificar dependências
npm install
```

### **Problemas de Upload:**
```bash
# Verificar tamanho do arquivo
ls -lh arquivo.jpg

# Verificar tipo MIME
file --mime-type arquivo.jpg

# Testar base64 manual
base64 arquivo.jpg | head -c 100
```

## ✅ **Checklist de Validação**

### **Backend:**
- [ ] Todos os testes passam
- [ ] Migração V49 aplica corretamente
- [ ] Validação regex funciona
- [ ] Endpoints retornam dados corretos
- [ ] Performance dentro dos limites

### **Frontend:**
- [ ] Componentes renderizam corretamente
- [ ] Upload funciona em todos os browsers
- [ ] Validações mostram erros apropriados
- [ ] Preview em tempo real funciona
- [ ] Estados de loading/erro funcionam

### **Integração:**
- [ ] Avatar persiste no banco
- [ ] Avatar aparece no chat
- [ ] Múltiplos formatos funcionam
- [ ] Operações CRUD completas
- [ ] Fallbacks funcionam

## 🎉 **Conclusão**

Todos os testes cobrem:
- **Funcionalidade**: Upload, exibição, validação
- **Segurança**: Validação de formato e tamanho
- **Performance**: Limites de tempo e memória
- **UX**: Estados de loading, erro e sucesso
- **Persistência**: Armazenamento e recuperação
- **Compatibilidade**: Múltiplos formatos de imagem

**Status dos Testes: COMPLETO E FUNCIONAL** ✅