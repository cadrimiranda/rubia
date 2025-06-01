# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Architecture

**Rubia** is a corporate chat application with AI-powered chatbot functionality, consisting of:

- **Frontend (`/client`)**: React 19 + TypeScript + Vite application using Ant Design for UI components and Zustand for state management
- **Backend (`/api`)**: Spring Boot 3.5 application with Java 24, supporting WebSocket connections, RabbitMQ messaging, Redis caching, PostgreSQL database with Flyway migrations

### Key Architecture Patterns

**Frontend State Management**: Uses Zustand store (`useChatStore.ts`) as the single source of truth for chat state, managing conversations across three status categories: `entrada` (incoming), `esperando` (waiting), and `finalizados` (finalized).

**Backend Stack**: Spring Boot with Security, JPA, WebSocket, AMQP (RabbitMQ), Redis, and Actuator for monitoring. Uses Flyway for database migrations and Prometheus for metrics.

**Component Structure**: React components are organized by feature (`ChatHeader`, `ChatInput`, `ChatMessage`, etc.) with shared types in `/types/index.ts` and utilities in `/utils/`.

## Development Commands

### Frontend Development (from `/client` directory)
```bash
npm run dev        # Start development server with hot reload
npm run build      # Build for production (TypeScript compilation + Vite build)
npm run lint       # Run ESLint
npm run preview    # Preview production build
```

### Backend Development (from `/api` directory)
```bash
./mvnw spring-boot:run              # Start Spring Boot application
./mvnw clean compile                # Compile Java sources
./mvnw test                         # Run tests
./mvnw clean package                # Build JAR package
```

## Tech Stack Details

**Frontend Dependencies**:
- React 19 with TypeScript
- Ant Design 5.25+ for UI components
- Zustand for state management
- Lucide React for icons
- Tailwind CSS for styling
- Vite for build tooling

**Backend Dependencies**:
- Spring Boot 3.5 with Java 24
- PostgreSQL with Flyway migrations
- Redis for caching
- RabbitMQ for messaging
- Spring Security for authentication
- WebSocket for real-time communication
- Actuator + Prometheus for monitoring

## Important Notes

- Package name in backend is `com.ruby.rubia_server` (note underscore, not hyphen)
- Frontend uses Portuguese for UI text but English for code
- Chat status workflow: entrada → esperando → finalizados
- All TypeScript interfaces are defined in `/client/src/types/index.ts`
- Mock data is available in `/client/src/mocks/data.ts` for development