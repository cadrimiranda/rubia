import React, { useState, useEffect, useCallback } from "react";
import { User, Check, Plus, Edit3, Trash2, MoreVertical } from "lucide-react";
import { Button, Select, Input, message, Dropdown, Modal, Slider } from "antd";
import { AvatarUpload } from "../AvatarUpload";
import { aiAgentApi, type AIAgent } from "../../api/services/aiAgentApi";
import { aiModelService } from "../../services/aiModelService";
import { useCurrentUser } from "../../store/useAuthStore";

const { Option } = Select;
const { TextArea } = Input;

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

type AgentManagementProps = object;

export const AgentManagement: React.FC<AgentManagementProps> = () => {
  const user = useCurrentUser();

  const [agentConfig, setAgentConfig] = useState<AgentConfig>({
    name: "",
    description: "",
    avatarBase64: "",
    aiModelId: "default",
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
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [agentToDelete, setAgentToDelete] = useState<AIAgent | null>(null);

  // Carregar modelo padrão
  useEffect(() => {
    const loadDefaultModel = async () => {
      if (!agentConfig.aiModelId || agentConfig.aiModelId === "default") {
        try {
          const models = await aiModelService.getActiveModels();
          if (models.length > 0) {
            setAgentConfig((prev) => ({ ...prev, aiModelId: models[0].id }));
          }
        } catch (error) {
          console.error("Erro ao carregar modelo padrão:", error);
        }
      }
    };

    loadDefaultModel();
  }, [agentConfig.aiModelId]);

  // Carregar agentes existentes da empresa
  const loadExistingAgents = useCallback(async () => {
    if (!user?.companyId) {
      console.log("🔄 [DEBUG] loadExistingAgents: No companyId available");
      return;
    }

    console.log(
      "🔄 [DEBUG] loadExistingAgents: Loading agents for company",
      user.companyId
    );
    try {
      const agents = await aiAgentApi.getAIAgentsByCompany(user.companyId);
      console.log("🔄 [DEBUG] loadExistingAgents: Loaded agents", agents);
      setExistingAgents(agents);
      console.log(
        "🔄 [DEBUG] loadExistingAgents: State updated with",
        agents.length,
        "agents"
      );
    } catch (error) {
      console.error(
        "🔄 [ERROR] loadExistingAgents: Failed to load agents:",
        error
      );
      message.error("Erro ao carregar agentes existentes");
    }
  }, [user?.companyId]);

  // Verificar limites da empresa
  const checkAgentLimits = useCallback(async () => {
    if (!user?.companyId) {
      console.log("🔄 [DEBUG] checkAgentLimits: No companyId available");
      return;
    }

    console.log(
      "🔄 [DEBUG] checkAgentLimits: Checking limits for company",
      user.companyId
    );
    try {
      const canCreate = await aiAgentApi.canCreateAgent(user.companyId);
      const remaining = await aiAgentApi.getRemainingAgentSlots(user.companyId);

      console.log(
        "🔄 [DEBUG] checkAgentLimits: canCreate =",
        canCreate,
        "remaining =",
        remaining
      );
      setCanCreateAgent(canCreate);
      setRemainingSlots(remaining);
      console.log("🔄 [DEBUG] checkAgentLimits: State updated");
    } catch (error) {
      console.error(
        "🔄 [ERROR] checkAgentLimits: Failed to check limits:",
        error
      );
    }
  }, [user?.companyId]);

  // Função unificada para refresh completo
  const refreshAgentData = useCallback(async () => {
    console.log("🔄 [DEBUG] refreshAgentData: Starting complete refresh");
    if (!user?.companyId) {
      console.log(
        "🔄 [DEBUG] refreshAgentData: No companyId, skipping refresh"
      );
      return;
    }

    try {
      // Executar ambas as operações em paralelo
      await Promise.all([loadExistingAgents(), checkAgentLimits()]);
      console.log("🔄 [DEBUG] refreshAgentData: Complete refresh finished");
    } catch (error) {
      console.error("🔄 [ERROR] refreshAgentData: Failed to refresh:", error);
    }
  }, [user?.companyId, loadExistingAgents, checkAgentLimits]);

  // Carregar dados ao montar o componente
  useEffect(() => {
    if (user?.companyId) {
      refreshAgentData();
    }
  }, [user?.companyId, refreshAgentData]);

  const handleAgentConfigChange = (
    field: keyof AgentConfig,
    value: string | number | boolean
  ) => {
    console.log("🔵 handleAgentConfigChange called", {
      field,
      valueType: typeof value,
      valueLength: typeof value === "string" ? value.length : "n/a",
      valuePreview:
        typeof value === "string" && value.length > 50
          ? value.substring(0, 50) + "..."
          : value,
    });

    setAgentConfig((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Salvar agente
  const handleSaveAgent = async () => {
    if (!user?.companyId) {
      message.error("Erro: Dados da empresa não encontrados");
      return;
    }

    if (!agentConfig.name.trim()) {
      message.error("Nome do agente é obrigatório");
      return;
    }

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

      console.log("🚀 [DEBUG] Creating agent with data:", createAgentData);
      console.log("🚀 [DEBUG] User data:", {
        userId: user.id,
        companyId: user.companyId,
        companySlug: user.companySlug,
        role: user.role,
      });

      // Debug context before creating agent
      try {
        await aiAgentApi.debugContext();
      } catch (debugError) {
        console.error("🔍 [DEBUG] Failed to get context:", debugError);
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
      await refreshAgentData();

      // Fechar modal após sucesso
      setShowAgentModal(false);
      setSelectedAgent(null);
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
        description: agentConfig.description || undefined,
        avatarBase64: agentConfig.avatarBase64 || undefined,
        aiModelId: agentConfig.aiModelId,
        temperament: agentConfig.temperament,
        maxResponseLength: agentConfig.maxResponseLength,
        temperature: agentConfig.temperature,
        isActive: agentConfig.isActive,
      };

      const updatedAgent = await aiAgentApi.updateAIAgent(
        selectedAgent.id,
        updateData
      );

      message.success(`Agente "${updatedAgent.name}" atualizado com sucesso!`);

      // Fechar modal e recarregar lista
      setShowAgentModal(false);
      setSelectedAgent(null);
      await refreshAgentData();
    } catch (error: unknown) {
      console.error("Erro ao atualizar agente:", error);
      const errorMessage =
        (error as Error)?.message || "Erro ao atualizar agente";
      message.error(errorMessage);
    }
  };

  // Excluir agente
  const handleDeleteAgent = (agent: AIAgent) => {
    console.log("🗑️ [DEBUG] handleDeleteAgent called for:", agent.name);
    setAgentToDelete(agent);
    setShowDeleteModal(true);
  };

  // Confirmar exclusão do agente
  const handleConfirmDelete = async () => {
    if (!agentToDelete) return;

    console.log("🗑️ [DEBUG] Confirm delete for:", agentToDelete.name);
    try {
      await aiAgentApi.deleteAIAgent(agentToDelete.id);
      message.success(`Agente "${agentToDelete.name}" excluído com sucesso!`);

      // Pequeno delay para garantir que o backend processou a exclusão
      await new Promise((resolve) => setTimeout(resolve, 500));
      await refreshAgentData();
    } catch (error: unknown) {
      console.error("Erro ao excluir agente:", error);
      const errorMessage =
        (error as Error)?.message || "Erro ao excluir agente";
      message.error(`Erro ao excluir agente: ${errorMessage}`);
    } finally {
      setShowDeleteModal(false);
      setAgentToDelete(null);
    }
  };

  // Criar novo agente
  const handleCreateNewAgent = () => {
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
    setShowAgentModal(true);
  };

  return (
    <div className="max-w-4xl mx-auto p-8">
      {/* Seção de Agentes Existentes */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold text-gray-800">
              Agentes IA da Empresa
            </h3>
            <div className="bg-blue-100 text-blue-700 px-3 py-1 rounded-xl text-sm font-medium">
              {existingAgents.length} de{" "}
              {existingAgents.length + remainingSlots} agentes
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="text-sm text-gray-600">
              {remainingSlots > 0 ? (
                <span className="text-green-600 font-medium">
                  {remainingSlots} slot{remainingSlots !== 1 ? "s" : ""}{" "}
                  disponível{remainingSlots !== 1 ? "eis" : ""}
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
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 bg-gray-200 rounded-xl flex items-center justify-center">
                    {agent.avatarBase64 ? (
                      <img
                        src={agent.avatarBase64}
                        alt={agent.name}
                        className="w-10 h-10 rounded-xl object-cover"
                      />
                    ) : (
                      <User className="w-5 h-5 text-gray-500" />
                    )}
                  </div>
                  <div className="flex-1">
                    <h4 className="font-medium text-gray-800">{agent.name}</h4>
                    <p className="text-sm text-gray-500">{agent.temperament}</p>
                  </div>
                  <div
                    className={`w-2 h-2 rounded-xl ${
                      agent.isActive ? "bg-green-500" : "bg-gray-300"
                    }`}
                  />
                  <Dropdown
                    menu={{
                      items: [
                        {
                          key: "edit",
                          label: "Editar",
                          icon: <Edit3 className="w-4 h-4" />,
                          onClick: (e) => {
                            console.log(
                              "🔧 [DEBUG] Edit clicked for:",
                              agent.name
                            );
                            e?.domEvent?.stopPropagation();
                            handleEditAgent(agent);
                          },
                        },
                        {
                          key: "delete",
                          label: "Excluir",
                          icon: <Trash2 className="w-4 h-4" />,
                          danger: true,
                          onClick: (e) => {
                            console.log(
                              "🗑️ [DEBUG] Delete menu item clicked for:",
                              agent.name
                            );
                            e?.domEvent?.stopPropagation();
                            handleDeleteAgent(agent);
                          },
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

                <div className="flex items-center justify-between text-sm text-gray-500">
                  <span>Temp: {agent.temperature}</span>
                  <span>Max: {agent.maxResponseLength}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Mensagem quando limite atingido */}
      {!canCreateAgent && existingAgents.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <div className="text-center py-12">
            <div className="w-16 h-16 bg-orange-100 rounded-xl flex items-center justify-center mx-auto mb-4">
              <User className="w-8 h-8 text-orange-500" />
            </div>
            <h3 className="text-xl font-semibold text-gray-800 mb-2">
              Limite de Agentes Atingido
            </h3>
            <p className="text-gray-600 mb-4 max-w-md mx-auto">
              Você atingiu o limite máximo de agentes IA para seu plano atual.
              Para criar mais agentes, considere fazer upgrade do seu plano.
            </p>
            <div className="bg-orange-50 border border-orange-200 rounded-lg p-4 max-w-md mx-auto">
              <p className="text-sm text-orange-700">
                <strong>Agentes ativos:</strong> {existingAgents.length}
              </p>
              <p className="text-sm text-orange-700">
                <strong>Limite do plano:</strong>{" "}
                {existingAgents.length + remainingSlots}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Criação/Edição */}
      <Modal
        title={
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-red-100 rounded-xl flex items-center justify-center">
              <User className="w-5 h-5 text-red-600" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-gray-800">
                {selectedAgent
                  ? "Editar Agente IA"
                  : "Configuração do Agente IA"}
              </h3>
              <p className="text-sm text-gray-600">
                Configure a personalidade e comportamento do assistente virtual
              </p>
            </div>
          </div>
        }
        open={showAgentModal}
        onCancel={() => {
          setShowAgentModal(false);
          setSelectedAgent(null);
        }}
        width={900}
        footer={
          <div className="flex items-center justify-between pt-4 border-t border-gray-200">
            <div className="text-sm text-gray-500">
              <span className="font-medium">Dica:</span> Um bom agente precisa
              de um nome marcante e personalidade bem definida
            </div>
            <div className="flex items-center gap-3">
              <Button
                onClick={() => {
                  setShowAgentModal(false);
                  setSelectedAgent(null);
                }}
                size="large"
              >
                Cancelar
              </Button>
              <Button
                type="primary"
                onClick={selectedAgent ? handleUpdateAgent : handleSaveAgent}
                disabled={
                  !agentConfig.name.trim() ||
                  (!selectedAgent && !canCreateAgent)
                }
                size="large"
                icon={<Check />}
                className="bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600 px-8"
              >
                {selectedAgent ? "Atualizar Agente" : "Criar Agente"}
              </Button>
            </div>
          </div>
        }
      >
        <div className="space-y-8">
          {/* Seção do avatar e nome com design melhorado */}
          <div className="flex items-start gap-8 p-6 bg-gray-50 rounded-xl">
            <AvatarUpload
              value={agentConfig.avatarBase64}
              onChange={(base64) => {
                console.log(
                  "🔵 AgentManagement Modal: AvatarUpload onChange called",
                  {
                    base64Length: base64?.length || 0,
                    base64Preview: base64?.substring(0, 50) + "..." || "null",
                  }
                );
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

          {/* Configurações de Personalidade */}
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3">
                Temperamento
              </label>
              <Select
                value={agentConfig.temperament}
                onChange={(value) =>
                  handleAgentConfigChange("temperament", value)
                }
                size="large"
                className="w-full"
              >
                <Option value="AMIGAVEL">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-green-500 rounded-xl"></span>
                    Amigável - Caloroso e acolhedor
                  </div>
                </Option>
                <Option value="PROFISSIONAL">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-blue-500 rounded-xl"></span>
                    Profissional - Direto e objetivo
                  </div>
                </Option>
                <Option value="EMPATICO">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-purple-500 rounded-xl"></span>
                    Empático - Compreensivo e solidário
                  </div>
                </Option>
              </Select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-3">
                  Criatividade (Temperature)
                </label>
                <div className="px-3">
                  <Slider
                    min={0.1}
                    max={1.0}
                    step={0.1}
                    value={agentConfig.temperature}
                    onChange={(value) =>
                      handleAgentConfigChange("temperature", value)
                    }
                    marks={{
                      0.1: "Conservador",
                      0.5: "Equilibrado",
                      1.0: "Criativo",
                    }}
                  />
                </div>
                <p className="text-xs text-gray-500 mt-2">
                  Controla a criatividade das respostas
                </p>
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-3">
                  Limite de Caracteres
                </label>
                <Select
                  value={agentConfig.maxResponseLength}
                  onChange={(value) =>
                    handleAgentConfigChange("maxResponseLength", value)
                  }
                  size="large"
                  className="w-full"
                >
                  <Option value={150}>Curta (150 caracteres)</Option>
                  <Option value={300}>Média (300 caracteres)</Option>
                  <Option value={500}>Longa (500 caracteres)</Option>
                  <Option value={1000}>Muito Longa (1000 caracteres)</Option>
                </Select>
              </div>
            </div>
          </div>

          {/* Preview da mensagem */}
          <div className="p-6 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl border border-blue-100">
            <div className="flex items-start gap-4">
              <div className="w-10 h-10 bg-white rounded-xl flex items-center justify-center shadow-sm">
                {agentConfig.avatarBase64 ? (
                  <img
                    src={agentConfig.avatarBase64}
                    alt={agentConfig.name || "Agente"}
                    className="w-10 h-10 rounded-xl object-cover"
                  />
                ) : (
                  <User className="w-5 h-5 text-gray-400" />
                )}
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <span className="font-medium text-gray-800">
                    {agentConfig.name || "Seu Agente"}
                  </span>
                  <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded-xl">
                    {agentConfig.temperament}
                  </span>
                </div>
                <div className="bg-white p-3 rounded-lg shadow-sm border text-gray-700">
                  {agentConfig.temperament === "AMIGAVEL" &&
                    "Olá! 😊 Que bom ter você aqui! Como posso ajudar hoje?"}
                  {agentConfig.temperament === "PROFISSIONAL" &&
                    "Olá. Sou o assistente da empresa. Estou aqui para ajudá-lo."}
                  {agentConfig.temperament === "EMPATICO" &&
                    "Olá! Entendo que você precisa de ajuda. Estou aqui para te apoiar no que precisar! 💙"}
                </div>
              </div>
            </div>
          </div>
        </div>
      </Modal>

      {/* Modal de Confirmação de Exclusão */}
      <Modal
        title="Excluir Agente"
        open={showDeleteModal}
        onOk={handleConfirmDelete}
        onCancel={() => {
          setShowDeleteModal(false);
          setAgentToDelete(null);
        }}
        okText="Excluir"
        cancelText="Cancelar"
        okType="danger"
        centered
      >
        <p>
          Tem certeza que deseja excluir o agente{" "}
          <strong>"{agentToDelete?.name}"</strong>? Esta ação não pode ser
          desfeita.
        </p>
      </Modal>
    </div>
  );
};

export default AgentManagement;
