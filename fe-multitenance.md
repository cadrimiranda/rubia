# 📋 Planejamento Multi-Tenant Frontend

## 🔴 **Fase 1: Foundation (Crítico)**

### **MT-1: Create Company Types** 
- Criar interfaces `Company`, `CompanyContext`
- Definir tipos para multi-tenancy
- **Arquivo**: `types/index.ts`

### **MT-2: Update API DTOs**
- Adicionar `companyId` em todos DTOs
- Adicionar `CompanyDTO` interface  
- **Arquivos**: `api/types.ts`

### **MT-3: Update AuthUser Interface**
- Adicionar company info no `AuthUser`
- Incluir `companyId` e `company` object
- **Arquivo**: `auth/authService.ts`

## 🟠 **Fase 2: Core Infrastructure (Alto)**

### **MT-4: API Client Headers**
- Implementar `X-Company-Id` header
- Validação automática de company
- **Arquivo**: `api/client.ts`

### **MT-5: Auth Service Company Context**
- Login/logout com company handling
- Persistir company no localStorage
- **Arquivo**: `auth/authService.ts`

### **MT-6: Auth Store Company Management**
- Company state no auth store
- Métodos para company context
- **Arquivo**: `store/useAuthStore.ts`

## 🟡 **Fase 3: Data Layer (Médio)**

### **MT-7: Update Adapters**
- Mapear company fields nos adapters
- Validação de company nos DTOs
- **Arquivos**: `adapters/*.ts`

### **MT-8: Chat Store Validation**
- Company validation em todas operações
- Isolamento de dados por tenant
- **Arquivo**: `store/useChatStore.ts`

### **MT-9: WebSocket Company Context**
- Company context na conexão WebSocket
- Filtragem por tenant nos eventos
- **Arquivo**: `websocket/client.ts`

### **MT-10: API Services Validation**
- Validação de company em todos services
- Error handling para tenant mismatch
- **Arquivos**: `api/services/*.ts`

## 🟢 **Fase 4: Polish & Testing (Baixo)**

### **MT-11: Frontend Types Alignment**
- Alinhar tipos frontend com backend entities
- Consistência nos nomes de campos

### **MT-12: Error Handling**
- Tratamento de erros de company validation
- Messages de erro específicos para multi-tenant

### **MT-13: Testing**
- Testes de isolamento de dados
- Validação de segurança multi-tenant

---

## 📊 **Ordem de Execução Recomendada**

```
MT-1 → MT-2 → MT-3 → MT-4 → MT-5 → MT-6
  ↓
MT-7 → MT-8 → MT-9 → MT-10
  ↓  
MT-11 → MT-12 → MT-13
```

## ⚠️ **Considerações Importantes**

- **Quebra de Compatibilidade**: Algumas mudanças vão quebrar API calls existentes
- **Testagem**: Testar com múltiplas companies após cada fase
- **Rollback**: Manter backup antes de cada fase crítica
- **Coordenação**: Backend deve estar preparado para receber headers de company

## 🔍 **Análise de Gaps Identificados**

### **Missing Multi-Tenant Components**

#### 1. **Company/Tenant Context Missing**
- **Problema**: No company identification or context management in the frontend
- **Backend**: All entities have `company_id` (departments, users, customers, conversations, messages)
- **Frontend**: No company ID handling anywhere in the codebase

#### 2. **API Client Configuration**
**File**: `/Users/carlosadrianomiranda/rubia/client/src/api/client.ts`
- **Missing**: No company headers (`X-Company-Id` or similar) sent with requests
- **Missing**: No tenant-specific base URL handling
- **Current**: Only sends `Authorization: Bearer {token}` header

#### 3. **Authentication Service Gaps**
**File**: `/Users/carlosadrianomiranda/rubia/client/src/auth/authService.ts`
- **Missing**: No company information in `AuthUser` interface
- **Missing**: No company context after login
- **Backend expects**: User belongs to a company, but frontend doesn't track this

#### 4. **State Management Lacks Tenant Isolation**
**Files**: 
- `/Users/carlosadrianomiranda/rubia/client/src/store/useAuthStore.ts`
- `/Users/carlosadrianomiranda/rubia/client/src/store/useChatStore.ts`

**Missing**:
- No company context in auth store
- No tenant-specific data isolation
- No company validation in API calls

#### 5. **Data Types Missing Company Fields**
**File**: `/Users/carlosadrianomiranda/rubia/client/src/api/types.ts`
- **Missing**: No `companyId` field in any DTOs
- **Backend**: All DTOs should include company information
- **Missing**: No company-scoped filtering in API requests

#### 6. **Adapters Don't Handle Company Data**
**Files**:
- `/Users/carlosadrianomiranda/rubia/client/src/adapters/conversationAdapter.ts`  
- `/Users/carlosadrianomiranda/rubia/client/src/adapters/customerAdapter.ts`

**Missing**:
- No company field mapping from backend DTOs
- No company validation in data transformation

#### 7. **WebSocket Missing Tenant Context**
**File**: `/Users/carlosadrianomiranda/rubia/client/src/websocket/client.ts`
- **Missing**: No company context in WebSocket connection
- **Security Risk**: Could receive data from other tenants

## 🛡️ **Security Implications**

### **Critical Security Gaps**:
1. **Data Leakage**: Frontend could receive/display data from other companies
2. **Unauthorized Access**: No tenant boundary enforcement in frontend
3. **WebSocket Security**: No company isolation in real-time communications
4. **Cache Poisoning**: Shared cache could mix data between tenants

## 🎯 **Priority Implementation Order**

1. **High Priority**: Update API types and client to include company context
2. **High Priority**: Update authentication to handle company information  
3. **Medium Priority**: Update state management for tenant isolation
4. **Medium Priority**: Update adapters to handle company fields
5. **Low Priority**: Update WebSocket for tenant-specific connections

---

**Status**: O frontend atualmente **não está preparado** para arquitetura multi-tenant e teria problemas significativos de segurança e isolamento de dados em um ambiente multi-tenant.