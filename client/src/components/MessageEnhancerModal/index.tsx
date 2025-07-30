import React, { useState, useEffect } from "react";
import { Modal, Button, Spin, Card, message } from "antd";
import { Sparkles, Copy, Check, RefreshCw, Coins } from "lucide-react";
import { templateEnhancementService } from "../../services/templateEnhancementService";
import { useAuthStore } from "../../store/useAuthStore";

interface MessageEnhancerModalProps {
  show: boolean;
  originalMessage: string;
  templateCategory?: string;
  templateTitle?: string;
  templateId?: string; // ID do template se estiver editando um template existente
  onClose: () => void;
  onApply: (enhancedMessage: string) => void;
  onApplyWithHistory?: (enhancedMessage: string, aiMetadata: any) => void; // Callback para aplicar com histórico
}

const messageSuggestions = [
  {
    id: 'friendly',
    name: 'Mais Amigável',
    description: 'Tom caloroso e acolhedor',
    examples: {
      'oi': 'Olá! 😊 Como você está hoje?',
      'você pode vir amanhã?': 'Que tal vir nos visitar amanhã? Seria ótimo te ver! 😊',
      'temos horários disponíveis': 'Temos alguns horários especiais disponíveis só para você! ✨'
    }
  },
  {
    id: 'professional',
    name: 'Mais Profissional',
    description: 'Tom formal e técnico',
    examples: {
      'oi': 'Prezado(a), bom dia.',
      'você pode vir amanhã?': 'Gostaríamos de saber se seria possível sua presença amanhã.',
      'temos horários disponíveis': 'Informamos que possuímos disponibilidade de agendamento.'
    }
  },
  {
    id: 'empathetic',
    name: 'Mais Empático',
    description: 'Tom compreensivo e cuidadoso',
    examples: {
      'oi': 'Olá! Espero que esteja tudo bem com você. 💝',
      'você pode vir amanhã?': 'Entendo que sua agenda pode estar corrida, mas seria possível vir amanhã?',
      'temos horários disponíveis': 'Sabemos como é difícil encaixar compromissos, mas temos horários flexíveis que podem funcionar para você.'
    }
  },
  {
    id: 'urgent',
    name: 'Mais Urgente',
    description: 'Tom que transmite importância',
    examples: {
      'oi': 'Olá! Preciso falar com você sobre algo importante.',
      'você pode vir amanhã?': 'Seria fundamental sua presença amanhã - estamos com necessidade urgente.',
      'temos horários disponíveis': 'ATENÇÃO: Temos horários limitados disponíveis por pouco tempo!'
    }
  },
  {
    id: 'motivational',
    name: 'Mais Motivacional',
    description: 'Tom inspirador e encorajador',
    examples: {
      'oi': 'Olá, herói! 🦸‍♀️ Pronto para fazer a diferença hoje?',
      'você pode vir amanhã?': 'Que tal ser um herói amanhã e salvar vidas com sua doação?',
      'temos horários disponíveis': 'Temos horários perfeitos para você brilhar e salvar vidas! ⭐'
    }
  }
];

export const MessageEnhancerModal: React.FC<MessageEnhancerModalProps> = ({
  show,
  originalMessage,
  templateCategory = 'primeira-doacao',
  templateTitle,
  templateId,
  onClose,
  onApply,
  onApplyWithHistory,
}) => {
  const { user } = useAuthStore();
  const [isGenerating, setIsGenerating] = useState(false);
  const [suggestions, setSuggestions] = useState<Array<{
    id: string;
    name: string;
    description: string;
    enhancedMessage: string;
    aiModelUsed?: string;
    tokensUsed?: number;
    creditsConsumed?: number;
    explanation?: string;
  }>>([]);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Gerar sugestões usando IA real do banco de dados
  const generateSuggestions = React.useCallback(async () => {
    if (!originalMessage.trim() || !user?.companyId) return;

    setIsGenerating(true);
    
    try {
      const enhancementPromises = messageSuggestions.map(async (suggestion) => {
        try {
          const response = await templateEnhancementService.enhanceTemplate({
            companyId: user.companyId,
            originalContent: originalMessage,
            enhancementType: suggestion.id as any,
            category: templateCategory,
            title: templateTitle,
          });

          return {
            id: suggestion.id,
            name: suggestion.name,
            description: suggestion.description,
            enhancedMessage: response.enhancedContent,
            aiModelUsed: response.aiModelUsed,
            tokensUsed: response.tokensUsed,
            creditsConsumed: response.creditsConsumed,
            explanation: response.explanation,
          };
        } catch (error) {
          console.error(`Error enhancing with ${suggestion.id}:`, error);
          // Fallback para simulação em caso de erro
          return {
            id: suggestion.id,
            name: suggestion.name,
            description: suggestion.description,
            enhancedMessage: simulateFallback(originalMessage, suggestion.id),
            aiModelUsed: 'Simulação (erro na IA)',
            tokensUsed: 0,
            creditsConsumed: 0,
            explanation: 'Falhou ao conectar com IA, usando simulação',
          };
        }
      });

      const enhancedSuggestions = await Promise.all(enhancementPromises);
      setSuggestions(enhancedSuggestions);
      
    } catch (error) {
      console.error('Error generating suggestions:', error);
      message.error('Erro ao gerar sugestões com IA. Tente novamente.');
    } finally {
      setIsGenerating(false);
    }
  }, [originalMessage, user?.companyId, templateCategory, templateTitle]);

  // Função de fallback para simulação em caso de erro
  const simulateFallback = (message: string, enhancementType: string): string => {
    switch (enhancementType) {
      case 'friendly':
        return addFriendlyTouch(message);
      case 'professional':
        return makeProfessional(message);
      case 'empathetic':
        return addEmpathy(message);
      case 'urgent':
        return addUrgency(message);
      case 'motivational':
        return addMotivation(message);
      default:
        return message + ' (melhorado)';
    }
  };

  // Funções de melhoramento de mensagem (simulação simples)
  const addFriendlyTouch = (message: string): string => {
    const friendlyPrefixes = ['Oi! 😊 ', 'Olá! ', 'Oi, querido(a)! '];
    const friendlySuffixes = [' 😊', ' 💝', ' ✨'];
    
    let enhanced = message;
    if (!enhanced.match(/^(oi|olá|bom dia)/i)) {
      enhanced = friendlyPrefixes[Math.floor(Math.random() * friendlyPrefixes.length)] + enhanced;
    }
    if (!enhanced.includes('😊') && !enhanced.includes('💝') && !enhanced.includes('✨') && !enhanced.includes('❤️')) {
      enhanced += friendlySuffixes[Math.floor(Math.random() * friendlySuffixes.length)];
    }
    return enhanced;
  };

  const makeProfessional = (message: string): string => {
    let enhanced = message;
    enhanced = enhanced.replace(/\boi\b/gi, 'Prezado(a)');
    enhanced = enhanced.replace(/\btchau\b/gi, 'Atenciosamente');
    enhanced = enhanced.replace(/\bvc\b/gi, 'o(a) senhor(a)');
    enhanced = enhanced.replace(/\bpode\b/gi, 'poderia');
    enhanced = enhanced.replace(/\bquer\b/gi, 'gostaria');
    
    if (!enhanced.match(/^(prezado|senhor|senhora)/i)) {
      enhanced = 'Prezado(a), ' + enhanced.toLowerCase();
    }
    return enhanced;
  };

  const addEmpathy = (message: string): string => {
    const empathyPhrases = [
      'Entendo que pode ser difícil, mas ',
      'Sei como sua agenda pode estar corrida, porém ',
      'Compreendo suas preocupações, e ',
      'Sabemos que é uma decisão importante, '
    ];
    
    let enhanced = message;
    if (!enhanced.match(/^(entendo|sei|compreendo|sabemos)/i)) {
      enhanced = empathyPhrases[Math.floor(Math.random() * empathyPhrases.length)] + enhanced.toLowerCase();
    }
    enhanced += ' Estamos aqui para ajudar no que precisar. 💝';
    return enhanced;
  };

  const addUrgency = (message: string): string => {
    let enhanced = message.toUpperCase();
    enhanced = 'IMPORTANTE: ' + enhanced;
    enhanced += ' ⚠️ Ação necessária!';
    return enhanced;
  };

  const addMotivation = (message: string): string => {
    const motivationalPrefixes = [
      'Você é incrível! ',
      'Que tal ser um herói hoje? ',
      'Sua ajuda pode salvar vidas! ',
      'Cada gesto conta muito! '
    ];
    
    let enhanced = motivationalPrefixes[Math.floor(Math.random() * motivationalPrefixes.length)] + message;
    enhanced += ' Você faz a diferença! 🌟';
    return enhanced;
  };

  const handleCopy = async (id: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch (err) {
      console.error('Erro ao copiar:', err);
    }
  };

  const handleApplySuggestion = async (suggestion: any) => {
    // Se for um template existente e tiver callback para histórico, usar o novo endpoint
    if (templateId && onApplyWithHistory && user?.id) {
      try {
        const aiMetadata = {
          templateId,
          content: suggestion.enhancedMessage,
          userId: user.id,
          aiAgentId: suggestion.aiAgentId,
          aiEnhancementType: suggestion.id,
          aiTokensUsed: suggestion.tokensUsed,
          aiCreditsConsumed: suggestion.creditsConsumed,
          aiModelUsed: suggestion.aiModelUsed,
          aiExplanation: suggestion.explanation
        };
        
        onApplyWithHistory(suggestion.enhancedMessage, aiMetadata);
      } catch (error) {
        console.error('Error applying suggestion with history:', error);
        // Fallback para aplicação normal
        onApply(suggestion.enhancedMessage);
      }
    } else {
      // Aplicação normal (para templates novos ou sem histórico)
      onApply(suggestion.enhancedMessage);
    }
  };

  useEffect(() => {
    if (show && originalMessage.trim()) {
      generateSuggestions();
    }
  }, [show, originalMessage, generateSuggestions]);

  return (
    <Modal
      title={
        <div className="flex items-center gap-3">
          <Sparkles className="w-6 h-6 text-purple-500" />
          <div>
            <h3 className="text-lg font-semibold m-0">Melhorar Mensagem</h3>
            <p className="text-sm text-gray-500 m-0">IA vai sugerir versões aprimoradas</p>
          </div>
        </div>
      }
      open={show}
      onCancel={onClose}
      footer={null}
      width={700}
      className="message-enhancer-modal"
    >
      <div className="space-y-4">
        <Card className="bg-gray-50">
          <h4 className="text-sm font-medium text-gray-700 mb-2">Mensagem Original:</h4>
          <p className="text-gray-900 italic">"{originalMessage}"</p>
        </Card>

        {isGenerating ? (
          <div className="text-center py-8">
            <Spin size="large" />
            <p className="text-gray-600 mt-4">IA analisando sua mensagem...</p>
          </div>
        ) : (
          <div className="space-y-3">
            {/* Aviso quando usando modelo padrão */}
            {suggestions.some(s => s.aiModelUsed?.includes('(Padrão)')) && (
              <div className="bg-orange-50 border border-orange-200 rounded-lg p-3">
                <div className="flex items-start gap-2">
                  <span className="text-orange-500 text-lg">⚠️</span>
                  <div className="text-sm">
                    <p className="font-medium text-orange-800 m-0">Usando modelo padrão do sistema</p>
                    <p className="text-orange-700 m-0 mt-1">
                      Para melhor personalização, configure um agente de IA específico para sua empresa na aba "Agente" desta página.
                    </p>
                  </div>
                </div>
              </div>
            )}
            
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-medium text-gray-700">Sugestões de Melhoria:</h4>
              <Button 
                size="small" 
                icon={<RefreshCw className="w-4 h-4" />}
                onClick={generateSuggestions}
              >
                Regenerar
              </Button>
            </div>

            {suggestions.map((suggestion) => (
              <Card key={suggestion.id} className="hover:shadow-md transition-shadow">
                <div className="flex justify-between items-start mb-2">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h5 className="font-medium text-gray-900 m-0">{suggestion.name}</h5>
                      {suggestion.creditsConsumed !== undefined && suggestion.creditsConsumed > 0 && (
                        <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-700">
                          <Coins className="w-3 h-3" />
                          {suggestion.creditsConsumed} créditos
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-500 m-0 mb-1">{suggestion.description}</p>
                    {suggestion.aiModelUsed && (
                      <div className="text-xs m-0">
                        <p className={`m-0 ${suggestion.aiModelUsed.includes('(Padrão)') ? 'text-orange-600' : 'text-blue-600'}`}>
                          ✨ Gerado por: {suggestion.aiModelUsed}
                          {suggestion.tokensUsed && ` (${suggestion.tokensUsed} tokens)`}
                        </p>
                        {suggestion.aiModelUsed.includes('(Padrão)') && (
                          <p className="text-orange-500 m-0 mt-1 font-medium">
                            ⚠️ Usando modelo padrão - Configure seu agente IA para melhor personalização
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                  <div className="flex gap-2 ml-2">
                    <Button
                      size="small"
                      icon={copiedId === suggestion.id ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                      onClick={() => handleCopy(suggestion.id, suggestion.enhancedMessage)}
                      className={copiedId === suggestion.id ? 'text-green-600' : ''}
                    >
                      {copiedId === suggestion.id ? 'Copiado' : 'Copiar'}
                    </Button>
                    <Button
                      type="primary"
                      size="small"
                      onClick={() => handleApplySuggestion(suggestion)}
                    >
                      Usar Esta
                    </Button>
                  </div>
                </div>
                <div className="bg-blue-50 p-3 rounded border border-blue-200 mb-2">
                  <p className="text-gray-900 m-0">"{suggestion.enhancedMessage}"</p>
                </div>
                {suggestion.explanation && (
                  <div className="text-xs text-gray-600 bg-gray-50 p-2 rounded">
                    <strong>Melhorias aplicadas:</strong> {suggestion.explanation}
                  </div>
                )}
              </Card>
            ))}
          </div>
        )}

        <div className="flex justify-end gap-3 pt-4 border-t">
          <Button size="large" onClick={onClose}>
            Cancelar
          </Button>
          <Button 
            type="primary" 
            size="large" 
            onClick={() => onApply(originalMessage)}
          >
            Usar Original
          </Button>
        </div>
      </div>
    </Modal>
  );
};