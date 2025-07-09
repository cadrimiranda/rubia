import React, { useState, useEffect } from "react";
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
import type { ConversationTemplate, CampaignData } from "../../types/types";
import {
  createMockCampaign,
  syncCampaignConversationsWithStore,
  simulateContactStatusChanges,
  getAllCampaignConversations,
} from "../../mocks/campaignMock";
import { useChatStore } from "../../store/useChatStore";
import {
  messageTemplateService,
  type CreateMessageTemplateRequest,
  type UpdateMessageTemplateRequest,
  type MessageTemplateRevision,
  type RevisionType,
} from "../../services/messageTemplateService";
import { useAuthStore } from "../../store/useAuthStore";

const { Option } = Select;
const { TextArea } = Input;
const { RangePicker } = DatePicker;

interface AgentConfig {
  name: string;
  avatar: string;
  speechProfile: string;
  llmType: string;
}

interface ConfigurationPageProps {
  onBack: () => void;
}


export const ConfigurationPage: React.FC<ConfigurationPageProps> = ({
  onBack,
}) => {
  const { refreshConversations } = useChatStore();
  const { user } = useAuthStore();

  const [activeTab, setActiveTab] = useState<
    "agent" | "campaign" | "templates"
  >("agent");
  const [agentConfig, setAgentConfig] = useState<AgentConfig>({
    name: "Sofia",
    avatar: "",
    speechProfile: "amigavel",
    llmType: "medio",
  });
  const [campaignData, setCampaignData] = useState<CampaignData>({
    name: "",
    description: "",
    startDate: "",
    endDate: "",
    sourceSystem: "",
  });
  const [templates, setTemplates] = useState<ConversationTemplate[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [duplicateUsers, setDuplicateUsers] = useState<string[]>([]);
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [editingTemplate, setEditingTemplate] =
    useState<ConversationTemplate | null>(null);
  const [isLoadingTemplates, setIsLoadingTemplates] = useState(false);
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [revisionHistory, setRevisionHistory] = useState<MessageTemplateRevision[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  // Carregar templates da API
  const loadTemplates = async () => {
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
  };

  // Carregar templates ao montar o componente ou quando o usuário mudar
  useEffect(() => {
    if (user?.companyId) {
      loadTemplates();
    }
  }, [user?.companyId]);

  // Função para obter ícone e cor do tipo de revisão
  const getRevisionTypeInfo = (type: RevisionType) => {
    switch (type) {
      case 'CREATE':
        return { icon: <Plus className="w-3 h-3" />, color: 'green', label: 'Criado' };
      case 'EDIT':
        return { icon: <Edit3 className="w-3 h-3" />, color: 'blue', label: 'Editado' };
      case 'DELETE':
        return { icon: <Trash2 className="w-3 h-3" />, color: 'red', label: 'Excluído' };
      case 'RESTORE':
        return { icon: <History className="w-3 h-3" />, color: 'orange', label: 'Restaurado' };
      default:
        return { icon: <Edit3 className="w-3 h-3" />, color: 'gray', label: 'Modificado' };
    }
  };

  // Carregar histórico de revisões
  const loadTemplateHistory = async (templateId: string) => {
    setIsLoadingHistory(true);
    try {
      const history = await messageTemplateService.getRevisionHistory(templateId);
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

  const handleAgentConfigChange = (field: keyof AgentConfig, value: string) => {
    setAgentConfig((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleCampaignChange = (field: keyof CampaignData, value: string) => {
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

    setIsUploading(true);

    try {
      // Simular processamento do arquivo enviado
      message.loading("Processando arquivo...", 0.5);
      await new Promise((resolve) => setTimeout(resolve, 1000));

      message.loading("Validando contatos...", 0.5);
      await new Promise((resolve) => setTimeout(resolve, 800));

      message.loading("Aplicando templates de conversa...", 0.5);
      await new Promise((resolve) => setTimeout(resolve, 1200));

      // Criar campanha mock com contatos gerados
      const result = await createMockCampaign(campaignData, selectedTemplates);

      if (result.success) {
        setDuplicateUsers(result.stats.duplicatesFound);

        // Mensagem de sucesso detalhada
        message.success({
          content: (
            <div>
              <div className="font-semibold mb-2">
                Campanha "{campaignData.name}" criada com sucesso! 🎉
              </div>
              <div className="text-sm space-y-1">
                <div>
                  📊 {result.stats.contactsProcessed} contatos processados do
                  arquivo
                </div>
                <div>
                  💬 {result.stats.conversationsCreated} conversas iniciadas
                </div>
                <div>📝 {selectedTemplates.length} templates aplicados:</div>
                <div className="pl-4 text-xs text-gray-600">
                  {result.stats.templateDistribution.map((item, index) => (
                    <div key={index}>
                      • {item.template}: {item.used} mensagens
                    </div>
                  ))}
                </div>
                <div>
                  ⚠️ {result.stats.duplicatesFound.length} duplicatas detectadas
                </div>
                <div className="mt-2 text-green-600">
                  ✅ Campanhas ativas receberão respostas automaticamente
                </div>
              </div>
            </div>
          ),
          duration: 10,
        });

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

        // Log detalhado dos templates utilizados
        console.log("🎯 Campanha criada:", result.campaign.name);
        console.log("📋 Templates aplicados:");
        result.stats.templateDistribution.forEach((item) => {
          const template = selectedTemplates.find(
            (t) => t.title === item.template
          );
          console.log(`${item.template} (${item.used}x):`);
          console.log(`"${template?.content}"`);
        });

        // Debug: verificar conversas criadas
        console.log("🔍 Debug: Verificando conversas criadas...");
        setTimeout(() => {
          const allConversations = getAllCampaignConversations();
          console.log(
            "📊 Total de conversas no sistema:",
            allConversations.length
          );
          allConversations.forEach((conv, index) => {
            console.log(
              `  ${index + 1}. ${conv.customer?.name} - Status: ${conv.status}`
            );
          });
        }, 500);

        // Sincronizar conversas com o chat store
        syncCampaignConversationsWithStore(refreshConversations);

        // Iniciar simulação de mudanças de status
        simulateContactStatusChanges();

        // Voltar para conversas após 4 segundos
        setTimeout(() => {
          onBack();
        }, 4000);
      } else {
        message.error("Erro ao criar campanha!");
      }
    } catch (error) {
      console.error("Erro ao criar campanha:", error);
      message.error("Erro ao criar campanha!");
    } finally {
      setIsUploading(false);
    }
  };

  const avatarUploadProps: UploadProps = {
    name: "avatar",
    accept: ".jpg,.jpeg,.png,.gif",
    beforeUpload: (file) => {
      const isImage = file.type.startsWith("image/");
      if (!isImage) {
        message.error("Você só pode enviar arquivos de imagem!");
        return false;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        if (e.target?.result) {
          handleAgentConfigChange("avatar", e.target.result as string);
        }
      };
      reader.readAsDataURL(file);

      return false;
    },
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
                  <div className="relative">
                    <div className="w-24 h-24 bg-gradient-to-br from-red-500 to-red-600 rounded-xl flex items-center justify-center shadow-lg">
                      {agentConfig.avatar ? (
                        <img
                          src={agentConfig.avatar}
                          alt="Agent Avatar"
                          className="w-full h-full object-cover rounded-xl"
                        />
                      ) : (
                        <User className="w-12 h-12 text-white" />
                      )}
                    </div>
                    <AntUpload {...avatarUploadProps} showUploadList={false}>
                      <button className="absolute -bottom-2 -right-2 w-8 h-8 bg-white rounded-full shadow-lg flex items-center justify-center border-2 border-red-100 hover:border-red-300 transition-colors">
                        <Upload className="w-4 h-4 text-red-600" />
                      </button>
                    </AntUpload>
                  </div>
                  <div className="flex-1">
                    <label className="block text-sm font-semibold text-gray-700 mb-3">
                      Nome do Agente
                    </label>
                    <Input
                      value={agentConfig.name}
                      onChange={(e) =>
                        handleAgentConfigChange("name", e.target.value)
                      }
                      placeholder="Ex: Sofia, Ana, João..."
                      size="large"
                      className="font-medium"
                    />
                    <p className="text-xs text-gray-500 mt-2">
                      O nome será usado nas conversas com os doadores
                    </p>
                  </div>
                </div>

                {/* Configurações principais */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                  <div className="space-y-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-4">
                        Perfil de Comunicação
                      </label>
                      <div className="space-y-2">
                        <Radio.Group
                          value={agentConfig.speechProfile}
                          onChange={(e: RadioChangeEvent) =>
                            handleAgentConfigChange(
                              "speechProfile",
                              e.target.value
                            )
                          }
                          className="w-full"
                        >
                          <div className="space-y-2">
                            <Radio.Button
                              value="formal"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Formal e Respeitoso
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="amigavel"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Amigável e Acolhedor
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="descontraido"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Descontraído e Informal
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="serio"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Sério e Profissional
                              </span>
                            </Radio.Button>
                            <Radio.Button
                              value="animado"
                              className="w-full h-12 flex items-center text-left"
                            >
                              <span className="font-medium">
                                Animado e Entusiasmado
                              </span>
                            </Radio.Button>
                          </div>
                        </Radio.Group>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-4">
                        Inteligência do Agente
                      </label>
                      <div className="space-y-2">
                        <Radio.Group
                          value={agentConfig.llmType}
                          onChange={(e: RadioChangeEvent) =>
                            handleAgentConfigChange("llmType", e.target.value)
                          }
                          className="w-full"
                        >
                          <div className="space-y-2">
                            <Radio.Button
                              value="barato"
                              className="w-full h-16 flex items-center text-left"
                            >
                              <div>
                                <div className="font-medium">Econômico</div>
                                <div className="text-xs text-gray-500">
                                  Respostas rápidas e diretas
                                </div>
                              </div>
                            </Radio.Button>
                            <Radio.Button
                              value="medio"
                              className="w-full h-16 flex items-center text-left"
                            >
                              <div>
                                <div className="font-medium">
                                  Padrão (Recomendado)
                                </div>
                                <div className="text-xs text-gray-500">
                                  Equilibrio entre custo e qualidade
                                </div>
                              </div>
                            </Radio.Button>
                            <Radio.Button
                              value="caro"
                              className="w-full h-16 flex items-center text-left"
                            >
                              <div>
                                <div className="font-medium">Premium</div>
                                <div className="text-xs text-gray-500">
                                  Máxima qualidade e contextualização
                                </div>
                              </div>
                            </Radio.Button>
                          </div>
                        </Radio.Group>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Botões de ação */}
                <div className="flex items-center justify-between pt-8 border-t border-gray-200 mt-8">
                  <div className="text-sm text-gray-500">
                    Última atualização: Hoje, 14:30
                  </div>
                  <div className="flex gap-3">
                    <Button size="large" className="px-6">
                      Testar Agente
                    </Button>
                    <Button
                      type="primary"
                      size="large"
                      className="px-8 bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
                    >
                      Salvar Configurações
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
                        {agentConfig.speechProfile === "formal" &&
                          "Bom dia! Sou a assistente virtual do Centro de Sangue. Como posso ajudá-lo hoje?"}
                        {agentConfig.speechProfile === "amigavel" &&
                          "Oi! 😊 Eu sou a assistente do Centro de Sangue! Como posso te ajudar hoje?"}
                        {agentConfig.speechProfile === "descontraido" &&
                          "E aí! Tudo bem? Sou a assistente aqui do Centro. Em que posso te ajudar?"}
                        {agentConfig.speechProfile === "serio" &&
                          "Olá. Sou a assistente do Centro de Sangue. Estou aqui para ajudá-lo."}
                        {agentConfig.speechProfile === "animado" &&
                          "Oi, oi! 🎉 Que alegria ter você aqui! Sou a assistente do Centro de Sangue!"}
                      </div>
                    </div>
                  </div>
                </div>
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
        style={{ top: 20 }}
        bodyStyle={{ padding: "24px", maxHeight: "70vh", overflow: "hidden" }}
      >
        {isLoadingHistory ? (
          <div className="flex justify-center items-center py-8">
            <Spin size="large" />
          </div>
        ) : (
          <div style={{ maxHeight: "60vh", overflowY: "auto", paddingRight: "8px" }}>
            {revisionHistory.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                Nenhum histórico encontrado para este template.
              </div>
            ) : (
              <Timeline>
                {revisionHistory.map((revision) => {
                  const revisionInfo = getRevisionTypeInfo(revision.revisionType);
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
                                  revisionInfo.color === 'green' ? '#52c41a' :
                                  revisionInfo.color === 'blue' ? '#1890ff' :
                                  revisionInfo.color === 'red' ? '#ff4d4f' :
                                  revisionInfo.color === 'orange' ? '#fa8c16' : '#8c8c8c',
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
    </div>
  );
};
