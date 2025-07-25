# Test Coverage Improvements: JSON Serialization & Entity Relationships

## Background
Durante o desenvolvimento da funcionalidade de mensageria em tempo real (RBY-21), foi identificada uma falha de referência circular Jackson entre as entidades `Company` e `Department` que não foi detectada pelos testes existentes. Este documento detalha os cenários de teste necessários para evitar que problemas similares passem despercebidos.

## Cenários de Teste a Implementar

### 1. Testes de Serialização JSON para Entidades (@JsonTest)

#### 1.1 CompanyJsonTest
```java
@JsonTest
class CompanyJsonTest {
    
    @Test
    void shouldSerializeCompanyWithoutDepartments() {
        // Testa serialização básica da Company
    }
    
    @Test
    void shouldSerializeCompanyWithDepartments() {
        // Testa serialização com relacionamentos carregados
        // DEVE detectar referências circulares
    }
    
    @Test
    void shouldDeserializeCompanyCorrectly() {
        // Testa deserialização de JSON para entidade
    }
    
    @Test
    void shouldHandleNullRelationships() {
        // Testa cenários com relacionamentos nulos
    }
}
```

#### 1.2 DepartmentJsonTest
```java
@JsonTest
class DepartmentJsonTest {
    
    @Test
    void shouldSerializeDepartmentWithCompany() {
        // Testa serialização com referência para Company
        // DEVE validar @JsonBackReference
    }
    
    @Test
    void shouldSerializeDepartmentWithUsers() {
        // Testa relacionamento Department -> Users
    }
    
    @Test
    void shouldHandleCircularReferencesProperly() {
        // Testa especificamente referências circulares
    }
}
```

### 2. Testes de Integração para Controllers

#### 2.1 CompanyControllerIntegrationTest
```java
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class CompanyControllerIntegrationTest {
    
    @Test
    void shouldGetCompanyById() {
        // GET /api/companies/{id}
        // Valida JSON response completo
    }
    
    @Test
    void shouldGetCompanyWithDepartments() {
        // GET /api/companies/{id}/departments
        // CENÁRIO CRÍTICO: testa serialização com relacionamentos
    }
    
    @Test
    void shouldCreateCompany() {
        // POST /api/companies
        // Valida serialização do request/response
    }
    
    @Test
    void shouldUpdateCompany() {
        // PUT /api/companies/{id}
        // Testa deserialização + serialização
    }
    
    @Test
    void shouldHandleComplexObjectGraphs() {
        // Testa Company com departments, whatsappInstances, etc.
        // DEVE detectar qualquer referência circular
    }
}
```

#### 2.2 DepartmentControllerIntegrationTest
```java
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class DepartmentControllerIntegrationTest {
    
    @Test
    void shouldGetDepartmentById() {
        // GET /api/departments/{id}
        // Valida JSON response com company reference
    }
    
    @Test
    void shouldGetDepartmentsByCompany() {
        // GET /api/companies/{companyId}/departments
        // Testa lista de departments com company references
    }
    
    @Test
    void shouldCreateDepartment() {
        // POST /api/departments
        // Valida criação com referência para company
    }
    
    @Test
    void shouldUpdateDepartment() {
        // PUT /api/departments/{id}
        // Testa atualização mantendo integridade dos relacionamentos
    }
}
```

### 3. Testes de Serialização Direta (ObjectMapper)

#### 3.1 EntitySerializationTest
```java
@SpringBootTest
class EntitySerializationTest {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldSerializeCompanyEntityDirectly() {
        // Company com departments carregados via JPA
        // objectMapper.writeValueAsString(company)
        // DEVE funcionar sem referência circular
    }
    
    @Test
    void shouldSerializeDepartmentEntityDirectly() {
        // Department com company carregado via JPA
        // objectMapper.writeValueAsString(department)
        // DEVE funcionar sem referência circular
    }
    
    @Test
    void shouldHandleLazyLoadedRelationships() {
        // Testa serialização com FetchType.LAZY
    }
    
    @Test
    void shouldHandleEagerLoadedRelationships() {
        // Testa serialização com FetchType.EAGER
    }
}
```

### 4. Testes de Configuração Jackson

#### 4.1 JacksonConfigurationTest
```java
@SpringBootTest
class JacksonConfigurationTest {
    
    @Test
    void shouldConfigureCircularReferenceHandling() {
        // Valida configuração global do Jackson
    }
    
    @Test
    void shouldRespectJsonManagedReference() {
        // Testa @JsonManagedReference annotations
    }
    
    @Test
    void shouldRespectJsonBackReference() {
        // Testa @JsonBackReference annotations
    }
    
    @Test
    void shouldHandleDeepObjectGraphs() {
        // Company -> Department -> Users -> Company
        // Testa grafos complexos sem explodir
    }
}
```

### 5. Testes de Cenários Reais de Uso

#### 5.1 WhatsAppSetupIntegrationTest (Expansão)
```java
// Expandir teste existente para incluir:

@Test
void shouldReturnCompanyWithInstancesInSetupStatus() {
    // GET /api/whatsapp-setup/status
    // Retorna Company com whatsappInstances
    // DEVE serializar sem referência circular
}
```

#### 5.2 AuthenticationIntegrationTest (Expansão)  
```java
// Expandir para incluir:

@Test
void shouldReturnAuthResponseWithCompanyInfo() {
    // POST /api/auth/login
    // Response inclui dados da company
    // DEVE serializar corretamente
}
```

### 6. Testes de Performance e Limites

#### 6.1 SerializationPerformanceTest
```java
@Test
void shouldHandleLargeObjectGraphsEfficiently() {
    // Company com muitos departments
    // Cada department com muitos users
    // Testa limites de serialização
}

@Test
void shouldNotExceedStackDepthLimits() {
    // Testa especificamente o limite de 1000 níveis
    // que causou o erro original
}
```

## Implementação Recomendada

### Fase 1: Testes Críticos (Alta Prioridade)
1. `CompanyJsonTest` e `DepartmentJsonTest` 
2. `EntitySerializationTest`
3. Expansão dos testes de integração existentes

### Fase 2: Testes de Controller (Média Prioridade)
1. `CompanyControllerIntegrationTest`
2. `DepartmentControllerIntegrationTest` 

### Fase 3: Testes Avançados (Baixa Prioridade)
1. `JacksonConfigurationTest`
2. `SerializationPerformanceTest`

## Configuração dos Testes

### Dependências Necessárias
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <scope>test</scope>
</dependency>
```

### Profile de Teste
```yaml
# application-test.yml
spring:
  jackson:
    serialization:
      fail-on-empty-beans: false
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop
```

## Benefícios Esperados

1. **Detecção Precoce**: Problemas de serialização serão detectados nos testes
2. **Documentação Viva**: Testes servem como documentação dos comportamentos esperados
3. **Regressão**: Evita que problemas similares sejam reintroduzidos
4. **Confiança**: Maior confiança ao fazer mudanças em entidades e relacionamentos
5. **Performance**: Identificação de problemas de performance na serialização

## Critérios de Aceitação

- [ ] Todos os testes passam sem referências circulares
- [ ] Cobertura de teste > 90% para classes de entidade
- [ ] Testes de integração cobrem todos os endpoints que retornam entidades
- [ ] Documentação dos cenários de teste mantida atualizada
- [ ] Pipeline CI/CD executa todos os novos testes automaticamente

---

**Estimativa de Esforço**: 3-5 dias de desenvolvimento
**Prioridade**: Alta (evita bugs em produção)
**Assignee**: [A definir]
**Epic**: Test Coverage Improvements
**Sprint**: [A definir]