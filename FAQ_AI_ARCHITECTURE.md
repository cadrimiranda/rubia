# Arquitetura: Sistema de FAQ Integrado com IA

## Visão Geral
Sistema que permite operadores cadastrarem FAQs e a IA utilizar essas informações para gerar respostas automáticas em DRAFT.

## 1. Estrutura de Dados

### FAQ Entity
```java
@Entity
@Table(name = "faqs")
public class FAQ {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String question;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;
    
    @ElementCollection
    @CollectionTable(name = "faq_keywords")
    private Set<String> keywords = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "faq_triggers")
    private Set<String> triggers = new HashSet<>(); // palavras que ativam esta FAQ
    
    @Enumerated(EnumType.STRING)
    private FAQCategory category;
    
    @Enumerated(EnumType.STRING)
    private FAQPriority priority = FAQPriority.MEDIUM;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0; // quantas vezes foi usada pela IA
    
    @Column(name = "success_rate")
    private Double successRate = 0.0; // % de aprovação quando usada
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Score calculado para busca (baseado em usage + success rate)
    @Formula("(usage_count * success_rate) / 100.0")
    private Double relevanceScore;
}
```

### Enums
```java
public enum FAQCategory {
    VENDAS("Vendas"),
    SUPORTE_TECNICO("Suporte Técnico"),
    FINANCEIRO("Financeiro"),
    PRODUTOS("Produtos"),
    POLITICAS("Políticas"),
    PROCEDIMENTOS("Procedimentos"),
    GERAL("Geral");
    
    private final String displayName;
}

public enum FAQPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);
    
    private final int weight;
}
```

### FAQ Match Entity (Para tracking)
```java
@Entity
@Table(name = "faq_matches")
public class FAQMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "faq_id")
    private FAQ faq;
    
    @Column(name = "user_message")
    private String userMessage;
    
    @Column(name = "confidence_score")
    private Double confidenceScore; // 0.0 - 1.0
    
    @Column(name = "was_used")
    private Boolean wasUsed = false; // se a IA usou esta FAQ no draft
    
    @Column(name = "was_approved")
    private Boolean wasApproved; // se o draft foi aprovado pelo operador
    
    @CreationTimestamp
    private LocalDateTime matchedAt;
}
```

## 2. Services Backend

### FAQService.java
```java
@Service
@Transactional
public class FAQService {
    
    @Autowired
    private FAQRepository faqRepository;
    
    @Autowired
    private FAQMatchRepository faqMatchRepository;
    
    // CRUD Operations
    public FAQ createFAQ(CreateFAQRequest request) {
        FAQ faq = new FAQ();
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setKeywords(extractAndNormalizeKeywords(request.getQuestion()));
        faq.setTriggers(request.getTriggers());
        faq.setCategory(request.getCategory());
        faq.setPriority(request.getPriority());
        faq.setCreatedBy(request.getOperatorId());
        
        return faqRepository.save(faq);
    }
    
    public List<FAQ> getAllFAQs(FAQFilter filter) {
        return faqRepository.findAllWithFilter(filter);
    }
    
    public FAQ updateFAQ(Long id, UpdateFAQRequest request) {
        FAQ faq = faqRepository.findById(id)
            .orElseThrow(() -> new FAQNotFoundException(id));
        
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setKeywords(extractAndNormalizeKeywords(request.getQuestion()));
        faq.setTriggers(request.getTriggers());
        faq.setCategory(request.getCategory());
        faq.setPriority(request.getPriority());
        faq.setIsActive(request.getIsActive());
        
        return faqRepository.save(faq);
    }
    
    public void deleteFAQ(Long id) {
        faqRepository.deleteById(id);
    }
    
    // AI Integration Methods
    public List<FAQ> findRelevantFAQs(String userMessage, int limit) {
        List<FAQ> exactMatches = findExactMatches(userMessage);
        List<FAQ> keywordMatches = findKeywordMatches(userMessage);
        List<FAQ> triggerMatches = findTriggerMatches(userMessage);
        
        // Combina resultados e ordena por relevância
        return combineAndRankResults(exactMatches, keywordMatches, triggerMatches, limit);
    }
    
    public void recordFAQUsage(Long faqId, String userMessage, Double confidence, Boolean approved) {
        // Registra uso da FAQ
        FAQMatch match = new FAQMatch();
        match.setFaq(faqRepository.getReferenceById(faqId));
        match.setUserMessage(userMessage);
        match.setConfidenceScore(confidence);
        match.setWasUsed(true);
        match.setWasApproved(approved);
        
        faqMatchRepository.save(match);
        
        // Atualiza estatísticas da FAQ
        updateFAQStatistics(faqId);
    }
    
    private List<FAQ> findExactMatches(String message) {
        // Busca por similaridade exata na pergunta
        return faqRepository.findByQuestionContainingIgnoreCase(normalizeText(message));
    }
    
    private List<FAQ> findKeywordMatches(String message) {
        Set<String> messageKeywords = extractKeywords(message);
        return faqRepository.findByKeywordsIn(messageKeywords);
    }
    
    private List<FAQ> findTriggerMatches(String message) {
        return faqRepository.findByTriggersMatching(normalizeText(message));
    }
    
    private Set<String> extractAndNormalizeKeywords(String text) {
        // Remove stopwords, normaliza, extrai palavras-chave
        return Arrays.stream(text.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 2)
            .filter(word -> !STOP_WORDS.contains(word))
            .collect(Collectors.toSet());
    }
    
    private void updateFAQStatistics(Long faqId) {
        List<FAQMatch> matches = faqMatchRepository.findByFaqId(faqId);
        
        int totalUsages = matches.size();
        long approvals = matches.stream()
            .filter(match -> Boolean.TRUE.equals(match.getWasApproved()))
            .count();
        
        double successRate = totalUsages > 0 ? (double) approvals / totalUsages * 100 : 0;
        
        FAQ faq = faqRepository.findById(faqId).orElseThrow();
        faq.setUsageCount(totalUsages);
        faq.setSuccessRate(successRate);
        
        faqRepository.save(faq);
    }
}
```

### FAQSearchService.java
```java
@Service
public class FAQSearchService {
    
    @Autowired
    private FAQService faqService;
    
    public FAQSearchResult searchFAQsForMessage(String userMessage) {
        // Busca FAQs relevantes
        List<FAQ> relevantFAQs = faqService.findRelevantFAQs(userMessage, 5);
        
        // Calcula scores de confiança
        List<FAQMatch> scoredMatches = relevantFAQs.stream()
            .map(faq -> calculateMatchScore(faq, userMessage))
            .sorted((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()))
            .collect(Collectors.toList());
        
        return new FAQSearchResult(scoredMatches, userMessage);
    }
    
    private FAQMatch calculateMatchScore(FAQ faq, String userMessage) {
        double score = 0.0;
        
        // Peso por categoria de match
        if (containsExactMatch(faq.getQuestion(), userMessage)) {
            score += 0.5;
        }
        
        // Peso por keywords
        score += calculateKeywordScore(faq.getKeywords(), userMessage) * 0.3;
        
        // Peso por triggers
        score += calculateTriggerScore(faq.getTriggers(), userMessage) * 0.2;
        
        // Peso por histórico de sucesso
        score += (faq.getSuccessRate() / 100.0) * 0.1;
        
        // Peso por prioridade
        score += (faq.getPriority().getWeight() / 4.0) * 0.1;
        
        FAQMatch match = new FAQMatch();
        match.setFaq(faq);
        match.setUserMessage(userMessage);
        match.setConfidenceScore(Math.min(score, 1.0));
        
        return match;
    }
}
```

## 3. Integration com AIDraftService

### Modified AIDraftService.java
```java
@Service
public class AIDraftService {
    
    @Autowired
    private FAQSearchService faqSearchService;
    
    @Autowired
    private FAQService faqService;
    
    public MessageDraft generateDraftResponse(Long conversationId, String userMessage) {
        // Busca FAQs relevantes
        FAQSearchResult faqResults = faqSearchService.searchFAQsForMessage(userMessage);
        
        // Constrói contexto com FAQs
        String context = buildContextWithFAQs(conversationId, faqResults);
        
        // Gera resposta via OpenAI
        AIResponse response = openAIService.generateResponse(context, userMessage);
        
        // Salva draft
        MessageDraft draft = new MessageDraft();
        draft.setConversationId(conversationId);
        draft.setContent(response.getContent());
        draft.setConfidence(response.getConfidence());
        draft.setAiModel(response.getModel());
        
        MessageDraft savedDraft = messageDraftRepository.save(draft);
        
        // Registra uso das FAQs
        recordFAQUsage(faqResults, userMessage, savedDraft);
        
        return savedDraft;
    }
    
    private String buildContextWithFAQs(Long conversationId, FAQSearchResult faqResults) {
        StringBuilder context = new StringBuilder();
        
        // Contexto da conversa
        context.append("HISTÓRICO DA CONVERSA:\n");
        context.append(getConversationHistory(conversationId));
        context.append("\n\n");
        
        // FAQs relevantes
        if (!faqResults.getMatches().isEmpty()) {
            context.append("FAQs RELEVANTES PARA CONSULTA:\n");
            
            faqResults.getMatches().forEach(match -> {
                context.append(String.format(
                    "PERGUNTA: %s\n" +
                    "RESPOSTA: %s\n" +
                    "CONFIANÇA: %.2f\n" +
                    "CATEGORIA: %s\n\n",
                    match.getFaq().getQuestion(),
                    match.getFaq().getAnswer(),
                    match.getConfidenceScore(),
                    match.getFaq().getCategory().getDisplayName()
                ));
            });
        }
        
        return context.toString();
    }
    
    private void recordFAQUsage(FAQSearchResult faqResults, String userMessage, MessageDraft draft) {
        // Registra quais FAQs foram consideradas para esta resposta
        faqResults.getMatches().forEach(match -> {
            if (match.getConfidenceScore() > 0.5) { // threshold para considerar "usada"
                faqService.recordFAQUsage(
                    match.getFaq().getId(),
                    userMessage,
                    match.getConfidenceScore(),
                    null // será atualizado quando operador aprovar/rejeitar
                );
            }
        });
    }
}
```

## 4. Controllers

### FAQController.java
```java
@RestController
@RequestMapping("/api/faqs")
@PreAuthorize("hasRole('OPERATOR')")
public class FAQController {
    
    @Autowired
    private FAQService faqService;
    
    @GetMapping
    public ResponseEntity<PagedResponse<FAQ>> getAllFAQs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) FAQCategory category,
            @RequestParam(required = false) String search) {
        
        FAQFilter filter = FAQFilter.builder()
            .category(category)
            .searchTerm(search)
            .build();
            
        Page<FAQ> faqs = faqService.getAllFAQs(filter, PageRequest.of(page, size));
        
        return ResponseEntity.ok(new PagedResponse<>(faqs));
    }
    
    @PostMapping
    public ResponseEntity<FAQ> createFAQ(@Valid @RequestBody CreateFAQRequest request) {
        FAQ faq = faqService.createFAQ(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(faq);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<FAQ> updateFAQ(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateFAQRequest request) {
        FAQ faq = faqService.updateFAQ(id, request);
        return ResponseEntity.ok(faq);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFAQ(@PathVariable Long id) {
        faqService.deleteFAQ(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/analytics")
    public ResponseEntity<FAQAnalytics> getFAQAnalytics(@PathVariable Long id) {
        FAQAnalytics analytics = faqService.getAnalytics(id);
        return ResponseEntity.ok(analytics);
    }
    
    @PostMapping("/test-search")
    public ResponseEntity<List<FAQMatch>> testSearch(@RequestBody TestSearchRequest request) {
        FAQSearchResult result = faqSearchService.searchFAQsForMessage(request.getMessage());
        return ResponseEntity.ok(result.getMatches());
    }
}
```

## 5. Frontend Components

### FAQManagement.tsx
```tsx
interface FAQ {
  id: number;
  question: string;
  answer: string;
  keywords: string[];
  triggers: string[];
  category: FAQCategory;
  priority: FAQPriority;
  usageCount: number;
  successRate: number;
  isActive: boolean;
  createdAt: string;
  createdBy: string;
}

const FAQManagement: React.FC = () => {
  const [faqs, setFaqs] = useState<FAQ[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedFAQ, setSelectedFAQ] = useState<FAQ | null>(null);
  const [isModalVisible, setIsModalVisible] = useState(false);

  const columns: ColumnsType<FAQ> = [
    {
      title: 'Pergunta',
      dataIndex: 'question',
      key: 'question',
      ellipsis: true,
      width: 300,
    },
    {
      title: 'Categoria',
      dataIndex: 'category',
      key: 'category',
      filters: Object.values(FAQCategory).map(cat => ({ text: cat, value: cat })),
      onFilter: (value, record) => record.category === value,
      render: (category: FAQCategory) => (
        <Tag color={getCategoryColor(category)}>{category}</Tag>
      ),
    },
    {
      title: 'Prioridade',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority: FAQPriority) => (
        <Tag color={getPriorityColor(priority)}>{priority}</Tag>
      ),
    },
    {
      title: 'Uso',
      dataIndex: 'usageCount',
      key: 'usageCount',
      sorter: (a, b) => a.usageCount - b.usageCount,
      render: (count: number) => (
        <Badge count={count} style={{ backgroundColor: '#52c41a' }} />
      ),
    },
    {
      title: 'Taxa de Sucesso',
      dataIndex: 'successRate',
      key: 'successRate',
      sorter: (a, b) => a.successRate - b.successRate,
      render: (rate: number) => (
        <Progress 
          percent={Math.round(rate)} 
          size="small" 
          status={rate > 70 ? 'success' : rate > 40 ? 'normal' : 'exception'}
        />
      ),
    },
    {
      title: 'Status',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (isActive: boolean) => (
        <Switch 
          checked={isActive} 
          onChange={(checked) => handleToggleStatus(selectedFAQ?.id!, checked)}
        />
      ),
    },
    {
      title: 'Ações',
      key: 'actions',
      render: (_, record: FAQ) => (
        <Space size="middle">
          <Button 
            type="link" 
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Editar
          </Button>
          <Button 
            type="link" 
            icon={<BarChartOutlined />}
            onClick={() => handleViewAnalytics(record)}
          >
            Analytics
          </Button>
          <Popconfirm
            title="Tem certeza que deseja excluir esta FAQ?"
            onConfirm={() => handleDelete(record.id)}
            okText="Sim"
            cancelText="Não"
          >
            <Button 
              type="link" 
              danger 
              icon={<DeleteOutlined />}
            >
              Excluir
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="faq-management">
      <div className="flex justify-between items-center mb-6">
        <Title level={3}>Gerenciamento de FAQ</Title>
        <Button 
          type="primary" 
          icon={<PlusOutlined />}
          onClick={() => setIsModalVisible(true)}
        >
          Adicionar FAQ
        </Button>
      </div>

      <Card>
        <div className="mb-4">
          <Search
            placeholder="Buscar FAQs..."
            allowClear
            enterButton
            onSearch={handleSearch}
            className="w-96"
          />
        </div>

        <Table
          columns={columns}
          dataSource={faqs}
          rowKey="id"
          loading={loading}
          pagination={{
            total: faqs.length,
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (total) => `Total de ${total} FAQs`,
          }}
        />
      </Card>

      <FAQModal
        visible={isModalVisible}
        faq={selectedFAQ}
        onCancel={() => {
          setIsModalVisible(false);
          setSelectedFAQ(null);
        }}
        onSuccess={(faq) => {
          if (selectedFAQ) {
            // Update
            setFaqs(faqs.map(f => f.id === faq.id ? faq : f));
          } else {
            // Create
            setFaqs([...faqs, faq]);
          }
          setIsModalVisible(false);
          setSelectedFAQ(null);
        }}
      />
    </div>
  );
};
```

### FAQModal.tsx
```tsx
interface FAQModalProps {
  visible: boolean;
  faq: FAQ | null;
  onCancel: () => void;
  onSuccess: (faq: FAQ) => void;
}

const FAQModal: React.FC<FAQModalProps> = ({ visible, faq, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [testMessage, setTestMessage] = useState('');
  const [testResults, setTestResults] = useState<FAQMatch[]>([]);

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      const payload = {
        ...values,
        keywords: values.keywords.split(',').map((k: string) => k.trim()),
        triggers: values.triggers.split(',').map((t: string) => t.trim()),
      };

      const result = faq 
        ? await updateFAQ(faq.id, payload)
        : await createFAQ(payload);

      message.success(`FAQ ${faq ? 'atualizada' : 'criada'} com sucesso!`);
      onSuccess(result);
    } catch (error) {
      message.error('Erro ao salvar FAQ');
    } finally {
      setLoading(false);
    }
  };

  const handleTestSearch = async () => {
    if (!testMessage.trim()) return;
    
    try {
      const results = await testFAQSearch(testMessage);
      setTestResults(results);
    } catch (error) {
      message.error('Erro ao testar busca');
    }
  };

  return (
    <Modal
      title={faq ? 'Editar FAQ' : 'Nova FAQ'}
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={800}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={faq ? {
          ...faq,
          keywords: faq.keywords.join(', '),
          triggers: faq.triggers.join(', '),
        } : {
          category: FAQCategory.GERAL,
          priority: FAQPriority.MEDIUM,
        }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="category"
              label="Categoria"
              rules={[{ required: true, message: 'Selecione uma categoria' }]}
            >
              <Select>
                {Object.values(FAQCategory).map(cat => (
                  <Option key={cat} value={cat}>{cat}</Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="priority"
              label="Prioridade"
              rules={[{ required: true, message: 'Selecione uma prioridade' }]}
            >
              <Select>
                {Object.values(FAQPriority).map(pri => (
                  <Option key={pri} value={pri}>{pri}</Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="question"
          label="Pergunta"
          rules={[{ required: true, message: 'Digite a pergunta' }]}
        >
          <Input.TextArea 
            rows={2}
            placeholder="Ex: Como faço para cancelar meu pedido?"
          />
        </Form.Item>

        <Form.Item
          name="answer"
          label="Resposta"
          rules={[{ required: true, message: 'Digite a resposta' }]}
        >
          <Input.TextArea 
            rows={4}
            placeholder="Resposta completa que a IA pode usar como base..."
          />
        </Form.Item>

        <Form.Item
          name="keywords"
          label="Palavras-chave (separadas por vírgula)"
        >
          <Input placeholder="cancelar, pedido, cancelamento, anular" />
        </Form.Item>

        <Form.Item
          name="triggers"
          label="Gatilhos (palavras que ativam esta FAQ)"
        >
          <Input placeholder="cancelar pedido, como cancelo, quero cancelar" />
        </Form.Item>

        {/* Teste de Busca */}
        <Card title="Testar Busca" size="small" className="mb-4">
          <Space.Compact style={{ width: '100%' }}>
            <Input
              value={testMessage}
              onChange={(e) => setTestMessage(e.target.value)}
              placeholder="Digite uma mensagem para testar..."
            />
            <Button type="primary" onClick={handleTestSearch}>
              Testar
            </Button>
          </Space.Compact>

          {testResults.length > 0 && (
            <div className="mt-4">
              <Title level={5}>Resultados:</Title>
              {testResults.map((result, index) => (
                <div key={index} className="border p-2 mb-2 rounded">
                  <div><strong>Confiança:</strong> {(result.confidenceScore * 100).toFixed(1)}%</div>
                  <div><strong>Pergunta:</strong> {result.faq.question}</div>
                </div>
              ))}
            </div>
          )}
        </Card>

        <div className="flex justify-end space-x-2">
          <Button onClick={onCancel}>Cancelar</Button>
          <Button type="primary" htmlType="submit" loading={loading}>
            {faq ? 'Atualizar' : 'Criar'}
          </Button>
        </div>
      </Form>
    </Modal>
  );
};
```

## 6. Migrations

### V11__create_faqs.sql
```sql
CREATE TABLE faqs (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    usage_count INTEGER DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE faq_keywords (
    faq_id BIGINT REFERENCES faqs(id) ON DELETE CASCADE,
    keyword VARCHAR(100) NOT NULL,
    PRIMARY KEY (faq_id, keyword)
);

CREATE TABLE faq_triggers (
    faq_id BIGINT REFERENCES faqs(id) ON DELETE CASCADE,
    trigger_phrase VARCHAR(200) NOT NULL,
    PRIMARY KEY (faq_id, trigger_phrase)
);

CREATE TABLE faq_matches (
    id BIGSERIAL PRIMARY KEY,
    faq_id BIGINT REFERENCES faqs(id) ON DELETE CASCADE,
    user_message TEXT NOT NULL,
    confidence_score DECIMAL(3,2) NOT NULL,
    was_used BOOLEAN DEFAULT FALSE,
    was_approved BOOLEAN,
    matched_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_faqs_category ON faqs(category);
CREATE INDEX idx_faqs_active ON faqs(is_active);
CREATE INDEX idx_faqs_success_rate ON faqs(success_rate DESC);
CREATE INDEX idx_faq_keywords_keyword ON faq_keywords(keyword);
CREATE INDEX idx_faq_matches_faq_id ON faq_matches(faq_id);
CREATE INDEX idx_faq_matches_confidence ON faq_matches(confidence_score DESC);

-- Full-text search
CREATE INDEX idx_faqs_question_fulltext ON faqs USING gin(to_tsvector('portuguese', question));
CREATE INDEX idx_faqs_answer_fulltext ON faqs USING gin(to_tsvector('portuguese', answer));
```

## 7. Integração no Admin Panel

### AdminTabs.tsx
```tsx
const AdminPanel = () => {
  return (
    <Tabs defaultActiveKey="faqs">
      <TabPane tab="FAQs" key="faqs" icon={<QuestionCircleOutlined />}>
        <FAQManagement />
      </TabPane>
      
      <TabPane tab="Configuração IA" key="ai-config" icon={<RobotOutlined />}>
        <AIConfiguration />
      </TabPane>
      
      <TabPane tab="Analytics" key="analytics" icon={<BarChartOutlined />}>
        <FAQAnalytics />
      </TabPane>
    </Tabs>
  );
};
```

## 8. Melhorias Futuras

1. **Busca Semântica**: Integração com embeddings para melhor matching
2. **Auto-categorização**: IA sugere categoria baseada na pergunta
3. **Bulk Import**: Upload de planilhas com FAQs
4. **Versionamento**: Histórico de mudanças nas FAQs
5. **A/B Testing**: Testar diferentes versões de respostas
6. **Machine Learning**: Otimização automática de scores baseada no feedback

Esta arquitetura permite que operadores gerenciem FAQs facilmente e a IA utilize essas informações de forma inteligente para gerar drafts mais precisos.