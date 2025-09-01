# Rubia.Server.Tests

Este projeto contém testes de integração migrados do sistema Java Spring Boot para C# .NET.

## Estrutura dos Testes

### Testes de Integração do CampaignController

Os testes migrados do Java Spring Boot incluem:

1. **ShouldProcessValidExcelFileAndCreateTwoContacts**
   - Testa o processamento de um arquivo Excel válido (`planilha_correta.xlsx`)
   - Verifica se 2 contatos são criados corretamente
   - Valida dados específicos dos contatos (MAURA RODRIGUES e TARZE CARVALHO)
   - Confirma criação de campaign, campaign_contacts e conversações

2. **ShouldRejectExcelFileWithDuplicatePhones**
   - Testa o processamento de um arquivo Excel com duplicatas (`planilha_incorreta.xlsx`)
   - Verifica se duplicatas são detectadas e tratadas adequadamente
   - Garante que apenas contatos únicos são criados

3. **ShouldCreateTwoCampaignsWithSameContactsWithoutError**
   - Testa a criação de duas campanhas com os mesmos contatos
   - Verifica se o sistema permite reutilizar contatos em diferentes campanhas
   - Confirma que 4 conversações são criadas (2 contatos × 2 campanhas)

4. **ShouldUpdateContactAndCreateTwoCampaigns**
   - Testa o update de contatos existentes usando `planilha_correta_update.xlsx`
   - Verifica se as informações dos contatos são atualizadas corretamente
   - Confirma que duas campanhas são criadas com sucesso

## Recursos de Teste

### Planilhas Excel
- `planilha_correta.xlsx` - Arquivo Excel válido com 2 contatos
- `planilha_incorreta.xlsx` - Arquivo Excel com duplicatas para teste de validação
- `planilha_correta_update.xlsx` - Arquivo Excel com dados atualizados para teste de update

### Dados de Teste
Os testes usam dados específicos:
- **MAURA RODRIGUES**: Telefone +5511999999999, Data nascimento 18/09/2003
- **TARZE CARVALHO**: Telefone +5511999999992, Data nascimento 16/08/2005

## Como Executar

### Pré-requisitos
- .NET 8.0 SDK
- SQL Server ou PostgreSQL (para testes locais)

### Comandos
```bash
# Restaurar dependências
dotnet restore

# Compilar o projeto
dotnet build

# Executar todos os testes
dotnet test

# Executar testes específicos do CampaignController
dotnet test --filter CampaignControllerIntegrationTest

# Executar com saída detalhada
dotnet test --logger "console;verbosity=detailed"
```

## Tecnologias Utilizadas

- **xUnit**: Framework de testes
- **FluentAssertions**: Biblioteca para assertions mais legíveis
- **Microsoft.AspNetCore.Mvc.Testing**: Testes de integração para ASP.NET Core
- **Microsoft.EntityFrameworkCore.InMemory**: Banco em memória para testes
- **EPPlus**: Processamento de arquivos Excel
- **Newtonsoft.Json**: Serialização JSON
- **Moq**: Framework de mocking

## Arquitetura dos Testes

### BaseIntegrationTest
Classe base que configura:
- Cliente HTTP de teste
- Banco de dados em memória
- Limpeza automática de dados entre testes
- Configuração de ambiente de teste

### IAsyncLifetime
Interface implementada para:
- Configuração assíncrona de dados de teste
- Limpeza automática após cada teste
- Gerenciamento de recursos

## Migração do Java

Estes testes foram migrados do arquivo Java:
`/api/src/test/java/com/ruby/rubia_server/controller/CampaignControllerIntegrationTest.java`

### Principais Diferenças
- **MockMvc** (Java) → **WebApplicationFactory** (C#)
- **@Test** (JUnit) → **[Fact]** (xUnit)
- **assertThat** (AssertJ) → **Should()** (FluentAssertions)
- **LocalDate** (Java) → **DateOnly** (C#)
- **ClassPathResource** (Java) → **File.ReadAllBytesAsync** (C#)