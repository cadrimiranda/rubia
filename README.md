# Rubia - Chat Corporativo com IA

Uma aplicação de chat corporativo moderna com funcionalidades de chatbot alimentado por IA, desenvolvida para facilitar a comunicação entre equipes e clientes.

## 🏗️ Arquitetura

O projeto é dividido em duas partes principais:

- **Frontend** (`/client`): Aplicação React com TypeScript e comunicação em tempo real
- **Backend** (`/api`): API REST com Spring Boot e WebSocket

### Fluxo de Dados Frontend
```
UI Components → Zustand Store → API Services → Backend
     ↑              ↓
WebSocket Client → Event Handlers → Store Updates
```

### Padrões Arquiteturais
- **Single Source of Truth**: Zustand store centraliza todo estado
- **Adapter Pattern**: Transformação de dados entre frontend e backend  
- **Observer Pattern**: WebSocket eventos e notificações
- **Error Boundaries**: Tratamento de erros em múltiplos níveis

## 🚀 Tecnologias

### Frontend
- **React 19** + TypeScript - Framework frontend moderno
- **Vite** - Build tool e dev server rápido
- **Ant Design 5.25+** - Biblioteca de componentes UI
- **Tailwind CSS** - Framework CSS utilitário
- **Zustand** - Gerenciamento de estado simples e eficiente
- **Lucide React** - Ícones modernos e consistentes
- **WebSocket** - Comunicação bidirecional em tempo real
- **Custom Event System** - Sistema de eventos personalizados

### Backend
- Spring Boot 3.5
- Java 24
- PostgreSQL (banco de dados)
- Redis (cache)
- RabbitMQ (messaging)
- WebSocket (comunicação em tempo real)
- Flyway (migrations)
- Spring Security (autenticação)
- Actuator + Prometheus (monitoramento)

## 🛠️ Pré-requisitos

- Node.js 18+
- Java 24
- Maven 3.8+
- PostgreSQL 13+
- Redis 6+
- RabbitMQ 3.8+

## 📦 Instalação

### 1. Clone o repositório
```bash
git clone <repository-url>
cd rubia
```

### 2. Configure o Frontend
```bash
cd client
npm install
```

### 3. Configure o Backend
```bash
cd api
./mvnw clean install
```

### 4. Configure o banco de dados
Crie um banco PostgreSQL e configure as credenciais no arquivo `api/src/main/resources/application.properties`.

## 🚀 Executando o projeto

### Frontend (Desenvolvimento)
```bash
cd client
npm run dev
```
Acesse: http://localhost:5173

### Backend (Desenvolvimento)
```bash
cd api
./mvnw spring-boot:run
```
API disponível em: http://localhost:8080

## 🏗️ Build para Produção

### Frontend
```bash
cd client
npm run build
```

### Backend
```bash
cd api
./mvnw clean package
java -jar target/rubia-server-0.0.1-SNAPSHOT.jar
```

## 📁 Estrutura do Projeto

```
rubia/
├── client/                 # Frontend React
│   ├── src/
│   │   ├── adapters/       # Adaptadores para transformação de dados
│   │   ├── api/           # Configuração e serviços de API
│   │   ├── auth/          # Sistema de autenticação JWT
│   │   ├── components/    # Componentes React
│   │   │   ├── skeletons/ # Loading states
│   │   │   ├── notifications/ # Sistema de notificações
│   │   │   └── ...        # Outros componentes
│   │   ├── hooks/         # Custom hooks
│   │   ├── pages/         # Páginas da aplicação
│   │   ├── store/         # Gerenciamento de estado (Zustand)
│   │   ├── types/         # Definições TypeScript globais
│   │   ├── utils/         # Utilitários e validações
│   │   ├── websocket/     # Sistema WebSocket
│   │   │   ├── client.ts  # Cliente WebSocket
│   │   │   ├── eventHandlers.ts # Handlers de eventos
│   │   │   └── index.ts   # Manager principal
│   │   └── mocks/         # Dados mock para desenvolvimento
│   └── package.json
├── api/                    # Backend Spring Boot
│   ├── src/
│   │   ├── main/java/     # Código fonte Java
│   │   └── main/resources/ # Recursos e configurações
│   └── pom.xml
└── README.md
```

## 🎯 Funcionalidades

### 💬 **Chat em Tempo Real**
- **WebSocket** com reconexão automática e heartbeat
- **Typing indicators** ("usuário digitando...") 
- **Status de conexão** em tempo real
- **Notificações** para novas mensagens
- **Optimistic updates** para envio instantâneo

### 🔐 **Sistema de Autenticação**
- **JWT** com refresh automático de tokens
- **Proteção de rotas** baseada em roles (ADMIN, SUPERVISOR, AGENT)
- **Login/logout** com interceptors HTTP automáticos

### 📱 **Interface e UX**
- **Design responsivo** otimizado para mobile e desktop
- **Skeleton loading** para todos os estados de carregamento
- **Error boundaries** para tratamento robusto de erros
- **Infinite scroll** na lista de conversas
- **Sistema de notificações** toast personalizadas

### 📊 **Gerenciamento de Conversas**
- **Três status**: Entrada, Esperando, Finalizados
- **Sistema de tags** para organização
- **Busca inteligente** por contatos e mensagens
- **Transferência entre agentes** com notificações
- **Sistema de fixação** de conversas importantes
- **Cache inteligente** de mensagens por conversa

### 🆕 **Sistema de Criação de Contatos**
- **Criação diferida de conversas**: Novos contatos aparecem no sidebar apenas após primeira mensagem
- **Modal inteligente** com abas para contatos existentes e criação de novos
- **Validação de telefone brasileiro** automática
- **Detecção de contatos duplicados** por telefone
- **Interface otimizada** para centro de doação de sangue

### 🔄 **Integrações**
- **Adapter pattern** para transformação de dados
- **Validação de dados** com classes específicas
- **Sistema de eventos customizados** para comunicação entre componentes

## 📋 Scripts Disponíveis

### Frontend
- `npm run dev` - Servidor de desenvolvimento (http://localhost:5173)
- `npm run build` - Build para produção com TypeScript compilation
- `npm run lint` - Verificação de código ESLint
- `npm run preview` - Preview do build de produção
- `npx tsc --noEmit` - Verificação de tipos TypeScript

### Backend
- `./mvnw spring-boot:run` - Executa a aplicação
- `./mvnw test` - Executa testes
- `./mvnw clean package` - Gera o JAR

## 🔧 Configuração

### Variáveis de Ambiente Frontend
```env
# .env.development
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws

# .env.production  
VITE_API_URL=https://api.production.com/api
VITE_WS_URL=wss://api.production.com/ws
```

### Variáveis de Ambiente (Backend)
Configure no `application.properties`:
- Credenciais do PostgreSQL
- Configurações do Redis
- Configurações do RabbitMQ
- Configurações de segurança JWT
- Configurações WebSocket

## 📚 Documentação Detalhada

Para documentação técnica completa do frontend (arquitetura, padrões, componentes, WebSocket, etc.), consulte:

**[📖 Frontend - Documentação Completa](./client/README.md)**

Esta documentação inclui:
- 🏗️ **Arquitetura detalhada** com fluxo de dados
- 🔌 **Sistema WebSocket** com examples de código
- 🗄️ **Gerenciamento de Estado** com Zustand
- 🧩 **Componentes** e estrutura
- 🔐 **Sistema de Autenticação** JWT
- 🆕 **Sistema de Criação de Contatos** e conversas diferidas
- 🎨 **UX/UI Enhancements** e loading states
- 🔄 **APIs e Adaptadores** com padrões
- 🚀 **Build e Deploy** otimizado
- 🔧 **Troubleshooting** e debug

### 🌟 Funcionalidades Recentes

#### Criação Inteligente de Conversas
O sistema agora implementa um fluxo mais intuitivo onde:

1. **Novos contatos** são criados mas **não aparecem imediatamente** no sidebar
2. **Conversas são criadas dinamicamente** quando a primeira mensagem é enviada  
3. **Interface limpa** mostra apenas conversas com interação real
4. **Modal otimizado** separa contatos existentes de criação de novos

Isso resulta em melhor UX e performance, evitando poluição da interface com contatos sem conversas reais.

## 🔧 Troubleshooting Rápido

### Frontend Issues
```bash
# Verificar tipos
npx tsc --noEmit

# Verificar lint
npm run lint

# Build local
npm run build
```

### WebSocket não conecta?
1. Verificar se backend está rodando na porta 8080
2. Verificar URL do WebSocket (`VITE_WS_URL`)
3. Verificar autenticação JWT

## 🤝 Contribuição

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.