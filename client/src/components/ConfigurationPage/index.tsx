import React, { useState, useEffect, useCallback } from "react";
import {
  ArrowLeft,
  Upload,
  User,
  Check,
  Plus,
  Edit3,
  Sparkles,
  Trash2,
  MoreVertical,
  History,
  RotateCcw,
} from "lucide-react";
import {
  Button,
  Select,
  Input,
  Upload as AntUpload,
  DatePicker,
  message,
  Radio,
  Dropdown,
  Modal,
  Timeline,
  Badge,
  Spin,
} from "antd";
import type { UploadProps, RadioChangeEvent } from "antd";
import dayjs from "dayjs";
import { TemplateModal } from "../TemplateModal";
import { AvatarUpload } from "../AvatarUpload";
import type { ConversationTemplate } from "../../types/types";
import { campaignService, type CampaignData } from "../../services/campaignService";

interface ExtendedCampaignData extends Omit<CampaignData, 'templateIds'> {
  file?: File;
  templateIds?: string[];
}
import {
  messageTemplateService,
  type CreateMessageTemplateRequest,
  type UpdateMessageTemplateRequest,
  type MessageTemplateRevision,
  type RevisionType,
} from "../../services/messageTemplateService";
import { useAuthStore } from "../../store/useAuthStore";
import { aiAgentApi, type AIAgent } from "../../api/services/aiAgentApi";
import { aiModelService } from "../../services/aiModelService";

const { Option } = Select;
const { TextArea } = Input;
const { RangePicker } = DatePicker;

interface AgentConfig {
  name: string;
  description: string;
  avatarBase64: string;
  aiModelId: string;
  temperament: string;
  maxResponseLength: number;
  temperature: number;
  isActive: boolean;
}

interface ConfigurationPageProps {
  onBack: () => void;
}

export const ConfigurationPage: React.FC<ConfigurationPageProps> = ({
  onBack,
}) => {
  const { user } = useAuthStore();

  const [activeTab, setActiveTab] = useState<
    "agent" | "campaign" | "templates" | "deleted"
  >("agent");
  const [agentConfig, setAgentConfig] = useState<AgentConfig>({
    name: "Sofia",
    description: "",
    avatarBase64: "",
    aiModelId: "default", // Use default model for MVP
    temperament: "AMIGAVEL",
    maxResponseLength: 500,
    temperature: 0.7,
    isActive: true,
  });
  const [existingAgents, setExistingAgents] = useState<AIAgent[]>([]);
  const [selectedAgent, setSelectedAgent] = useState<AIAgent | null>(null);
  const [showAgentModal, setShowAgentModal] = useState(false);
  const [canCreateAgent, setCanCreateAgent] = useState(true);
  const [remainingSlots, setRemainingSlots] = useState(1);
  const [campaignData, setCampaignData] = useState<ExtendedCampaignData>({
    name: "",
    description: "",
    startDate: "",
    endDate: "",
    sourceSystem: "",
  });
  const [templates, setTemplates] = useState<ConversationTemplate[]>([]);
  const [deletedTemplates, setDeletedTemplates] = useState<ConversationTemplate[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [duplicateUsers, setDuplicateUsers] = useState<string[]>([]);
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [editingTemplate, setEditingTemplate] =
    useState<ConversationTemplate | null>(null);
  const [isLoadingTemplates, setIsLoadingTemplates] = useState(false);
  const [isLoadingDeletedTemplates, setIsLoadingDeletedTemplates] = useState(false);
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [revisionHistory, setRevisionHistory] = useState<
    MessageTemplateRevision[]
  >([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  // Carregar templates da API
  const loadTemplates = useCallback(async () => {
    setIsLoadingTemplates(true);
    try {
      // Carregar todos os templates ou apenas da empresa do usuário
      const apiTemplates = user?.companyId
        ? await messageTemplateService.getByCompany(user.companyId)
        : await messageTemplateService.getAll();

      const convertedTemplates: ConversationTemplate[] = apiTemplates.map(
        (template) => ({
          id: template.id,
          title: template.name,
          content: template.content,
          selected: false,
          category: template.tone || "geral",
          isCustom: true, // Todos os templates vêm da API agora
        })
      );
      setTemplates(convertedTemplates);
    } catch (error) {
      console.error("Erro ao carregar templates:", error);
      message.error("Erro ao carregar templates da API");
    } finally {
      setIsLoadingTemplates(false);
    }
  }, [user?.companyId]);

  // Carregar templates excluídos da API
  const loadDeletedTemplates = async () => {
    setIsLoadingDeletedTemplates(true);
    try {
      const apiTemplates = await messageTemplateService.getDeleted(user?.companyId);
      const convertedTemplates: ConversationTemplate[] = apiTemplates.map(
        (template) => ({
          id: template.id,
          title: template.name,
          content: template.content,
          selected: false,
          category: template.tone || "geral",
          isCustom: true,
        })
      );
      setDeletedTemplates(convertedTemplates);
    } catch (error) {
      console.error("Erro ao carregar templates excluídos:", error);
      message.error("Erro ao carregar templates excluídos da API");
    } finally {
      setIsLoadingDeletedTemplates(false);
    }
  };

  // Carregar agentes existentes da empresa
  const loadExistingAgents = useCallback(async () => {
    if (!user?.companyId) return;
    
    try {
      const agents = await aiAgentApi.getAIAgentsByCompany(user.companyId);
      setExistingAgents(agents);
    } catch (error) {
      console.error("Erro ao carregar agentes:", error);
      message.error("Erro ao carregar agentes existentes");
    }
  }, [user?.companyId]);

  // Verificar limites da empresa
  const checkAgentLimits = useCallback(async () => {
    if (!user?.companyId) return;
    
    try {
      const [canCreate, remaining] = await Promise.all([
        aiAgentApi.canCreateAgent(user.companyId),
        aiAgentApi.getRemainingAgentSlots(user.companyId)
      ]);
      
      setCanCreateAgent(canCreate);
      setRemainingSlots(remaining);
    } catch (error) {
      console.error("Erro ao verificar limites:", error);
    }
  }, [user?.companyId]);

  // Carregar modelo padrão
  useEffect(() => {
    const loadDefaultModel = async () => {
      if (!agentConfig.aiModelId || agentConfig.aiModelId === "default") {
        try {
          const models = await aiModelService.getActiveModels();
          if (models.length > 0) {
            // Usar o primeiro modelo ativo
            setAgentConfig(prev => ({ ...prev, aiModelId: models[0].id }));
          }
        } catch (error) {
          console.error("Erro ao carregar modelo padrão:", error);
        }
      }
    };
    
    loadDefaultModel();
  }, [agentConfig.aiModelId]);

  // Carregar templates ao montar o componente ou quando o usuário mudar
  useEffect(() => {
    if (user?.companyId) {
      loadTemplates();
      loadExistingAgents();
      checkAgentLimits();
    }
  }, [user?.companyId, loadTemplates, loadExistingAgents, checkAgentLimits]);


  // Função para obter ícone e cor do tipo de revisão
  const getRevisionTypeInfo = (type: RevisionType) => {
    switch (type) {
      case "CREATE":
        return {
          icon: <Plus className="w-3 h-3" />,
          color: "green",
          label: "Criado",
        };
      case "EDIT":
        return {
          icon: <Edit3 className="w-3 h-3" />,
          color: "blue",
          label: "Editado",
        };
      case "DELETE":
        return {
          icon: <Trash2 className="w-3 h-3" />,
          color: "red",
          label: "Excluído",
        };
      case "RESTORE":
        return {
          icon: <History className="w-3 h-3" />,
          color: "orange",
          label: "Restaurado",
        };
      default:
        return {
          icon: <Edit3 className="w-3 h-3" />,
          color: "gray",
          label: "Modificado",
        };
    }
  };

  // Carregar histórico de revisões
  const loadTemplateHistory = async (templateId: string) => {
    setIsLoadingHistory(true);
    try {
      const history = await messageTemplateService.getRevisionHistory(
        templateId
      );
      setRevisionHistory(history);
    } catch (error) {
      console.error("Erro ao carregar histórico:", error);
      message.error("Erro ao carregar histórico do template");
    } finally {
      setIsLoadingHistory(false);
    }
  };

  // Abrir modal de histórico
  const handleViewHistory = async (templateId: string) => {
    setShowHistoryModal(true);
    await loadTemplateHistory(templateId);
  };

  // Restaurar template excluído
  const handleRestoreTemplate = async (templateId: string) => {
    try {
      await messageTemplateService.restore(templateId);
      message.success("Template restaurado com sucesso!");
      loadDeletedTemplates(); // Recarregar templates excluídos
      loadTemplates(); // Recarregar templates ativos
    } catch (error: unknown) {
      console.error("Erro ao restaurar template:", error);
      const errorMessage = (error as Error)?.message || "Erro ao restaurar template";
      if ((error as { status?: number })?.status === 403) {
        message.error("Você não tem permissão para restaurar este template");
      } else if ((error as { status?: number })?.status === 404) {
        message.error("Template não encontrado");
      } else {
        message.error(`Erro ao restaurar template: ${errorMessage}`);
      }
    }
  };

  const handleAgentConfigChange = (field: keyof AgentConfig, value: string | number | boolean) => {
    console.log('🔵 handleAgentConfigChange called', {
      field,
      valueType: typeof value,
      valueLength: typeof value === 'string' ? value.length : 'n/a',
      valuePreview: typeof value === 'string' && value.length > 50 ? value.substring(0, 50) + '...' : value
    });
    
    setAgentConfig((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleCampaignChange = (field: keyof ExtendedCampaignData, value: string) => {
    setCampaignData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleDateRangeChange = (dates: any) => {
    if (dates && dates.length === 2) {
      setCampaignData((prev) => ({
        ...prev,
        startDate: dates[0].format("YYYY-MM-DD"),
        endDate: dates[1].format("YYYY-MM-DD"),
      }));
    }
  };

  const handleTemplateToggle = (templateId: string) => {
    setTemplates((prev) =>
      prev.map((template) =>
        template.id === templateId
          ? { ...template, selected: !template.selected }
          : template
      )
    );
  };

  const handleEditTemplate = (templateId: string) => {
    const template = templates.find((t) => t.id === templateId);
    if (template) {
      setEditingTemplate(template);
      setShowTemplateModal(true);
    }
  };

  const handleSaveAgent = async () => {
    if (!agentConfig.name || !agentConfig.temperament) {
      message.error("Preencha todos os campos obrigatórios!");
      return;
    }

    if (!user?.companyId) {
      message.error("Erro: Usuário não possui empresa associada");
      return;
    }

    // Verificar limite antes de criar
    if (!canCreateAgent) {
      message.error("Limite de agentes IA atingido para seu plano atual");
      return;
    }

    try {
      const createAgentData = {
        companyId: user.companyId,
        name: agentConfig.name,
        description: agentConfig.description || null,
        avatarBase64: agentConfig.avatarBase64 || null,
        aiModelId: agentConfig.aiModelId,
        temperament: agentConfig.temperament,
        maxResponseLength: agentConfig.maxResponseLength,
        temperature: agentConfig.temperature,
        isActive: agentConfig.isActive,
      };

      console.log('🚀 [DEBUG] Creating agent with data:', createAgentData);
      console.log('🚀 [DEBUG] User data:', { 
        userId: user.id, 
        companyId: user.companyId, 
        companySlug: user.companySlug,
        role: user.role 
      });

      // Debug context before creating agent
      try {
        await aiAgentApi.debugContext();
      } catch (debugError) {
        console.error('🔍 [DEBUG] Failed to get context:', debugError);
      }

      const createdAgent = await aiAgentApi.createAIAgent(createAgentData);
      
      message.success(`Agente "${createdAgent.name}" criado com sucesso!`);
      
      // Reset form
      setAgentConfig({
        name: "",
        description: "",
        avatarBase64: "",
        aiModelId: "default",
        temperament: "AMIGAVEL",
        maxResponseLength: 500,
        temperature: 0.7,
        isActive: true,
      });

      // Recarregar lista de agentes e limites
      await loadExistingAgents();
      await checkAgentLimits();

    } catch (error: unknown) {
      console.error("Erro ao criar agente:", error);
      const errorMessage = (error as Error)?.message || "Erro ao criar agente";
      message.error(errorMessage);
    }
  };

  // Editar agente existente
  const handleEditAgent = (agent: AIAgent) => {
    setSelectedAgent(agent);
    setAgentConfig({
      name: agent.name,
      description: agent.description || "",
      avatarBase64: agent.avatarBase64 || "",
      aiModelId: agent.aiModelId,
      temperament: agent.temperament,
      maxResponseLength: agent.maxResponseLength,
      temperature: agent.temperature,
      isActive: agent.isActive,
    });
    setShowAgentModal(true);
  };

  // Salvar alterações do agente
  const handleUpdateAgent = async () => {
    if (!selectedAgent) return;

    try {
      const updateData = {
        name: agentConfig.name,
        description: agentConfig.description || null,
        avatarBase64: agentConfig.avatarBase64 || null,
        aiModelId: agentConfig.aiModelId,
        temperament: agentConfig.temperament,
        maxResponseLength: agentConfig.maxResponseLength,
        temperature: agentConfig.temperature,
        isActive: agentConfig.isActive,
      };

      await aiAgentApi.updateAIAgent(selectedAgent.id, updateData);
      
      message.success(`Agente "${agentConfig.name}" atualizado com sucesso!`);
      
      // Fechar modal e recarregar lista
      setShowAgentModal(false);
      setSelectedAgent(null);
      await loadExistingAgents();

      // Reset form
      setAgentConfig({
        name: "",
        description: "",
        avatarBase64: "",
        aiModelId: "default",
        temperament: "AMIGAVEL",
        maxResponseLength: 500,
        temperature: 0.7,
        isActive: true,
      });

    } catch (error: unknown) {
      console.error("Erro ao atualizar agente:", error);
      const errorMessage = (error as Error)?.message || "Erro ao atualizar agente";
      message.error(errorMessage);
    }
  };

  // Deletar agente
  const handleDeleteAgent = (agent: AIAgent) => {
    Modal.confirm({
      title: 'Excluir Agente IA',
      content: `Tem certeza que deseja excluir o agente "${agent.name}"? Esta ação não pode ser desfeita.`,
      okText: 'Excluir',
      okType: 'danger',
      cancelText: 'Cancelar',
      onOk: async () => {
        try {
          await aiAgentApi.deleteAIAgent(agent.id);
          message.success(`Agente "${agent.name}" excluído com sucesso!`);
          await loadExistingAgents();
          await checkAgentLimits();
        } catch (error) {
          console.error("Erro ao excluir agente:", error);
          message.error("Erro ao excluir agente");
        }
      }
    });
  };

  // Criar novo agente
  const handleCreateNewAgent = () => {
    setSelectedAgent(null);
    setAgentConfig({
      name: "",
      description: "",
      avatarBase64: "",
      aiModelId: aiModels.length > 0 ? aiModels[0].id : "",
      temperament: "AMIGAVEL",
      maxResponseLength: 500,
      temperature: 0.7,
      isActive: true,
    });
    setShowAgentModal(true);
  };

  const handleEnhanceTemplate = (templateId: string) => {
    const template = templates.find((t) => t.id === templateId);
    if (template) {
      setEditingTemplate({ ...template, content: template.content });
      setShowTemplateModal(true);
    }
  };

  const handleDeleteTemplate = async (templateId: string) => {
    try {
      await messageTemplateService.delete(templateId);
      message.success("Template excluído com sucesso!");
      // Recarregar templates da API
      await loadTemplates();
    } catch (error) {
      console.error("Erro ao excluir template:", error);
      message.error("Erro ao excluir template");
    }
  };

  const handleSaveTemplate = async (templateData: {
    title: string;
    content: string;
    category: string;
  }) => {
    try {
      if (editingTemplate) {
        // Editar template existente
        const updateData: UpdateMessageTemplateRequest = {
          name: templateData.title,
          content: templateData.content,
          tone: templateData.category,
        };
        await messageTemplateService.update(editingTemplate.id, updateData);
        message.success("Template atualizado com sucesso!");
      } else {
        // Criar novo template
        if (!user?.companyId) {
          message.error("Erro: Usuário não possui empresa associada");
          return;
        }

        const createData: CreateMessageTemplateRequest = {
          companyId: user.companyId,
          name: templateData.title,
          content: templateData.content,
          isAiGenerated: false,
          tone: templateData.category,
        };

        await messageTemplateService.create(createData);
        message.success("Template criado com sucesso!");
      }

      // Recarregar templates da API
      await loadTemplates();
    } catch (error) {
      console.error("Erro ao salvar template:", error);
      message.error("Erro ao salvar template");
    }

    setShowTemplateModal(false);
    setEditingTemplate(null);
  };

  const uploadProps: UploadProps = {
    name: "file",
    accept: ".xlsx,.csv",
    beforeUpload: (file) => {
      const isValidType =
        file.type ===
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
        file.type === "text/csv" ||
        file.name.endsWith(".xlsx") ||
        file.name.endsWith(".csv");

      if (!isValidType) {
        message.error("Você só pode enviar arquivos XLSX ou CSV!");
        return false;
      }

      setCampaignData((prev) => ({ ...prev, file }));
      return false;
    },
    onRemove: () => {
      setCampaignData((prev) => ({ ...prev, file: undefined }));
    },
  };

  const handleImportCampaign = async () => {
    if (
      !campaignData.name ||
      !campaignData.file ||
      !campaignData.startDate ||
      !campaignData.endDate
    ) {
      message.error("Preencha todos os campos obrigatórios!");
      return;
    }

    const selectedTemplates = templates.filter((t) => t.selected);
    if (selectedTemplates.length === 0) {
      message.error("Selecione pelo menos um template de conversa!");
      return;
    }

    if (!user?.companyId || !user?.id) {
      message.error("Erro: Usuário não autenticado corretamente");
      return;
    }

    setIsUploading(true);

    try {
      // Mostrar progresso do processamento
      message.loading("Processando arquivo...", 1);
      await new Promise((resolve) => setTimeout(resolve, 500));

      message.loading("Validando contatos...", 1);
      await new Promise((resolve) => setTimeout(resolve, 500));

      message.loading("Criando campanha...", 1);

      // Preparar dados da campanha
      const campaignRequestData: CampaignData = {
        name: campaignData.name,
        description: campaignData.description,
        startDate: campaignData.startDate,
        endDate: campaignData.endDate,
        sourceSystem: campaignData.sourceSystem || "Planilha Manual",
        templateIds: selectedTemplates.map((t) => t.id),
      };

      // Enviar para o backend
      
      const result = await campaignService.processExcelAndCreateCampaign(
        campaignData.file,
        campaignRequestData,
        user.companyId,
        user.id
      );


      if (result && result.success) {
        // Mostrar duplicados encontrados
        if (result.statistics.duplicates > 0) {
          setDuplicateUsers([`${result.statistics.duplicates} contatos duplicados encontrados`]);
        }

        // Mensagem de sucesso detalhada
        message.success({
          content: (
            <div>
              <div className="font-semibold mb-2">
                Campanha "{result.campaign.name}" criada com sucesso! 🎉
              </div>
              <div className="text-sm space-y-1">
                <div>
                  📊 {result.statistics.processed} contatos processados do arquivo
                </div>
                <div>
                  💬 {result.statistics.created} contatos adicionados à campanha
                </div>
                <div>📝 {selectedTemplates.length} templates selecionados</div>
                {result.statistics.duplicates > 0 && (
                  <div>
                    ⚠️ {result.statistics.duplicates} duplicatas detectadas
                  </div>
                )}
                {result.errors.length > 0 && (
                  <div>
                    ❌ {result.errors.length} erros no processamento
                  </div>
                )}
                <div className="mt-2 text-green-600">
                  ✅ Campanha ativa e pronta para uso
                </div>
              </div>
            </div>
          ),
          duration: 8,
        });

        // Mostrar erros se houverem
        if (result.errors.length > 0) {
          console.warn("Erros durante o processamento:", result.errors);
          // Opcional: mostrar modal com detalhes dos erros
        }

        // Reset form
        setCampaignData({
          name: "",
          description: "",
          startDate: "",
          endDate: "",
          sourceSystem: "",
          file: undefined,
        });

        // Limpar lista de duplicados
        setDuplicateUsers([]);

        // Log da campanha criada

        // Fechar configurações e voltar para o chat
        onBack();
      } else {
        message.error(result ? "Erro ao criar campanha!" : "Erro na comunicação com o servidor!");
      }
    } catch (error: unknown) {
      console.error("Erro ao criar campanha:", error);
      
      // Tentar extrair mensagem de erro específica
      let errorMessage = "Erro ao criar campanha!";
      const err = error as { response?: { data?: { error?: string } }; message?: string };
      if (err.response?.data?.error) {
        errorMessage = err.response.data.error;
      } else if (err.message) {
        errorMessage = `Erro: ${err.message}`;
      }
      
      message.error(errorMessage);
    } finally {
      setIsUploading(false);
    }
  };


  return (
    <div className="flex h-screen bg-gray-50">
      <div className="flex-1 flex flex-col">
        {/* Header melhorado com gradiente sutil */}
        <div className="bg-white border-b border-gray-200 shadow-sm">
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <button
                  onClick={onBack}
                  className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                  <h1 className="text-2xl font-bold text-gray-800">
                    Configurações do Sistema
                  </h1>
                  <p className="text-sm text-gray-600 mt-1">
                    Gerencie agentes, campanhas e templates de conversa
                  </p>
                </div>
              </div>
              {/* Status do centro de sangue */}
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2 bg-green-50 text-green-700 px-3 py-2 rounded-lg text-sm font-medium border border-green-200">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  Sistema Ativo
                </div>
              </div>
            </div>
          </div>

          {/* Tabs melhoradas */}
          <div className="px-6">
            <div className="flex gap-1 border-b border-gray-200">
              <button
                onClick={() => setActiveTab("agent")}
                className={`px-6 py-3 font-medium transition-all relative ${
                  activeTab === "agent"
                    ? "text-red-600 border-b-2 border-red-500 bg-red-50/50"
                    : "text-gray-600 hover:text-red-600 hover:bg-gray-50"
                }`}
              >
                Agente IA
              </button>
              <button
                onClick={() => setActiveTab("campaign")}
                className={`px-6 py-3 font-medium transition-all relative ${
                  activeTab === "campaign"
                    ? "text-red-600 border-b-2 border-red-500 bg-red-50/50"
                    : "text-gray-600 hover:text-red-600 hover:bg-gray-50"
                }`}
              >
                Nova Campanha
              </button>
              <button
                onClick={() => setActiveTab("templates")}
                className={`px-6 py-3 font-medium transition-all relative ${
                  activeTab === "templates"
                    ? "text-red-600 border-b-2 border-red-500 bg-red-50/50"
                    : "text-gray-600 hover:text-red-600 hover:bg-gray-50"
                }`}
              >
                Templates
              </button>
              <button
                onClick={() => {
                  setActiveTab("deleted");
                  loadDeletedTemplates();
                }}
                className={`px-6 py-3 font-medium transition-all relative ${
                  activeTab === "deleted"
                    ? "text-red-600 border-b-2 border-red-500 bg-red-50/50"
                    : "text-gray-600 hover:text-red-600 hover:bg-gray-50"
                }`}
              >
                Templates Excluídos
              </button>
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto bg-gray-50">
          {activeTab === "agent" && (
            <div className="max-w-4xl mx-auto p-8">
              {/* Seção principal com card melhorado */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 mb-6">
                <div className="flex items-center gap-3 mb-8">
                  <div className="w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center">
                    <User className="w-6 h-6 text-red-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-800">
                      Configuração do Agente IA
                    </h2>
                    <p className="text-gray-600">
                      Configure a personalidade e comportamento do assistente
                      virtual
                    </p>
                  </div>
                </div>

                {/* Seção do avatar e nome */}
                <div className="flex items-start gap-8 mb-8 p-6 bg-gray-50 rounded-xl">
                  <AvatarUpload
                    value={agentConfig.avatarBase64}
                    onChange={(base64) => {
                      console.log('🔵 ConfigurationPage: AvatarUpload onChange called', {
                        base64Length: base64?.length || 0,
                        base64Preview: base64?.substring(0, 50) + '...' || 'null'
                      });
                      handleAgentConfigChange("avatarBase64", base64 || "");
                    }}
                    size={96}
                    placeholder="Upload avatar"
                  />
                  <div className="flex-1 space-y-4">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Nome do Agente *
                      </label>
                      <Input
                        value={agentConfig.name}
                        onChange={(e) =>
                          handleAgentConfigChange("name", e.target.value)
                        }
                        placeholder="Ex: Sofia, Ana, João..."
                        size="large"
                        className="font-medium"
                        required
                      />
                      <p className="text-xs text-gray-500 mt-2">
                        O nome será usado nas conversas com os usuários
                      </p>
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Descrição
                      </label>
                      <TextArea
                        value={agentConfig.description}
                        onChange={(e) =>
                          handleAgentConfigChange("description", e.target.value)
                        }
                        placeholder="Descreva a função e especialidade do agente..."
                        rows={3}
                      />
                    </div>
                  </div>
                </div>

                {/* Configurações principais */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                  <div className="space-y-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-4">
                        Temperamento *
                      </label>
                      <div className="space-y-2">
                        <Radio.Group
                          value={agentConfig.temperament}
                          onChange={(e: RadioChangeEvent) =>
                            handleAgentConfigChange(
                              "temperament",
                              e.target.value
                            )
                          }
                          className="w-full"
                        >
                          <div className="space-y-2">
                            <Radio.Button
                              value="FORMAL"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Formal e Respeitoso
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="AMIGAVEL"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Amigável e Acolhedor
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="DESCONTRAIDO"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Descontraído e Informal
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="SERIO"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Sério e Profissional
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="EMPATICO"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Empático e Compreensivo
                              </span>
                            </Radio.Button>
                          </div>
                        </Radio.Group>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-6">
                    <div>
                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        <div className="flex items-center gap-2 mb-2">
                          <h4 className="font-semibold text-blue-900">
                            ✨ Modelo de IA
                          </h4>
                        </div>
                        <div className="text-sm text-blue-800">
                          O sistema usa automaticamente o melhor modelo disponível para otimizar qualidade e custo
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Configurações avançadas */}
                <div className="mt-8 p-6 bg-gray-50 rounded-xl">
                  <h3 className="text-lg font-semibold text-gray-800 mb-6">
                    Configurações Avançadas
                  </h3>
                  <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Limite de Caracteres
                      </label>
                      <Input
                        type="number"
                        value={agentConfig.maxResponseLength}
                        onChange={(e) =>
                          handleAgentConfigChange("maxResponseLength", parseInt(e.target.value) || 500)
                        }
                        min={1}
                        max={10000}
                        size="large"
                      />
                      <p className="text-xs text-gray-500 mt-1">
                        Máximo de caracteres por resposta (1-10000)
                      </p>
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Criatividade (Temperature)
                      </label>
                      <Input
                        type="number"
                        value={agentConfig.temperature}
                        onChange={(e) =>
                          handleAgentConfigChange("temperature", parseFloat(e.target.value) || 0.7)
                        }
                        min={0}
                        max={1}
                        step={0.1}
                        size="large"
                      />
                      <p className="text-xs text-gray-500 mt-1">
                        Nível de criatividade (0.0 - 1.0)
                      </p>
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Status
                      </label>
                      <Radio.Group
                        value={agentConfig.isActive}
                        onChange={(e: RadioChangeEvent) =>
                          handleAgentConfigChange("isActive", e.target.value)
                        }
                        className="w-full"
                      >
                        <Radio.Button value={true} className="w-20">
                          Ativo
                        </Radio.Button>
                        <Radio.Button value={false} className="w-20">
                          Inativo
                        </Radio.Button>
                      </Radio.Group>
                    </div>
                  </div>
                </div>

                {/* Botões de ação */}
                <div className="flex items-center justify-between pt-8 border-t border-gray-200 mt-8">
                  <div className="text-sm text-gray-500">
                    Configure todos os campos obrigatórios
                  </div>
                  <div className="flex gap-3">
                    <Button size="large" className="px-6">
                      Testar Agente
                    </Button>
                    <Button
                      type="primary"
                      size="large"
                      onClick={handleSaveAgent}
                      disabled={!agentConfig.name || !agentConfig.temperament}
                      className="px-8 bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
                    >
                      Salvar Agente
                    </Button>
                  </div>
                </div>
              </div>

              {/* Card de preview do agente */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-4">
                  Preview da Conversa
                </h3>
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-start gap-3">
                    <div className="w-8 h-8 bg-red-500 rounded-full flex items-center justify-center">
                      <User className="w-4 h-4 text-white" />
                    </div>
                    <div className="flex-1">
                      <div className="text-sm font-medium text-gray-700 mb-1">
                        {agentConfig.name}
                      </div>
                      <div className="text-sm text-gray-600">
                        {agentConfig.temperament === "FORMAL" &&
                          "Bom dia! Sou o assistente virtual da empresa. Como posso ajudá-lo hoje?"}
                        {agentConfig.temperament === "AMIGAVEL" &&
                          "Oi! 😊 Eu sou o assistente da empresa! Como posso te ajudar hoje?"}
                        {agentConfig.temperament === "DESCONTRAIDO" &&
                          "E aí! Tudo bem? Sou o assistente aqui da empresa. Em que posso te ajudar?"}
                        {agentConfig.temperament === "SERIO" &&
                          "Olá. Sou o assistente da empresa. Estou aqui para ajudá-lo."}
                        {agentConfig.temperament === "EMPATICO" &&
                          "Olá! Entendo que você precisa de ajuda. Estou aqui para te apoiar no que precisar! 💙"}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Seção de Agentes Existentes */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mt-6">
                <div className="flex items-center justify-between mb-6">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-semibold text-gray-800">
                      Agentes IA da Empresa
                    </h3>
                    <div className="bg-blue-100 text-blue-700 px-3 py-1 rounded-full text-sm font-medium">
                      {existingAgents.length} de {existingAgents.length + remainingSlots} agentes
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="text-sm text-gray-600">
                      {remainingSlots > 0 ? (
                        <span className="text-green-600 font-medium">
                          {remainingSlots} slot{remainingSlots !== 1 ? 's' : ''} disponível{remainingSlots !== 1 ? 'eis' : ''}
                        </span>
                      ) : (
                        <span className="text-orange-600 font-medium">
                          Limite atingido
                        </span>
                      )}
                    </div>
                    <Button
                      type="primary"
                      icon={<Plus />}
                      onClick={handleCreateNewAgent}
                      disabled={!canCreateAgent}
                      className="bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
                    >
                      Novo Agente
                    </Button>
                  </div>
                </div>

                {existingAgents.length === 0 ? (
                  <div className="text-center py-12 bg-gray-50 rounded-lg">
                    <User className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                    <h4 className="text-lg font-medium text-gray-500 mb-2">
                      Nenhum agente criado ainda
                    </h4>
                    <p className="text-gray-400 mb-4">
                      Crie seu primeiro agente IA para começar a automatizar conversas
                    </p>
                    <Button
                      type="primary"
                      icon={<Plus />}
                      onClick={handleCreateNewAgent}
                      disabled={!canCreateAgent}
                      className="bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
                    >
                      Criar Primeiro Agente
                    </Button>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {existingAgents.map((agent) => (
                      <div
                        key={agent.id}
                        className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                      >
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                              {agent.avatarBase64 ? (
                                <img
                                  src={agent.avatarBase64}
                                  alt={agent.name}
                                  className="w-10 h-10 rounded-full object-cover"
                                />
                              ) : (
                                <User className="w-5 h-5 text-red-600" />
                              )}
                            </div>
                            <div>
                              <h5 className="font-semibold text-gray-800">
                                {agent.name}
                              </h5>
                              <div className="flex items-center gap-2">
                                <span
                                  className={`px-2 py-1 rounded-full text-xs font-medium ${
                                    agent.isActive
                                      ? "bg-green-100 text-green-700"
                                      : "bg-gray-100 text-gray-600"
                                  }`}
                                >
                                  {agent.isActive ? "Ativo" : "Inativo"}
                                </span>
                                <span className="text-xs text-gray-500">
                                  {agent.temperament}
                                </span>
                              </div>
                            </div>
                          </div>
                          <Dropdown
                            trigger={["click"]}
                            menu={{
                              items: [
                                {
                                  key: "edit",
                                  label: "Editar",
                                  icon: <Edit3 className="w-4 h-4" />,
                                  onClick: () => handleEditAgent(agent),
                                },
                                {
                                  key: "delete",
                                  label: "Excluir",
                                  icon: <Trash2 className="w-4 h-4" />,
                                  danger: true,
                                  onClick: () => handleDeleteAgent(agent),
                                },
                              ],
                            }}
                          >
                            <Button
                              size="small"
                              type="text"
                              icon={<MoreVertical className="w-4 h-4" />}
                              className="hover:bg-gray-100"
                            />
                          </Dropdown>
                        </div>

                        {agent.description && (
                          <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                            {agent.description}
                          </p>
                        )}

                        <div className="text-xs text-gray-500 space-y-1">
                          <div className="flex justify-between">
                            <span>Temperatura:</span>
                            <span className="font-medium">{agent.temperature}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Max chars:</span>
                            <span className="font-medium">{agent.maxResponseLength}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === "campaign" && (
            <div className="max-w-7xl mx-auto p-8">
              {/* Header da seção */}
              <div className="flex items-center gap-3 mb-8">
                <div className="w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center">
                  <Plus className="w-6 h-6 text-red-600" />
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-gray-800">
                    Nova Campanha
                  </h2>
                  <p className="text-gray-600">
                    Configure uma nova campanha de doação e selecione os
                    templates
                  </p>
                </div>
              </div>

              {/* Layout em 2 colunas */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Coluna 1: Dados da Campanha */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                  <h3 className="text-lg font-semibold text-gray-800 mb-6">
                    Dados da Campanha
                  </h3>

                  <div className="space-y-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Nome da Campanha *
                      </label>
                      <Input
                        value={campaignData.name}
                        onChange={(e) =>
                          handleCampaignChange("name", e.target.value)
                        }
                        placeholder="Ex: Campanha Junho 2025"
                        size="large"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Descrição
                      </label>
                      <TextArea
                        value={campaignData.description}
                        onChange={(e) =>
                          handleCampaignChange("description", e.target.value)
                        }
                        rows={4}
                        placeholder="Descreva o objetivo da campanha..."
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Período da Campanha *
                      </label>
                      <RangePicker
                        value={
                          campaignData.startDate && campaignData.endDate
                            ? [
                                dayjs(campaignData.startDate),
                                dayjs(campaignData.endDate),
                              ]
                            : null
                        }
                        onChange={handleDateRangeChange}
                        className="w-full"
                        size="large"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Sistema de Origem
                      </label>
                      <Select
                        value={campaignData.sourceSystem}
                        onChange={(value) =>
                          handleCampaignChange("sourceSystem", value)
                        }
                        className="w-full"
                        placeholder="Selecione o sistema"
                        size="large"
                      >
                        <Option value="realblood">RealBlood</Option>
                        <Option value="realclinic">RealClinic</Option>
                        <Option value="hemovida">HemoVida</Option>
                        <Option value="sangueheroi">SangueHerói</Option>
                        <Option value="doadordoar">DoadorDoar</Option>
                        <Option value="hemocentro">Sistema HemoCentro</Option>
                        <Option value="vidaativa">VidaAtiva</Option>
                        <Option value="planilha">Planilha Manual</Option>
                        <Option value="outro">Outro Sistema</Option>
                      </Select>
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-3">
                        Arquivo de Contatos *
                      </label>
                      <AntUpload.Dragger
                        {...uploadProps}
                        className="border-2 border-dashed border-gray-300 hover:border-red-400"
                      >
                        <p className="ant-upload-drag-icon">
                          <Upload className="w-12 h-12 text-gray-400 mx-auto" />
                        </p>
                        <p className="ant-upload-text text-gray-700">
                          Clique ou arraste arquivo para esta área
                        </p>
                        <p className="ant-upload-hint text-gray-500">
                          Suporta arquivos .xlsx, .csv. O sistema processará
                          automaticamente os contatos.
                        </p>
                      </AntUpload.Dragger>
                    </div>

                    {duplicateUsers.length > 0 && (
                      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                        <h4 className="text-sm font-medium text-yellow-800 mb-2">
                          ⚠️ Usuários que responderam negativamente em campanhas
                          anteriores:
                        </h4>
                        <ul className="text-sm text-yellow-700">
                          {duplicateUsers.map((user, index) => (
                            <li key={index}>• {user}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                </div>

                {/* Coluna 2: Templates */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                  <div className="sticky top-6">
                    <div className="flex items-center justify-between mb-6">
                      <h3 className="text-lg font-semibold text-gray-800">
                        Templates de Conversa
                      </h3>
                      <div className="text-sm text-gray-600 bg-gray-100 px-3 py-1 rounded-full">
                        {templates.filter((t) => t.selected).length}{" "}
                        selecionados
                      </div>
                    </div>

                    {templates.filter((t) => t.selected).length > 0 && (
                      <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                        <h4 className="font-medium text-blue-800 mb-2 text-sm">
                          💡 Como funcionam os templates
                        </h4>
                        <p className="text-xs text-blue-700">
                          Os templates selecionados serão distribuídos
                          automaticamente entre os contatos importados para
                          diversificar as abordagens.
                        </p>
                      </div>
                    )}

                    <div className="max-h-96 overflow-y-auto space-y-3">
                      {templates.map((template) => (
                        <div
                          key={template.id}
                          onClick={() => handleTemplateToggle(template.id)}
                          className={`cursor-pointer p-4 rounded-lg border-2 transition-all hover:shadow-md ${
                            template.selected
                              ? "border-red-300 bg-red-50"
                              : "border-gray-200 hover:border-gray-300"
                          }`}
                        >
                          <div className="flex items-start justify-between mb-2">
                            <h5 className="font-medium text-gray-800 text-sm flex-1 pr-3">
                              {template.title}
                            </h5>
                            <div
                              className={`w-5 h-5 rounded border-2 flex items-center justify-center flex-shrink-0 ${
                                template.selected
                                  ? "bg-red-500 border-red-500"
                                  : "border-gray-300"
                              }`}
                            >
                              {template.selected && (
                                <Check className="w-3 h-3 text-white" />
                              )}
                            </div>
                          </div>
                          <p className="text-xs text-gray-600 line-clamp-2">
                            {template.content}
                          </p>
                          {template.isCustom && (
                            <span className="inline-block mt-2 bg-green-100 text-green-800 text-xs px-2 py-1 rounded">
                              Personalizado
                            </span>
                          )}
                        </div>
                      ))}
                    </div>

                    {/* Resumo da seleção */}
                    {templates.filter((t) => t.selected).length > 0 && (
                      <div className="mt-6 p-4 bg-gray-50 rounded-lg">
                        <h4 className="text-sm font-medium text-gray-700 mb-2">
                          Templates Selecionados:
                        </h4>
                        <div className="space-y-1">
                          {templates
                            .filter((t) => t.selected)
                            .map((template) => (
                              <div
                                key={template.id}
                                className="text-xs text-gray-600"
                              >
                                • {template.title}
                              </div>
                            ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Botão de ação centralizado */}
              <div className="mt-8 flex justify-center">
                <Button
                  type="primary"
                  size="large"
                  onClick={handleImportCampaign}
                  loading={isUploading}
                  disabled={
                    !campaignData.name ||
                    !campaignData.file ||
                    !campaignData.startDate ||
                    !campaignData.endDate ||
                    templates.filter((t) => t.selected).length === 0
                  }
                  className="px-12 bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
                >
                  {isUploading
                    ? "Processando Arquivo e Aplicando Templates..."
                    : "Criar Campanha"}
                </Button>
              </div>
            </div>
          )}

          {activeTab === "templates" && (
            <div className="max-w-6xl mx-auto p-8">
              {/* Header da seção */}
              <div className="flex items-center justify-between mb-8">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center">
                    <Edit3 className="w-6 h-6 text-red-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-800">
                      Templates de Conversa
                    </h2>
                    <p className="text-gray-600">
                      Gerencie e personalize as mensagens automáticas
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-sm text-gray-600 bg-gray-100 px-4 py-2 rounded-lg">
                    {templates.filter((t) => t.selected).length} de{" "}
                    {templates.length} selecionados
                  </div>
                  {user?.companyId && (
                    <div className="text-xs text-gray-500 bg-blue-50 px-3 py-1 rounded-full">
                      Empresa: {user.companySlug || user.companyId}
                    </div>
                  )}
                  <Button
                    type="primary"
                    icon={<Plus />}
                    onClick={() => setShowTemplateModal(true)}
                    size="large"
                    loading={isLoadingTemplates}
                    disabled={!user?.companyId}
                    className="bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
                  >
                    Novo Template
                  </Button>
                </div>
              </div>

              {/* Info sobre templates */}
              <div className="mb-8 p-6 bg-white rounded-xl shadow-sm border border-gray-200">
                <h3 className="font-medium text-gray-800 mb-4 flex items-center gap-2">
                  💡 Como os templates funcionam nas campanhas
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
                  <ul className="space-y-2">
                    <li className="flex items-start gap-2">
                      <span className="text-red-500 mt-1">•</span>
                      Os templates selecionados serão distribuídos
                      automaticamente entre os contatos
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-red-500 mt-1">•</span>
                      Cada contato receberá uma mensagem inicial baseada em um
                      dos templates escolhidos
                    </li>
                  </ul>
                  <ul className="space-y-2">
                    <li className="flex items-start gap-2">
                      <span className="text-red-500 mt-1">•</span>O sistema
                      alternará entre os templates para diversificar as
                      abordagens
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-red-500 mt-1">•</span>
                      Contatos duplicados não receberão mensagens
                      automaticamente
                    </li>
                  </ul>
                </div>
              </div>

              {/* Grid de templates */}
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {templates.map((template) => (
                  <div
                    key={template.id}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleTemplateToggle(template.id);
                    }}
                    className={`cursor-pointer p-6 rounded-xl border-2 transition-all hover:shadow-lg ${
                      template.selected
                        ? "border-red-300 bg-red-50 shadow-md"
                        : "border-gray-200 bg-white hover:border-gray-300"
                    }`}
                  >
                    <div className="flex items-start justify-between mb-4">
                      <h4 className="font-semibold text-gray-800 flex-1 pr-3">
                        {template.title}
                      </h4>
                      <div className="flex items-center gap-2">
                        <Dropdown
                          trigger={["click"]}
                          menu={{
                            items: [
                              {
                                key: "edit",
                                label: "Editar",
                                icon: <Edit3 className="w-4 h-4" />,
                                onClick: () => handleEditTemplate(template.id),
                              },
                              {
                                key: "history",
                                label: "Ver Histórico",
                                icon: <History className="w-4 h-4" />,
                                onClick: () => handleViewHistory(template.id),
                              },
                              {
                                key: "enhance",
                                label: "Melhorar com IA",
                                icon: <Sparkles className="w-4 h-4" />,
                                onClick: () =>
                                  handleEnhanceTemplate(template.id),
                              },
                              {
                                key: "delete",
                                label: "Excluir",
                                icon: <Trash2 className="w-4 h-4" />,
                                danger: true,
                                onClick: () =>
                                  handleDeleteTemplate(template.id),
                              },
                            ],
                          }}
                        >
                          <Button
                            size="small"
                            type="text"
                            icon={<MoreVertical className="w-4 h-4" />}
                            onClick={(e) => e.stopPropagation()}
                            className="hover:bg-gray-100"
                          />
                        </Dropdown>
                        <div
                          className={`w-6 h-6 rounded border-2 flex items-center justify-center ${
                            template.selected
                              ? "bg-red-500 border-red-500"
                              : "border-gray-300"
                          }`}
                        >
                          {template.selected && (
                            <Check className="w-4 h-4 text-white" />
                          )}
                        </div>
                      </div>
                    </div>

                    <p className="text-sm text-gray-600 line-clamp-4 mb-4 leading-relaxed">
                      {template.content}
                    </p>

                    <div className="flex items-center justify-between pt-4 border-t border-gray-200">
                      <span className="text-xs text-gray-500 font-medium">
                        {template.category || "Geral"}
                      </span>
                      {template.isCustom && (
                        <span className="bg-green-100 text-green-800 text-xs px-2 py-1 rounded-full font-medium">
                          Personalizado
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              {/* Ações finais */}
              <div className="mt-8 flex items-center justify-between pt-8 border-t border-gray-200">
                <div className="text-sm text-gray-500">
                  {templates.filter((t) => t.selected).length} templates
                  selecionados para campanhas
                </div>
                <Button
                  type="primary"
                  size="large"
                  className="px-8 bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
                >
                  Salvar Seleção
                </Button>
              </div>
            </div>
          )}

          {activeTab === "deleted" && (
            <div className="max-w-6xl mx-auto p-8">
              {/* Header da seção */}
              <div className="flex items-center justify-between mb-8">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center">
                    <Trash2 className="w-6 h-6 text-red-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-800">
                      Templates Excluídos
                    </h2>
                    <p className="text-gray-600">
                      Visualize templates excluídos e suas revisões
                    </p>
                  </div>
                </div>
              </div>

              {/* Lista de Templates Excluídos */}
              <div className="space-y-4">
                {isLoadingDeletedTemplates ? (
                  <div className="flex justify-center items-center py-12">
                    <Spin size="large" />
                  </div>
                ) : deletedTemplates.length === 0 ? (
                  <div className="text-center py-12">
                    <Trash2 className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-gray-500 mb-2">
                      Nenhum template excluído
                    </h3>
                    <p className="text-gray-400">
                      Quando você excluir templates, eles aparecerão aqui
                    </p>
                  </div>
                ) : (
                  <div className="grid gap-4">
                    {deletedTemplates.map((template) => (
                      <div
                        key={template.id}
                        className="border border-gray-200 rounded-xl p-6 bg-white hover:shadow-md transition-shadow"
                      >
                        <div className="flex justify-between items-start mb-4">
                          <div className="flex-1">
                            <h3 className="text-lg font-semibold text-gray-800 mb-2">
                              {template.title}
                            </h3>
                            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                              <p className="text-sm text-gray-700 whitespace-pre-wrap">
                                {template.content}
                              </p>
                            </div>
                          </div>
                          <div className="flex gap-2 ml-4">
                            <Dropdown
                              menu={{
                                items: [
                                  {
                                    key: "history",
                                    label: "Ver Histórico",
                                    icon: <History className="w-4 h-4" />,
                                    onClick: () => handleViewHistory(template.id),
                                  },
                                  {
                                    key: "restore",
                                    label: "Restaurar",
                                    icon: <RotateCcw className="w-4 h-4" />,
                                    onClick: () => handleRestoreTemplate(template.id),
                                  },
                                ],
                              }}
                              trigger={["click"]}
                            >
                              <Button
                                type="text"
                                icon={<MoreVertical className="w-4 h-4" />}
                                size="small"
                              />
                            </Dropdown>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm text-gray-500">
                          <span className="inline-flex items-center gap-1">
                            <Trash2 className="w-3 h-3" />
                            Template excluído
                          </span>
                          {template.category && (
                            <span className="text-gray-400">•</span>
                          )}
                          {template.category && (
                            <span className="capitalize">{template.category}</span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Modal de Histórico */}
      <Modal
        title={
          <div className="flex items-center gap-2">
            <History className="w-5 h-5 text-blue-600" />
            <span>Histórico do Template</span>
          </div>
        }
        open={showHistoryModal}
        onCancel={() => {
          setShowHistoryModal(false);
          setRevisionHistory([]);
        }}
        footer={null}
        width={900}
        bodyStyle={{ maxHeight: "70vh", overflow: "hidden" }}
      >
        {isLoadingHistory ? (
          <div className="flex justify-center items-center py-8">
            <Spin size="large" />
          </div>
        ) : (
          <div
            style={{
              maxHeight: "60vh",
              overflowY: "auto",
              padding: "24px",
            }}
          >
            {revisionHistory.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                Nenhum histórico encontrado para este template.
              </div>
            ) : (
              <Timeline>
                {revisionHistory.map((revision) => {
                  const revisionInfo = getRevisionTypeInfo(
                    revision.revisionType
                  );
                  return (
                    <Timeline.Item
                      key={revision.id}
                      color={revisionInfo.color}
                      dot={
                        <div className="flex items-center justify-center w-6 h-6 rounded-full bg-white border-2 border-current">
                          {revisionInfo.icon}
                        </div>
                      }
                    >
                      <div className="pb-4">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <span className="font-medium text-gray-800">
                              Revisão #{revision.revisionNumber}
                            </span>
                            <Badge
                              count={revisionInfo.label}
                              style={{
                                backgroundColor:
                                  revisionInfo.color === "green"
                                    ? "#52c41a"
                                    : revisionInfo.color === "blue"
                                    ? "#1890ff"
                                    : revisionInfo.color === "red"
                                    ? "#ff4d4f"
                                    : revisionInfo.color === "orange"
                                    ? "#fa8c16"
                                    : "#8c8c8c",
                                fontSize: "10px",
                                height: "18px",
                                lineHeight: "18px",
                                minWidth: "50px",
                              }}
                            />
                            {revision.editedByUserName && (
                              <span className="text-sm text-gray-500">
                                por {revision.editedByUserName}
                              </span>
                            )}
                          </div>
                          <span className="text-sm text-gray-500">
                            {dayjs(revision.revisionTimestamp).format(
                              "DD/MM/YYYY HH:mm"
                            )}
                          </span>
                        </div>
                        <div className="bg-gray-50 p-3 rounded-lg border">
                          <p className="text-sm text-gray-700 whitespace-pre-wrap break-words">
                            {revision.content}
                          </p>
                        </div>
                      </div>
                    </Timeline.Item>
                  );
                })}
              </Timeline>
            )}
          </div>
        )}
      </Modal>

      <TemplateModal
        show={showTemplateModal}
        template={editingTemplate}
        onClose={() => {
          setShowTemplateModal(false);
          setEditingTemplate(null);
        }}
        onSave={handleSaveTemplate}
      />

      {/* Modal de Edição de Agente */}
      <Modal
        title={
          <div className="flex items-center gap-2">
            <User className="w-5 h-5 text-red-600" />
            <span>{selectedAgent ? "Editar Agente IA" : "Criar Novo Agente IA"}</span>
          </div>
        }
        open={showAgentModal}
        onCancel={() => {
          setShowAgentModal(false);
          setSelectedAgent(null);
          setAgentConfig({
            name: "",
            description: "",
            avatarBase64: "",
            aiModelId: "default",
            temperament: "AMIGAVEL",
            maxResponseLength: 500,
            temperature: 0.7,
            isActive: true,
          });
        }}
        footer={null}
        width={700}
      >
        <div className="space-y-6 py-4">
          {/* Avatar e Nome */}
          <div className="flex items-start gap-6 p-4 bg-gray-50 rounded-lg">
            <AvatarUpload
              value={agentConfig.avatarBase64}
              onChange={(base64) => {
                console.log('🔵 ConfigurationPage Modal: AvatarUpload onChange called', {
                  base64Length: base64?.length || 0,
                  base64Preview: base64?.substring(0, 50) + '...' || 'null'
                });
                handleAgentConfigChange("avatarBase64", base64 || "");
              }}
              size={80}
              placeholder="Upload avatar"
            />
            <div className="flex-1 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Nome do Agente *
                </label>
                <Input
                  value={agentConfig.name}
                  onChange={(e) =>
                    handleAgentConfigChange("name", e.target.value)
                  }
                  placeholder="Ex: Sofia, Ana, João..."
                  size="large"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Descrição
                </label>
                <TextArea
                  value={agentConfig.description}
                  onChange={(e) =>
                    handleAgentConfigChange("description", e.target.value)
                  }
                  placeholder="Descreva a função e especialidade do agente..."
                  rows={2}
                />
              </div>
            </div>
          </div>

          {/* Configurações */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3">
                Temperamento *
              </label>
              <Radio.Group
                value={agentConfig.temperament}
                onChange={(e: RadioChangeEvent) =>
                  handleAgentConfigChange("temperament", e.target.value)
                }
                className="w-full"
              >
                <div className="space-y-2">
                  {[
                    { value: "FORMAL", label: "Formal e Respeitoso" },
                    { value: "AMIGAVEL", label: "Amigável e Acolhedor" },
                    { value: "DESCONTRAIDO", label: "Descontraído e Informal" },
                    { value: "SERIO", label: "Sério e Profissional" },
                    { value: "EMPATICO", label: "Empático e Compreensivo" },
                  ].map((option) => (
                    <Radio.Button
                      key={option.value}
                      value={option.value}
                      className="w-full h-10 flex items-center text-left"
                    >
                      <span className="font-medium">{option.label}</span>
                    </Radio.Button>
                  ))}
                </div>
              </Radio.Group>
            </div>

            <div>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                <div className="flex items-center gap-2 mb-1">
                  <h4 className="text-sm font-semibold text-blue-900">
                    ✨ Modelo de IA
                  </h4>
                </div>
                <div className="text-xs text-blue-800">
                  Sistema otimizado automaticamente
                </div>
              </div>
            </div>
          </div>

          {/* Configurações Avançadas */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Limite de Caracteres
              </label>
              <Input
                type="number"
                value={agentConfig.maxResponseLength}
                onChange={(e) =>
                  handleAgentConfigChange("maxResponseLength", parseInt(e.target.value) || 500)
                }
                min={1}
                max={10000}
                size="large"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Temperatura
              </label>
              <Input
                type="number"
                value={agentConfig.temperature}
                onChange={(e) =>
                  handleAgentConfigChange("temperature", parseFloat(e.target.value) || 0.7)
                }
                min={0}
                max={1}
                step={0.1}
                size="large"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Status
              </label>
              <Radio.Group
                value={agentConfig.isActive}
                onChange={(e: RadioChangeEvent) =>
                  handleAgentConfigChange("isActive", e.target.value)
                }
                className="w-full"
              >
                <Radio.Button value={true} className="w-16">
                  Ativo
                </Radio.Button>
                <Radio.Button value={false} className="w-16">
                  Inativo
                </Radio.Button>
              </Radio.Group>
            </div>
          </div>

          {/* Botões de ação */}
          <div className="flex items-center justify-end gap-3 pt-6 border-t border-gray-200">
            <Button
              size="large"
              onClick={() => {
                setShowAgentModal(false);
                setSelectedAgent(null);
              }}
            >
              Cancelar
            </Button>
            <Button
              type="primary"
              size="large"
              onClick={selectedAgent ? handleUpdateAgent : handleSaveAgent}
              disabled={!agentConfig.name || !agentConfig.temperament}
              className="bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
            >
              {selectedAgent ? "Atualizar Agente" : "Criar Agente"}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
