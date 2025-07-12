# Configuração de Branch Protection Rules

Para garantir que os testes sejam bloqueantes ao merge de PRs, configure as seguintes regras de proteção de branch no GitHub:

## Como Configurar

1. Acesse o repositório no GitHub
2. Vá em **Settings** > **Branches**
3. Clique em **Add rule** ou edite a regra existente para `main`

## Configurações Recomendadas

### Branch name pattern
```
main
```

### Configurações obrigatórias:
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: `1`
  - ✅ Dismiss stale PR approvals when new commits are pushed
  - ✅ Require review from code owners (se houver CODEOWNERS)

- ✅ **Require status checks to pass before merging**
  - ✅ Require branches to be up to date before merging
  - ✅ **Status checks obrigatórios:**
    - `Build and Compile`
    - `Run Unit and Integration Tests`
    - `Test Status Check`

- ✅ **Require conversation resolution before merging**

- ✅ **Restrict pushes that create files larger than 100MB**

### Configurações opcionais (recomendadas):
- ✅ **Require linear history** (para manter histórico limpo)
- ✅ **Include administrators** (aplicar regras para admins também)

## Workflows Criados

### 1. `backend-tests.yml`
- **Trigger**: PRs e pushes para `main`/`develop` que modificam arquivos em `api/`
- **Função**: Executa todos os testes unitários e de integração
- **Bloqueante**: ❌ Se qualquer teste falhar, o PR não pode ser mergeado
- **Banco**: PostgreSQL 13.3 em container
- **Report**: Gera relatórios de teste em formato JUnit

### 2. `backend-build.yml`
- **Trigger**: PRs e pushes para `main`/`develop` que modificam arquivos em `api/`
- **Função**: Compila e gera o build da aplicação
- **Bloqueante**: ❌ Se a compilação falhar, o PR não pode ser mergeado
- **Artefatos**: Salva o JAR gerado por 7 dias

## Verificação de Status

Após configurar, cada PR mostrará:

```
✅ Build and Compile — Required
✅ Run Unit and Integration Tests — Required  
✅ Test Status Check — Required
⏳ Some checks haven't completed yet
```

⚠️ **IMPORTANTE**: O merge só será permitido quando todos os checks estiverem verdes (✅).

## Exemplo de PR Bloqueado

```
❌ 1 required status check has failed
   × Run Unit and Integration Tests

This branch has not been merged because it does not satisfy the required status checks.
```

## Comandos para Testar Localmente

Antes de abrir um PR, execute localmente:

```bash
cd api

# Compilação
./mvnw clean compile

# Testes
./mvnw clean test

# Build completo
./mvnw clean package
```

Se algum comando falhar localmente, o PR também falhará no GitHub Actions.