import React, { useState, useEffect } from 'react';
import {
  Button,
  Typography,
  Modal,
  Input,
  message,
  Tooltip,
  Progress
} from 'antd';
import {
  CheckCircle,
  XCircle,
  Edit3,
  MessageSquare,
  Zap,
  Copy,
  AlertCircle
} from 'lucide-react';
import { draftService, type MessageDraft } from '../../services/draftService';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

interface DraftPanelProps {
  conversationId: string;
  lastUserMessage?: string;
  onDraftSelected?: (content: string) => void;
  onDraftApproved?: () => void;
  className?: string;
}

export const DraftPanel: React.FC<DraftPanelProps> = ({
  conversationId,
  lastUserMessage,
  onDraftSelected,
  onDraftApproved,
  className = ''
}) => {
  const [currentDraft, setCurrentDraft] = useState<MessageDraft | null>(null);
  const [loading, setLoading] = useState(false);
  const [editingDraft, setEditingDraft] = useState<MessageDraft | null>(null);
  const [editedContent, setEditedContent] = useState('');
  const [rejecting, setRejecting] = useState<MessageDraft | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');

  // Gerar draft quando nova mensagem chegar
  const generateDraft = async () => {
    if (!conversationId || !lastUserMessage) return;
    
    setLoading(true);
    try {
      const draft = await draftService.generateDraft({
        conversationId,
        userMessage: lastUserMessage
      });
      
      if (draft) {
        setCurrentDraft(draft);
      }
    } catch (error) {
      console.error('Erro ao gerar draft:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (lastUserMessage) {
      generateDraft();
    }
  }, [lastUserMessage, conversationId]);

  // Usar draft - preenche na área de input
  const handleUseDraft = () => {
    if (currentDraft && onDraftSelected) {
      onDraftSelected(currentDraft.content);
      setCurrentDraft(null); // Remove a sugestão após usar
    }
  };

  // Aprovar e enviar diretamente
  const handleApproveDraft = async () => {
    if (!currentDraft) return;
    
    try {
      await draftService.approveDraft(currentDraft.id, true);
      message.success('Mensagem enviada!');
      setCurrentDraft(null);
      onDraftApproved?.();
    } catch (error) {
      console.error('Erro ao enviar mensagem:', error);
      message.error('Erro ao enviar mensagem');
    }
  };

  // Editar draft
  const handleEditDraft = () => {
    if (!currentDraft) return;
    setEditingDraft(currentDraft);
    setEditedContent(currentDraft.content);
  };

  // Confirmar edição
  const handleConfirmEdit = async () => {
    if (!editingDraft || !editedContent.trim()) return;

    try {
      await draftService.editAndApproveDraft(editingDraft.id, editedContent, true);
      message.success('Mensagem editada e enviada!');
      setEditingDraft(null);
      setEditedContent('');
      setCurrentDraft(null);
      onDraftApproved?.();
    } catch (error) {
      console.error('Erro ao editar mensagem:', error);
      message.error('Erro ao editar mensagem');
    }
  };

  // Rejeitar/descartar draft
  const handleRejectDraft = async () => {
    if (!currentDraft) return;

    try {
      await draftService.rejectDraft(currentDraft.id, 'Descartado pelo operador');
      setCurrentDraft(null);
    } catch (error) {
      console.error('Erro ao descartar draft:', error);
    }
  };

  // Confirmar rejeição com motivo
  const handleConfirmReject = async () => {
    if (!rejecting) return;

    try {
      await draftService.rejectDraft(rejecting.id, rejectionReason || 'Descartado pelo operador');
      setCurrentDraft(null);
      setRejecting(null);
      setRejectionReason('');
    } catch (error) {
      console.error('Erro ao rejeitar draft:', error);
    }
  };

  // Se não há draft ou está carregando, não mostrar
  if (!currentDraft && !loading) {
    return null;
  }

  return (
    <>
      {/* Sugestão de Draft */}
      {(loading || currentDraft) && (
        <div className={`draft-suggestion bg-blue-50 border border-blue-200 rounded-lg p-4 mb-3 ${className}`}>
          {loading ? (
            <div className="flex items-center gap-2">
              <div className="animate-spin">
                <Zap className="w-4 h-4 text-blue-500" />
              </div>
              <Text className="text-blue-700">IA gerando resposta baseada em FAQs...</Text>
            </div>
          ) : currentDraft && (
            <div>
              {/* Header */}
              <div className="flex items-center gap-2 mb-2">
                <span className="text-lg">
                  {draftService.getSourceIcon(currentDraft.sourceType)}
                </span>
                <Text strong className="text-blue-800">
                  IA sugere resposta
                </Text>
                {currentDraft.confidence && (
                  <Tooltip title={`Confiança: ${draftService.formatConfidence(currentDraft.confidence)}`}>
                    <Progress 
                      percent={Math.round(currentDraft.confidence * 100)} 
                      size="small" 
                      showInfo={false}
                      className="w-20"
                    />
                  </Tooltip>
                )}
                <Text type="secondary" className="text-xs ml-auto">
                  {currentDraft.sourceType === 'FAQ' ? 'Baseado em FAQ' : 
                   currentDraft.sourceType === 'TEMPLATE' ? 'Baseado em Template' : 
                   currentDraft.sourceType === 'AI_CONTEXTUAL' ? 'IA Contextualizada' :
                   'Gerado pela IA'}
                </Text>
              </div>

              {/* Conteúdo da Sugestão */}
              <div className="bg-white border border-blue-200 rounded p-3 mb-3">
                <Paragraph className="mb-0 text-sm">
                  {currentDraft.content}
                </Paragraph>
              </div>

              {/* Ações */}
              <div className="flex gap-2">
                <Button 
                  type="primary" 
                  size="small"
                  icon={<CheckCircle className="w-3 h-3" />}
                  onClick={handleApproveDraft}
                  className="bg-green-500 hover:bg-green-600 border-green-500"
                >
                  Enviar Agora
                </Button>
                
                <Button 
                  size="small"
                  icon={<Copy className="w-3 h-3" />}
                  onClick={handleUseDraft}
                >
                  Usar no Input
                </Button>
                
                <Button 
                  size="small"
                  icon={<Edit3 className="w-3 h-3" />}
                  onClick={handleEditDraft}
                >
                  Editar
                </Button>
                
                <Button 
                  size="small"
                  icon={<XCircle className="w-3 h-3" />}
                  onClick={handleRejectDraft}
                  className="ml-auto"
                >
                  Descartar
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal de Edição */}
      <Modal
        title="Editar Draft"
        open={!!editingDraft}
        onOk={handleConfirmEdit}
        onCancel={() => {
          setEditingDraft(null);
          setEditedContent('');
        }}
        okText="Enviar Editado"
        cancelText="Cancelar"
        width={600}
      >
        {editingDraft && (
          <div className="space-y-4">
            <div className="p-3 bg-gray-50 rounded">
              <Text type="secondary" className="text-sm">
                <MessageSquare className="w-4 h-4 inline mr-1" />
                Mensagem original do cliente:
              </Text>
              <div className="mt-1 text-sm">
                "{editingDraft.originalMessage || 'Não disponível'}"
              </div>
            </div>
            
            <div>
              <Text strong>Editar resposta:</Text>
              <TextArea
                value={editedContent}
                onChange={(e) => setEditedContent(e.target.value)}
                rows={6}
                placeholder="Digite sua resposta..."
                className="mt-2"
              />
            </div>
          </div>
        )}
      </Modal>

      {/* Modal de Rejeição */}
      <Modal
        title="Rejeitar Draft"
        open={!!rejecting}
        onOk={handleConfirmReject}
        onCancel={() => {
          setRejecting(null);
          setRejectionReason('');
        }}
        okText="Rejeitar"
        cancelText="Cancelar"
        okButtonProps={{ danger: true }}
      >
        <div className="space-y-4">
          <div className="flex items-start gap-2 p-3 bg-orange-50 rounded">
            <AlertCircle className="w-4 h-4 text-orange-500 mt-0.5" />
            <Text type="secondary" className="text-sm">
              Este draft será rejeitado e não será enviado. Você pode opcionalmente informar o motivo da rejeição.
            </Text>
          </div>
          
          <div>
            <Text>Motivo da rejeição (opcional):</Text>
            <TextArea
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              rows={3}
              placeholder="Ex: Resposta não adequada ao contexto..."
              className="mt-2"
            />
          </div>
        </div>
      </Modal>
    </>
  );
};