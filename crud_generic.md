# CRUD Generic Implementation - Rubia Backend

## 🎯 Objetivo
Criar uma estrutura genérica reutilizável para implementar CRUDs de forma consistente e eficiente, reduzindo código repetitivo em ~70%.

## 📊 Análise dos Padrões Repetitivos

### **Identificados nos CRUDs existentes:**
- **Service Layer**: Métodos CRUD básicos idênticos, validação de relacionamentos, logs padrão
- **Controller Layer**: Endpoints REST padrão, validação de company context, paginação
- **Repository Layer**: Queries comuns (findByCompanyId, countByCompanyId)
- **Test Layer**: Estrutura de testes praticamente idêntica

---

## 🏗️ Arquitetura da Solução Genérica

### **Componentes Base:**
1. **BaseEntity** - Interface comum para entidades
2. **BaseCompanyEntityService** - Service abstrato com CRUD padrão
3. **BaseCompanyEntityController** - Controller abstrato com endpoints padrão
4. **BaseCompanyEntityRepository** - Repository com queries comuns
5. **BaseServiceMockTest** - Template de testes genérico
6. **EntityRelationshipValidator** - Validador de relacionamentos
7. **DTOMapper** - Utilitário para conversões Entity ↔ DTO

---

## 📋 Passo a Passo de Implementação

### **Fase 1: Criação da Base Genérica (60-90 min)**

#### 1.1 Base Entity Interface
```bash
touch api/src/main/java/com/ruby/rubia_server/core/base/BaseEntity.java
```

**Conteúdo:**
```java
public interface BaseEntity {
    UUID getId();
    void setId(UUID id);
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    Company getCompany();
    void setCompany(Company company);
}
```

#### 1.2 Base Service Abstract Class
```bash
touch api/src/main/java/com/ruby/rubia_server/core/base/BaseCompanyEntityService.java
```

**Funcionalidades:**
- CRUD básico padronizado
- Validação de relacionamentos
- Logs estruturados
- Tratamento de erros consistente

#### 1.3 Base Controller Abstract Class  
```bash
touch api/src/main/java/com/ruby/rubia_server/core/base/BaseCompanyEntityController.java
```

**Funcionalidades:**
- Endpoints REST padrão
- Validação de company context
- Paginação e sorting automático
- Conversão Entity → DTO

#### 1.4 Base Repository Interface
```bash
touch api/src/main/java/com/ruby/rubia_server/core/base/BaseCompanyEntityRepository.java
```

**Queries comuns:**
- `findByCompanyId(UUID companyId)`
- `countByCompanyId(UUID companyId)`
- `existsByNameAndCompanyId(String name, UUID companyId)`
- `findByCompanyIdAndDateRange(...)`

#### 1.5 Base Test Abstract Class
```bash
touch api/src/test/java/com/ruby/rubia_server/core/base/BaseServiceMockTest.java
```

**Template de testes:**
- 15+ testes padrão pré-implementados
- Abstract methods para customização
- Setup padrão com mocks

#### 1.6 Utilitários de Apoio
```bash
touch api/src/main/java/com/ruby/rubia_server/core/base/EntityRelationshipValidator.java
touch api/src/main/java/com/ruby/rubia_server/core/base/DTOMapper.java
touch api/src/main/java/com/ruby/rubia_server/core/base/CrudMetadata.java
```

### **Fase 2: Configuração e Anotações (30 min)**

#### 2.1 Entity Configuration Annotation
```bash
touch api/src/main/java/com/ruby/rubia_server/core/base/EntityCrudConfig.java
```

**Exemplo de uso:**
```java
@EntityCrudConfig(
    entityName = "Campaign",
    requiredRelations = {"Company"},
    optionalRelations = {"User", "MessageTemplate"},
    customQueries = {"findByStatus", "findByCompanyIdAndStatus"},
    enableSoftDelete = false
)
```

#### 2.2 DTO Validation Annotations
```bash
touch api/src/main/java/com/ruby/rubia_server/core/base/ValidateRelationship.java
```

### **Fase 3: Testes da Base Genérica (30 min)**

#### 3.1 Criar Entidade de Teste
```bash
touch api/src/test/java/com/ruby/rubia_server/core/base/TestEntity.java
touch api/src/test/java/com/ruby/rubia_server/core/base/CreateTestEntityDTO.java
touch api/src/test/java/com/ruby/rubia_server/core/base/UpdateTestEntityDTO.java
```

#### 3.2 Teste da Base Genérica
```bash
touch api/src/test/java/com/ruby/rubia_server/core/base/BaseGenericCrudTest.java
```

#### 3.3 Executar Testes Base
```bash
./mvnw test -Dtest=BaseGenericCrudTest
```

---

## 🔄 Como Usar a Base Genérica (15-20 min por CRUD)

### **Template para Nova Entidade:**

#### Passo 1: Adaptar Entidade (2 min)
```java
@Entity
public class Campaign extends BaseEntity {
    // Implementar métodos da interface BaseEntity
    // Manter campos específicos da entidade
}
```

#### Passo 2: Criar DTOs (5 min)
```java
@Data
@Builder
public class CreateCampaignDTO {
    @ValidateRelationship(entity = "Company", required = true)
    private UUID companyId;
    
    @ValidateRelationship(entity = "User", required = false)
    private UUID createdByUserId;
    
    @NotBlank
    private String name;
    
    // Outros campos específicos...
}
```

#### Passo 3: Repository (1 min)
```java
@Repository
public interface CampaignRepository extends BaseCompanyEntityRepository<Campaign> {
    // Queries específicas se necessário
    List<Campaign> findByStatus(CampaignStatus status);
    List<Campaign> findByCompanyIdAndStatus(UUID companyId, CampaignStatus status);
}
```

#### Passo 4: Service (3 min)
```java
@Service
@EntityCrudConfig(
    entityName = "Campaign",
    requiredRelations = {"Company"},
    optionalRelations = {"User", "MessageTemplate"}
)
public class CampaignService extends BaseCompanyEntityService<Campaign, CreateCampaignDTO, UpdateCampaignDTO> {
    
    // Apenas métodos específicos da entidade
    public List<Campaign> findByStatus(CampaignStatus status) {
        return campaignRepository.findByStatus(status);
    }
}
```

#### Passo 5: Controller (2 min)
```java
@RestController
@RequestMapping("/api/campaigns")
public class CampaignController extends BaseCompanyEntityController<Campaign, CreateCampaignDTO, UpdateCampaignDTO, CampaignDTO> {
    
    @Override
    protected CampaignDTO convertToDTO(Campaign entity) {
        return DTOMapper.toDTO(entity, CampaignDTO.class);
    }
    
    // Endpoints específicos se necessário
}
```

#### Passo 6: Testes (5 min)
```java
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest extends BaseServiceMockTest<Campaign, CreateCampaignDTO, UpdateCampaignDTO> {
    
    @Override
    protected CreateCampaignDTO createValidDTO() {
        return CreateCampaignDTO.builder()
                .companyId(companyId)
                .name("Test Campaign")
                .status(CampaignStatus.DRAFT)
                .build();
    }
    
    @Override
    protected Campaign createEntity() {
        return Campaign.builder()
                .id(entityId)
                .company(company)
                .name("Test Campaign")
                .build();
    }
    
    // Testes específicos se necessário
}
```

#### Passo 7: Executar Testes (2 min)
```bash
./mvnw test -Dtest=CampaignServiceTest
./mvnw test  # Todos os testes
```

---

## 🎯 Implementação Detalhada dos Componentes Base

### **1. BaseEntity Interface**
```java
public interface BaseEntity {
    UUID getId();
    void setId(UUID id);
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    Company getCompany();
    void setCompany(Company company);
    
    // Métodos utilitários
    default boolean isNew() {
        return getId() == null;
    }
    
    default boolean belongsToCompany(UUID companyId) {
        return getCompany() != null && getCompany().getId().equals(companyId);
    }
}
```

### **2. BaseCompanyEntityService**
```java
@Slf4j
@Transactional
public abstract class BaseCompanyEntityService<T extends BaseEntity, CreateDTO, UpdateDTO> {
    
    protected final JpaRepository<T, UUID> repository;
    protected final CompanyRepository companyRepository;
    protected final EntityRelationshipValidator relationshipValidator;
    
    // Template methods (implementação padrão)
    public T create(CreateDTO createDTO) {
        log.info("Creating {} with data: {}", getEntityName(), createDTO);
        
        // Validar relacionamentos
        relationshipValidator.validateRelationships(createDTO);
        
        // Buscar company obrigatória
        Company company = getCompanyFromDTO(createDTO);
        
        // Criar entidade
        T entity = buildEntityFromDTO(createDTO);
        entity.setCompany(company);
        
        // Salvar
        T saved = repository.save(entity);
        log.info("{} created successfully with id: {}", getEntityName(), saved.getId());
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public Optional<T> findById(UUID id) {
        log.debug("Finding {} by id: {}", getEntityName(), id);
        return repository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        log.debug("Finding all {} with pagination: {}", getEntityName(), pageable);
        return repository.findAll(pageable);
    }
    
    public Optional<T> update(UUID id, UpdateDTO updateDTO) {
        log.info("Updating {} with id: {}", getEntityName(), id);
        
        Optional<T> entityOpt = repository.findById(id);
        if (entityOpt.isEmpty()) {
            log.warn("{} not found with id: {}", getEntityName(), id);
            return Optional.empty();
        }
        
        T entity = entityOpt.get();
        updateEntityFromDTO(entity, updateDTO);
        
        T saved = repository.save(entity);
        log.info("{} updated successfully with id: {}", getEntityName(), saved.getId());
        
        return Optional.of(saved);
    }
    
    public boolean deleteById(UUID id) {
        log.info("Deleting {} with id: {}", getEntityName(), id);
        
        if (!repository.existsById(id)) {
            log.warn("{} not found with id: {}", getEntityName(), id);
            return false;
        }
        
        repository.deleteById(id);
        log.info("{} deleted successfully", getEntityName());
        return true;
    }
    
    // Métodos comuns implementados
    @Transactional(readOnly = true)
    public List<T> findByCompanyId(UUID companyId) {
        return ((BaseCompanyEntityRepository<T>) repository).findByCompanyId(companyId);
    }
    
    @Transactional(readOnly = true)
    public long countByCompanyId(UUID companyId) {
        return ((BaseCompanyEntityRepository<T>) repository).countByCompanyId(companyId);
    }
    
    // Abstract methods para implementação específica
    protected abstract String getEntityName();
    protected abstract T buildEntityFromDTO(CreateDTO createDTO);
    protected abstract void updateEntityFromDTO(T entity, UpdateDTO updateDTO);
    protected abstract Company getCompanyFromDTO(CreateDTO createDTO);
}
```

### **3. BaseCompanyEntityController**
```java
@Slf4j
public abstract class BaseCompanyEntityController<T extends BaseEntity, CreateDTO, UpdateDTO, ResponseDTO> {
    
    protected final BaseCompanyEntityService<T, CreateDTO, UpdateDTO> service;
    protected final CompanyContextUtil companyContextUtil;
    
    @PostMapping
    public ResponseEntity<ResponseDTO> create(@Valid @RequestBody CreateDTO createDTO) {
        log.info("Creating {} via API", getEntityName());
        
        // Validar company context
        UUID companyId = getCompanyIdFromDTO(createDTO);
        companyContextUtil.ensureCompanyAccess(companyId);
        
        T entity = service.create(createDTO);
        ResponseDTO responseDTO = convertToDTO(entity);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> findById(@PathVariable UUID id) {
        log.debug("Finding {} by id via API: {}", getEntityName(), id);
        
        return service.findById(id)
                .map(entity -> {
                    companyContextUtil.ensureCompanyAccess(entity.getCompany().getId());
                    return ResponseEntity.ok(convertToDTO(entity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<Page<ResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.debug("Finding all {} via API - page: {}, size: {}", getEntityName(), page, size);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<T> entities = service.findAll(pageable);
        Page<ResponseDTO> responseDTOs = entities.map(this::convertToDTO);
        
        return ResponseEntity.ok(responseDTOs);
    }
    
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ResponseDTO>> findByCompany(@PathVariable UUID companyId) {
        log.debug("Finding {} by company via API: {}", getEntityName(), companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<T> entities = service.findByCompanyId(companyId);
        List<ResponseDTO> responseDTOs = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody UpdateDTO updateDTO) {
        log.info("Updating {} via API with id: {}", getEntityName(), id);
        
        return service.update(id, updateDTO)
                .map(entity -> {
                    companyContextUtil.ensureCompanyAccess(entity.getCompany().getId());
                    return ResponseEntity.ok(convertToDTO(entity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        log.info("Deleting {} via API with id: {}", getEntityName(), id);
        
        // Verificar se existe e tem acesso antes de deletar
        Optional<T> entity = service.findById(id);
        if (entity.isPresent()) {
            companyContextUtil.ensureCompanyAccess(entity.get().getCompany().getId());
            boolean deleted = service.deleteById(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.notFound().build();
    }
    
    // Métodos utilitários
    protected Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
    
    // Abstract methods
    protected abstract String getEntityName();
    protected abstract ResponseDTO convertToDTO(T entity);
    protected abstract UUID getCompanyIdFromDTO(CreateDTO createDTO);
}
```

### **4. BaseServiceMockTest**
```java
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceMockTest<T extends BaseEntity, CreateDTO, UpdateDTO> {
    
    @Mock protected JpaRepository<T, UUID> repository;
    @Mock protected CompanyRepository companyRepository;
    @Mock protected EntityRelationshipValidator relationshipValidator;
    
    protected Company company;
    protected T entity;
    protected CreateDTO createDTO;
    protected UpdateDTO updateDTO;
    protected UUID companyId;
    protected UUID entityId;
    
    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        entityId = UUID.randomUUID();
        
        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();
        
        entity = createEntity();
        createDTO = createValidDTO();
        updateDTO = createUpdateDTO();
    }
    
    // Template tests (15+ testes padronizados)
    @Test
    void create_ShouldCreateAndReturn_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenReturn(entity);
        
        // When
        T result = getService().create(createDTO);
        
        // Then
        assertNotNull(result);
        assertEquals(entity.getId(), result.getId());
        verify(companyRepository).findById(companyId);
        verify(repository).save(any());
    }
    
    @Test
    void create_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> getService().create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(repository, never()).save(any());
    }
    
    // ... mais 13+ testes padrão
    
    // Abstract methods para customização
    protected abstract T createEntity();
    protected abstract CreateDTO createValidDTO();
    protected abstract UpdateDTO createUpdateDTO();
    protected abstract BaseCompanyEntityService<T, CreateDTO, UpdateDTO> getService();
}
```

---

## 🚀 Cronograma de Implementação

### **Fase 1: Base Genérica (1 dia)**
- [ ] **Manhã**: BaseEntity, BaseService, BaseController
- [ ] **Tarde**: BaseRepository, BaseTest, Utilitários
- [ ] **Noite**: Testes da base genérica

### **Fase 2: Primeiro CRUD com Base (0.5 dia)**
- [ ] **Manhã**: Campaign usando base genérica
- [ ] **Tarde**: Testes e ajustes da base

### **Fase 3: CRUDs Restantes (2 dias)**
- [ ] **Dia 1**: CampaignContact, ConversationMedia, ConversationParticipant  
- [ ] **Dia 2**: DonationAppointment, MessageTemplate, MessageTemplateRevision, UserAIAgent

### **Total Estimado: 3.5 dias vs 8 dias (economia de 56%)**

---

## ✅ Critérios de Sucesso

1. **Redução de código**: 70% menos código repetitivo
2. **Velocidade**: 15-20 min por CRUD vs 60-90 min
3. **Consistência**: Padrão uniforme em todos os CRUDs
4. **Qualidade**: Manter 90%+ cobertura de testes
5. **Manutenibilidade**: Mudanças centralizadas na base

---

## 🎯 Próximos Passos

1. **Implementar Base Genérica** seguindo o passo a passo
2. **Testar com Campaign** como piloto
3. **Ajustar base** com base no feedback
4. **Implementar CRUDs restantes** usando a base
5. **Documentar padrões** para futuros CRUDs

**Comando para verificar progresso:**
```bash
./mvnw test | grep "Tests run" | tail -1
```

**Meta**: Chegar a 200+ testes passando com implementação eficiente e padronizada.