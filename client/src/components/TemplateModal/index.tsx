import React, { useState, useEffect } from "react";
import { Modal, Input, Select, Button, Card } from "antd";
import { MessageEnhancerModal } from "../MessageEnhancerModal";

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
  { value: 'geral', label: 'Geral' },
  { value: 'primeira-doacao', label: 'Primeira Doação' },
  { value: 'retorno', label: 'Doador de Retorno' },
  { value: 'urgencia', label: 'Urgência' },
  { value: 'campanhas', label: 'Campanhas Especiais' },
  { value: 'agradecimento', label: 'Agradecimento' },
  { value: 'motivacional', label: 'Motivacional' },
  { value: 'corporativo', label: 'Corporativo' }
];

export const TemplateModal: React.FC<TemplateModalProps> = ({
  show,
  template,
  onClose,
  onSave,
}) => {
  const [formData, setFormData] = useState({
    title: "",
    content: "",
    category: "geral"
  });
  const [showEnhancer, setShowEnhancer] = useState(false);

  useEffect(() => {
    if (template) {
      setFormData({
        title: template.title,
        content: template.content,
        category: template.category || "geral"
      });
    } else {
      setFormData({
        title: "",
        content: "",
        category: "geral"
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
      category: "geral"
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
              placeholder="Digite a mensagem que será enviada aos doadores..."
              rows={6}
              showCount
              maxLength={500}
            />
            <p className="text-xs text-gray-500 mt-1">
              Dica: Use uma linguagem acolhedora e motivadora para engajar os doadores
            </p>
          </div>

          <Card className="bg-blue-50 border-blue-200">
            <div className="text-sm">
              <h4 className="font-medium text-blue-800 mb-2">💡 Dicas para um bom template:</h4>
              <ul className="text-blue-700 space-y-1">
                <li>• Use tom amigável e acolhedor</li>
                <li>• Inclua informações relevantes sobre doação</li>
                <li>• Termine com uma pergunta ou call-to-action</li>
                <li>• Considere usar emojis para tornar mais caloroso</li>
                <li>• Mantenha entre 100-300 caracteres para melhor engajamento</li>
              </ul>
            </div>
          </Card>

          <div className="flex gap-3 pt-4">
            <Button
              type="primary"
              size="large"
              onClick={handleSave}
              disabled={!formData.title.trim() || !formData.content.trim()}
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
        onClose={() => setShowEnhancer(false)}
        onApply={handleApplyEnhancedContent}
      />
    </>
  );
};