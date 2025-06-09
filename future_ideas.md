# Future Ideas - Rubia Chat

## Phone Number Lookup com Twilio

### Objetivo
Implementar validação real de números de telefone usando Twilio Lookup API para verificar se números são válidos e ativos antes de tentar enviar mensagens.

### Implementação Proposta

#### Backend (Java/Spring Boot)
```java
@Service
public class PhoneValidationService {
    
    private final Twilio twilio;
    
    public PhoneValidationResult validatePhone(String phoneNumber) {
        try {
            PhoneNumber number = PhoneNumber.fetcher(phoneNumber)
                .setFields(Arrays.asList("line_type_intelligence", "identity_match"))
                .fetch();
            
            return PhoneValidationResult.builder()
                .valid(number.getValid())
                .phoneNumber(number.getPhoneNumber().toString())
                .carrier(number.getLineTypeIntelligence().getCarrierName())
                .type(number.getLineTypeIntelligence().getType()) // mobile, landline, voip
                .build();
        } catch (Exception e) {
            return PhoneValidationResult.builder()
                .valid(false)
                .error(e.getMessage())
                .build();
        }
    }
}
```

#### Frontend (TypeScript)
```typescript
const lookupNumber = async (phoneNumber: string) => {
  try {
    const result = await customerApi.validatePhone(phoneNumber);
    
    return {
      valid: result.valid,
      phoneNumber: result.phoneNumber,
      carrier: result.carrier,
      type: result.type // mobile, landline, voip
    };
  } catch (error) {
    return { valid: false, error: error.message };
  }
};

// Uso no NewConversationModal
const createNewCustomer = async () => {
  const phone = searchQuery.trim();
  
  // Validar número antes de criar customer
  const validation = await lookupNumber(phone);
  if (!validation.valid) {
    message.error("Número inválido ou inativo");
    return;
  }
  
  // Continuar com criação...
};
```

### Pontos de Implementação

1. **Quando usar:**
   - Na criação de novos contatos (+ button)
   - Antes de enviar primeira mensagem para um número
   - Periodicamente para números inativos

2. **Cache/Otimização:**
   - Cache resultado por 30 dias
   - Tabela `phone_validations` com resultado e timestamp
   - Rate limiting para evitar custos excessivos

3. **Custos:**
   - ~$0.05 por lookup
   - Implementar cache inteligente
   - Apenas para números novos ou suspeitos

4. **Melhorias UX:**
   - Feedback visual sobre tipo de número (móvel/fixo)
   - Exibir operadora do número
   - Avisos para números VOIP ou suspeitos

### Benefícios

- ✅ Reduz mensagens falhando
- ✅ Identifica tipo de linha (móvel/fixo/VOIP)
- ✅ Melhora qualidade da base de contatos
- ✅ Feedback mais preciso para usuários

### Considerações

- ❌ Custo adicional por consulta
- ❌ Latência na criação de contatos
- ❌ Dependência externa (Twilio API)
- ❌ Rate limits a considerar

---

## Outras Ideias Futuras

*Adicionar outras melhorias aqui...*