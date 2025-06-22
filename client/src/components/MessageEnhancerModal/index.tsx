import React, { useState, useEffect } from "react";
import { Modal, Button, Spin, Card } from "antd";
import { Sparkles, Copy, Check, RefreshCw } from "lucide-react";

interface MessageEnhancerModalProps {
  show: boolean;
  originalMessage: string;
  onClose: () => void;
  onApply: (enhancedMessage: string) => void;
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
  onClose,
  onApply,
}) => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [suggestions, setSuggestions] = useState<Array<{
    id: string;
    name: string;
    description: string;
    enhancedMessage: string;
  }>>([]);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Simular geração de sugestões baseadas na mensagem
  const generateSuggestions = React.useCallback(async () => {
    if (!originalMessage.trim()) return;

    setIsGenerating(true);
    
    // Simular delay da IA
    await new Promise(resolve => setTimeout(resolve, 1500));

    const enhancedSuggestions = messageSuggestions.map(suggestion => {
      let enhancedMessage = originalMessage;
      
      // Simulação simples de melhoramento baseado no tipo
      switch (suggestion.id) {
        case 'friendly':
          enhancedMessage = addFriendlyTouch(originalMessage);
          break;
        case 'professional':
          enhancedMessage = makeProfessional(originalMessage);
          break;
        case 'empathetic':
          enhancedMessage = addEmpathy(originalMessage);
          break;
        case 'urgent':
          enhancedMessage = addUrgency(originalMessage);
          break;
        case 'motivational':
          enhancedMessage = addMotivation(originalMessage);
          break;
        default:
          enhancedMessage = originalMessage;
      }

      return {
        id: suggestion.id,
        name: suggestion.name,
        description: suggestion.description,
        enhancedMessage
      };
    });

    setSuggestions(enhancedSuggestions);
    setIsGenerating(false);
  }, [originalMessage]);

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
                  <div>
                    <h5 className="font-medium text-gray-900 m-0">{suggestion.name}</h5>
                    <p className="text-xs text-gray-500 m-0">{suggestion.description}</p>
                  </div>
                  <div className="flex gap-2">
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
                      onClick={() => onApply(suggestion.enhancedMessage)}
                    >
                      Usar Esta
                    </Button>
                  </div>
                </div>
                <div className="bg-blue-50 p-3 rounded border border-blue-200">
                  <p className="text-gray-900 m-0">"{suggestion.enhancedMessage}"</p>
                </div>
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