import React, { useState, useEffect } from "react";
import { Modal, Input, Select, Button, Card, Alert, message } from "antd";
import { MessageEnhancerModal } from "../MessageEnhancerModal";
import { templateEnhancementService } from "../../services/templateEnhancementService";
import { useAuthStore } from "../../store/useAuthStore";

const { TextArea } = Input;
const { Option } = Select;

interface TemplateModalProps {
  show: boolean;
  template?: {
    id: string;
    title: string;
    content: string;
    category?: string;
  } | null;
  onClose: () => void;
  onSave: (templateData: { title: string; content: string; category: string }) => void;
}

const categories = [
  { value: 'primeira-doacao', label: '🩸 Primeira Doação' },
  { value: 'retorno', label: '🔄 Doador de Retorno' },
  { value: 'agendamento', label: '📅 Agendamento' },
  { value: 'urgencia', label: '🚨 Urgência - Estoque Baixo' },
  { value: 'campanhas', label: '📢 Campanhas Especiais' },
  { value: 'agradecimento', label: '🙏 Agradecimento Pós-Doação' },
  { value: 'motivacional', label: '⭐ Motivacional - Seja um Herói' },
  { value: 'corporativo', label: '🏢 Empresas Parceiras' },
  { value: 'fidelizacao', label: '💝 Fidelização de Doadores' }
];

export const TemplateModal: React.FC<TemplateModalProps> = ({
  show,
  template,
  onClose,
  onSave,
}) => {
  const { user } = useAuthStore();
  const [formData, setFormData] = useState({
    title: "",
    content: "",
    category: "primeira-doacao"
  });
  const [showEnhancer, setShowEnhancer] = useState(false);

  useEffect(() => {
    if (template) {
      setFormData({
        title: template.title,
        content: template.content,
        category: template.category || "primeira-doacao"
      });
    } else {
      setFormData({
        title: "",
        content: "",
        category: "primeira-doacao"
      });
    }
  }, [template, show]);

  const handleSave = () => {
    if (!formData.title.trim() || !formData.content.trim()) {
      return;
    }

    onSave(formData);
    handleClose();
  };

  const handleClose = () => {
    setFormData({
      title: "",
      content: "",
      category: "primeira-doacao"
    });
    onClose();
  };

  const handleEnhanceContent = () => {
    if (formData.content.trim()) {
      setShowEnhancer(true);
    }
  };

  const handleApplyEnhancedContent = (enhancedContent: string) => {
    setFormData(prev => ({ ...prev, content: enhancedContent }));
    setShowEnhancer(false);
  };

  const handleApplyEnhancedContentWithHistory = async (enhancedContent: string, aiMetadata: any) => {
    if (!template?.id || !user?.id) {
      // Fallback para aplicação normal se não houver template ID ou user
      handleApplyEnhancedContent(enhancedContent);
      return;
    }

    try {
      message.loading('Salvando template com histórico de IA...', 0);
      
      // Salvar diretamente no backend com metadados de IA
      await templateEnhancementService.saveTemplateWithAIMetadata(aiMetadata);
      
      message.destroy();
      message.success('Template atualizado com histórico de IA!');
      
      // Atualizar o formulário local
      setFormData(prev => ({ ...prev, content: enhancedContent }));
      setShowEnhancer(false);
      
      // Chamar onSave para atualizar a lista no componente pai
      onSave({ title: formData.title, content: enhancedContent, category: formData.category });
      
    } catch (error) {
      message.destroy();
      message.error('Erro ao salvar template com histórico de IA');
      console.error('Error saving with AI metadata:', error);
      
      // Fallback para aplicação normal
      handleApplyEnhancedContent(enhancedContent);
    }
  };

  return (
    <>
      <Modal
        title={template ? "Editar Template" : "Novo Template"}
        open={show}
        onCancel={handleClose}
        footer={null}
        width={600}
      >
        <div className="space-y-6 pt-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Título do Template *
            </label>
            <Input
              value={formData.title}
              onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
              placeholder="Ex: Convite para primeira doação"
              size="large"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Categoria
            </label>
            <Select
              value={formData.category}
              onChange={(value) => setFormData(prev => ({ ...prev, category: value }))}
              className="w-full"
              size="large"
            >
              {categories.map(cat => (
                <Option key={cat.value} value={cat.value}>
                  {cat.label}
                </Option>
              ))}
            </Select>
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-gray-700">
                Conteúdo da Mensagem *
              </label>
              {formData.content.trim() && (
                <Button
                  type="link"
                  size="small"
                  className="text-purple-600 hover:text-purple-800"
                  onClick={handleEnhanceContent}
                >
                  ✨ Melhorar com IA
                </Button>
              )}
            </div>
            <TextArea
              value={formData.content}
              onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
              placeholder="Digite a mensagem que será enviada aos doadores...&#10;Dica: Use {{nome}} para personalizar com o nome do doador"
              rows={6}
              showCount
              maxLength={500}
            />
            <div className="space-y-2 mt-2">
              <div className="flex items-start gap-2">
                <span className="text-orange-500 text-sm">💡</span>
                <div className="text-xs text-gray-600">
                  <p className="m-0 font-medium">Personalização obrigatória:</p>
                  <p className="m-0">Use <code className="bg-gray-100 px-1 rounded text-orange-600">{{nome}}</code> para personalizar com o nome do doador</p>
                </div>
              </div>
              {formData.content && !formData.content.includes('{{nome}}') && (
                <Alert
                  message="Placeholder obrigatório"
                  description="Inclua {{nome}} na sua mensagem para personalizá-la com o nome do doador"
                  type="warning"
                  size="small"
                  showIcon
                />
              )}
              {formData.content.includes('{{nome}}') && (
                <div className="bg-green-50 border border-green-200 rounded p-2">
                  <div className="text-xs text-green-700">
                    <p className="m-0 font-medium">✅ Preview personalizado:</p>
                    <p className="m-0 italic mt-1">
                      "{formData.content.replace('{{nome}}', 'João Silva')}"
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>

          <Card className="bg-blue-50 border-blue-200">
            <div className="text-sm">
              <h4 className="font-medium text-blue-800 mb-2">🩸 Dicas para captação de doadores:</h4>
              <ul className="text-blue-700 space-y-1">
                <li>• <strong>Personalize sempre:</strong> Use {{nome}} para tornar a mensagem mais próxima</li>
                <li>• <strong>Apele ao heroísmo:</strong> Mostre como a doação salva vidas</li>
                <li>• <strong>Seja respeitoso:</strong> Entenda que doar é um ato voluntário</li>
                <li>• <strong>Transmita urgência ética:</strong> Destaque a necessidade sem pressionar</li>
                <li>• <strong>Inclua call-to-action:</strong> Convide para agendamento específico</li>
                <li>• <strong>Use tom adequado:</strong> Formal mas caloroso para fidelizar doadores</li>
              </ul>
            </div>
          </Card>

          <div className="flex gap-3 pt-4">
            <Button
              type="primary"
              size="large"
              onClick={handleSave}
              disabled={!formData.title.trim() || !formData.content.trim() || !formData.content.includes('{{nome}}')}
              className="flex-1"
            >
              {template ? "Atualizar Template" : "Criar Template"}
            </Button>
            <Button
              size="large"
              onClick={handleClose}
            >
              Cancelar
            </Button>
          </div>
        </div>
      </Modal>

      <MessageEnhancerModal
        show={showEnhancer}
        originalMessage={formData.content}
        templateCategory={formData.category}
        templateTitle={formData.title}
        templateId={template?.id}
        onClose={() => setShowEnhancer(false)}
        onApply={handleApplyEnhancedContent}
        onApplyWithHistory={handleApplyEnhancedContentWithHistory}
      />
    </>
  );
};