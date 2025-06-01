# Rubia - Chat Corporativo com IA

Uma aplicação de chat corporativo moderna com funcionalidades de chatbot alimentado por IA, desenvolvida para facilitar a comunicação entre equipes e clientes.

## 🏗️ Arquitetura

O projeto é dividido em duas partes principais:

- **Frontend** (`/client`): Aplicação React com TypeScript
- **Backend** (`/api`): API REST com Spring Boot

## 🚀 Tecnologias

### Frontend
- React 19 + TypeScript
- Vite (build tool)
- Ant Design (componentes UI)
- Tailwind CSS (estilização)
- Zustand (gerenciamento de estado)
- Lucide React (ícones)

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
│   │   ├── components/     # Componentes React
│   │   ├── pages/         # Páginas da aplicação
│   │   ├── store/         # Gerenciamento de estado (Zustand)
│   │   ├── types/         # Definições TypeScript
│   │   ├── utils/         # Utilitários
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

- **Gerenciamento de conversas** com três status: Entrada, Esperando, Finalizados
- **Chat em tempo real** via WebSocket
- **Sistema de tags** para organização de conversas
- **Busca inteligente** por contatos e mensagens
- **Transferência de conversas** entre agentes
- **Sistema de fixação** de conversas importantes
- **Interface responsiva** otimizada para desktop

## 📋 Scripts Disponíveis

### Frontend
- `npm run dev` - Servidor de desenvolvimento
- `npm run build` - Build para produção
- `npm run lint` - Verificação de código
- `npm run preview` - Preview do build

### Backend
- `./mvnw spring-boot:run` - Executa a aplicação
- `./mvnw test` - Executa testes
- `./mvnw clean package` - Gera o JAR

## 🔧 Configuração

### Variáveis de Ambiente (Backend)
Configure no `application.properties`:
- Credenciais do PostgreSQL
- Configurações do Redis
- Configurações do RabbitMQ
- Configurações de segurança

## 🤝 Contribuição

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.