import React, { useState, useEffect } from "react";
import {
  Card,
  Button,
  Steps,
  Form,
  Input,
  Select,
  Typography,
  Alert,
  Space,
  Table,
  Tag,
  message,
  Modal,
} from "antd";
import { useNavigate } from "react-router-dom";
import {
  PhoneOutlined,
  QrcodeOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  DisconnectOutlined,
  SyncOutlined,
} from "@ant-design/icons";
import { whatsappSetupApi } from "../api/services/whatsappSetupApi";
import { useWebSocket } from "../hooks/useWebSocket";
import ZApiActivation from "./ZApiActivation";
import type {
  WhatsAppSetupStatus,
  WhatsAppInstanceWithStatus,
  MessagingProvider,
} from "../types";
import type { MessagingProviderInfo } from "../api/services/whatsappSetupApi";

const { Title, Text } = Typography;
const { Step } = Steps;

interface WhatsAppSetupProps {
  onSetupComplete?: () => void;
}

const WhatsAppSetup: React.FC<WhatsAppSetupProps> = ({ onSetupComplete }) => {
  const navigate = useNavigate();
  const { onInstanceConnected } = useWebSocket();
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [setupStatus, setSetupStatus] = useState<WhatsAppSetupStatus | null>(
    null
  );
  const [selectedInstance, setSelectedInstance] =
    useState<WhatsAppInstanceWithStatus | null>(null);
  const [providers, setProviders] = useState<MessagingProviderInfo[]>([]);
  const [reconnectModalVisible, setReconnectModalVisible] = useState(false);
  const [reconnectingInstance, setReconnectingInstance] =
    useState<WhatsAppInstanceWithStatus | null>(null);

  const [form] = Form.useForm();

  useEffect(() => {
    loadSetupStatus();
    loadProviders();

    // Set up WebSocket callback for instance connection
    onInstanceConnected((instanceId: string, phoneNumber: string) => {
      console.log("Instance connected via WebSocket:", instanceId, phoneNumber);
      message.success(
        `WhatsApp ${whatsappSetupApi.formatPhoneNumber(
          phoneNumber
        )} conectado com sucesso!`
      );

      // If we're currently on QR step or setup process, redirect to chat
      if (
        currentStep === 2 ||
        (selectedInstance && selectedInstance.id === instanceId)
      ) {
        console.log("Redirecting to chat after successful connection");
        if (onSetupComplete) {
          onSetupComplete();
        } else {
          navigate("/", { replace: true });
        }
      }
    });
  }, [
    onInstanceConnected,
    currentStep,
    selectedInstance,
    onSetupComplete,
    navigate,
  ]);

  // Polling para atualizar status da instância quando estiver no step 2 (QR code)
  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;

    if (currentStep === 2 && selectedInstance && !selectedInstance.connected) {
      interval = setInterval(async () => {
        try {
          const status = await whatsappSetupApi.getSetupStatus();
          const updatedInstance = status.instances.find(
            (i) => i.id === selectedInstance.id
          );

          if (
            updatedInstance &&
            updatedInstance.connected !== selectedInstance.connected
          ) {
            setSelectedInstance(updatedInstance);
            setSetupStatus(status);

            if (updatedInstance.connected) {
              message.success("WhatsApp conectado com sucesso!");
            }
          }
        } catch (error) {
          console.error("Error polling status:", error);
        }
      }, 5000); // Poll a cada 5 segundos
    }

    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [currentStep, selectedInstance]);

  useEffect(() => {
    if (
      setupStatus &&
      setupStatus.hasConfiguredInstance &&
      setupStatus.hasConnectedInstance
    ) {
      console.log("Setup complete: redirecting to chat page");
      // if (onSetupComplete) {
      //   onSetupComplete();
      // } else {
      //   navigate("/", { replace: true });
      // }
    }
  }, [setupStatus, onSetupComplete, navigate]);

  const loadSetupStatus = async () => {
    try {
      setLoading(true);
      console.log("🔄 Loading setup status...");
      const status = await whatsappSetupApi.getSetupStatusWithRealTimeCheck();
      console.log("📊 Setup status loaded:", {
        requiresSetup: status.requiresSetup,
        hasConfiguredInstance: status.hasConfiguredInstance,
        hasConnectedInstance: status.hasConnectedInstance,
        totalInstances: status.totalInstances,
        instances: status.instances
      });
      setSetupStatus(status);

      // Determine current step based on status - prioritize active instances that need setup
      const instanceNeedingSetup = status.instances.find(
        (instance) =>
          instance.status === "NOT_CONFIGURED" ||
          instance.status === "CONFIGURING" ||
          instance.status === "AWAITING_QR_SCAN"
      );

      const disconnectedInstances = status.instances.filter(
        (i) => !i.connected && i.isActive
      );

      console.log("🔍 Step determination logic:", {
        instanceNeedingSetup: instanceNeedingSetup,
        hasConnectedInstance: status.hasConnectedInstance,
        disconnectedInstancesCount: disconnectedInstances.length,
        totalInstances: status.totalInstances
      });

      if (instanceNeedingSetup) {
        // If there's an instance that needs setup, handle it first
        console.log("🔧 Instance needs setup, going to configuration:", instanceNeedingSetup);
        setSelectedInstance(instanceNeedingSetup);
        if (instanceNeedingSetup.status === "NOT_CONFIGURED") {
          console.log("📝 Setting step to 1 (configuration)");
          setCurrentStep(1); // Go to configuration step
        } else if (
          instanceNeedingSetup.status === "CONFIGURING" ||
          instanceNeedingSetup.status === "AWAITING_QR_SCAN"
        ) {
          console.log("📱 Setting step to 2 (activation)");
          setCurrentStep(2); // Go to activation step
        }
      } else if (
        status.hasConnectedInstance ||
        disconnectedInstances.length > 0
      ) {
        // If has connected instances OR disconnected instances, show management
        console.log("🎛️ Setting step to 3 (management) - connected:", status.hasConnectedInstance, "disconnected:", disconnectedInstances.length);
        setCurrentStep(3);
      } else if (status.totalInstances === 0) {
        // If no instances, stay at step 0 for creating new instance
        console.log("➕ Setting step to 0 (create instance)");
        setCurrentStep(0);
      } else {
        // Fallback: if has instances but none need setup and none connected, go to management
        console.log("🎛️ Fallback: Setting step to 3 (management)");
        setCurrentStep(3);
      }
    } catch (error) {
      console.error("Error loading setup status:", error);
      message.error("Erro ao carregar status de configuração");
    } finally {
      setLoading(false);
    }
  };

  const loadProviders = async () => {
    try {
      const providersList = await whatsappSetupApi.getAvailableProviders();
      setProviders(providersList);
    } catch (error) {
      console.error("Error loading providers:", error);
    }
  };

  const handleCreateInstance = async (values: {
    phoneNumber: string;
    displayName?: string;
    provider: MessagingProvider;
  }) => {
    try {
      setLoading(true);

      // Validate phone number
      const validation = whatsappSetupApi.validatePhoneNumber(
        values.phoneNumber
      );
      if (!validation.valid) {
        message.error(validation.error);
        return;
      }

      const instance = await whatsappSetupApi.createInstance({
        phoneNumber: validation.formatted!,
        displayName: values.displayName || `WhatsApp ${validation.formatted}`,
      });

      message.success("Instância criada com sucesso!");
      setCurrentStep(1);
      // Reload setup status to get the instance with status
      const status = await whatsappSetupApi.getSetupStatus();
      setSetupStatus(status);

      // Find the created instance in the updated status
      const createdInstanceWithStatus = status.instances.find(
        (i) => i.id === instance.id
      );
      if (createdInstanceWithStatus) {
        setSelectedInstance(createdInstanceWithStatus);
      }
    } catch (error: unknown) {
      console.error("Error creating instance:", error);
      const errorMessage =
        error instanceof Error ? error.message : "Erro ao criar instância";
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleConfigureInstance = async (values: {
    instanceId: string;
    accessToken: string;
  }) => {
    if (!selectedInstance) return;

    try {
      setLoading(true);

      await whatsappSetupApi.configureInstance(selectedInstance.id, {
        instanceId: values.instanceId,
        accessToken: values.accessToken,
      });

      message.success("Instância configurada com sucesso!");
      setCurrentStep(2);
      // Reload setup status but don't change step automatically
      const status = await whatsappSetupApi.getSetupStatus();
      setSetupStatus(status);
    } catch (error: unknown) {
      console.error("Error configuring instance:", error);
      const errorMessage =
        error instanceof Error ? error.message : "Erro ao configurar instância";
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleActivateInstance = async () => {
    if (!selectedInstance) return;

    try {
      setLoading(true);

      const result = await whatsappSetupApi.activateInstance(
        selectedInstance.id
      );

      if (result.success) {
        message.success("Ativação iniciada! Escaneie o QR code.");
        setCurrentStep(2);
      } else {
        message.error(result.error || "Erro na ativação");
      }

      // Reload setup status but don't change step automatically
      const status = await whatsappSetupApi.getSetupStatus();
      setSetupStatus(status);
    } catch (error: unknown) {
      console.error("Error activating instance:", error);
      const errorMessage =
        error instanceof Error ? error.message : "Erro ao ativar instância";
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSetupComplete = async () => {
    try {
      // Verificar status atual antes de concluir
      setLoading(true);
      const currentStatus = await whatsappSetupApi.getSetupStatus();

      // Verificar se há pelo menos uma instância conectada
      const hasConnectedInstance = currentStatus.instances.some(
        (instance) => instance.connected
      );

      if (!hasConnectedInstance) {
        message.warning(
          "Por favor, complete o scan do QR code antes de concluir a configuração."
        );
        return;
      }

      message.success("Configuração do WhatsApp concluída!");
      if (onSetupComplete) {
        onSetupComplete();
      } else {
        // Se não há callback, navegar para a página principal
        navigate("/", { replace: true });
      }
    } catch (error) {
      console.error("Error checking setup status:", error);
      message.error("Erro ao verificar status da configuração");
    } finally {
      setLoading(false);
    }
  };

  const handleReconnectInstance = async (
    instance: WhatsAppInstanceWithStatus
  ) => {
    try {
      setLoading(true);
      setReconnectingInstance(instance);

      const result = await whatsappSetupApi.reconnectInstance(instance.id);

      if (result.success) {
        if (result.zapiStatus?.connected) {
          message.success("Instância já está conectada!");
          // Reload status
          await loadSetupStatus();
        } else {
          message.info(
            "QR Code necessário para reconectar. Escaneie com seu WhatsApp."
          );
          setReconnectModalVisible(true);
          setSelectedInstance(instance);
          setCurrentStep(2); // Go to QR step
        }
      } else {
        message.error(result.error || "Erro ao reconectar instância");
      }
    } catch (error: unknown) {
      console.error("Error reconnecting instance:", error);
      const errorMessage =
        error instanceof Error ? error.message : "Erro ao reconectar instância";
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleCheckConnectionStatus = async (
    instance: WhatsAppInstanceWithStatus
  ) => {
    try {
      setLoading(true);

      const result = await whatsappSetupApi.checkConnectionStatus(instance.id);

      if (result.success) {
        const { zapiStatus } = result;

        if (zapiStatus.connected && !instance.connected) {
          message.success("Instância reconectada automaticamente!");
          await loadSetupStatus();
        } else if (!zapiStatus.connected) {
          message.warning(
            `Instância desconectada: ${
              zapiStatus.error || "Motivo desconhecido"
            }`
          );
        } else {
          message.info(
            `Status atual: ${
              zapiStatus.connected ? "Conectado" : "Desconectado"
            }`
          );
        }
      } else {
        message.error(result.error || "Erro ao verificar status");
      }
    } catch (error: unknown) {
      console.error("Error checking connection status:", error);
      const errorMessage =
        error instanceof Error ? error.message : "Erro ao verificar status";
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleForceStatusCheck = async (
    instance: WhatsAppInstanceWithStatus
  ) => {
    try {
      setLoading(true);

      message.info("Forçando verificação de status...");

      const result = await whatsappSetupApi.forceStatusCheck(instance.id);

      if (result.success) {
        message.success(
          `Status atualizado: ${
            result.zapiStatus?.connected ? "Conectado" : "Desconectado"
          }`
        );
        if (result.zapiStatus?.error) {
          message.warning(`Erro detectado: ${result.zapiStatus.error}`);
        }
        await loadSetupStatus();
      } else {
        message.error(result.error || "Erro ao forçar verificação de status");
      }
    } catch (error: unknown) {
      console.error("Error forcing status check:", error);
      const errorMessage =
        error instanceof Error ? error.message : "Erro ao forçar verificação";
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case "CONNECTED":
        return "success";
      case "CONNECTING":
      case "CONFIGURING":
      case "AWAITING_QR_SCAN":
        return "processing";
      case "ERROR":
        return "error";
      case "DISCONNECTED":
        return "warning";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string): string => {
    const statusMap: Record<string, string> = {
      NOT_CONFIGURED: "Não Configurado",
      CONFIGURING: "Configurando",
      AWAITING_QR_SCAN: "Aguardando QR Code",
      CONNECTING: "Conectando",
      CONNECTED: "Conectado",
      DISCONNECTED: "Desconectado",
      ERROR: "Erro",
      SUSPENDED: "Suspenso",
    };
    return statusMap[status] || status;
  };

  const instanceColumns = [
    {
      title: "Telefone",
      dataIndex: "phoneNumber",
      key: "phoneNumber",
      render: (phone: string) => whatsappSetupApi.formatPhoneNumber(phone),
    },
    {
      title: "Nome",
      dataIndex: "displayName",
      key: "displayName",
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>
      ),
    },
    {
      title: "Principal",
      dataIndex: "isPrimary",
      key: "isPrimary",
      render: (isPrimary: boolean) =>
        isPrimary ? <CheckCircleOutlined /> : null,
    },
    {
      title: "Erro",
      dataIndex: "error",
      key: "error",
      render: (error: string) =>
        error ? (
          <span style={{ color: "#ff4d4f", fontSize: "12px" }}>
            {error.length > 30 ? `${error.substring(0, 30)}...` : error}
          </span>
        ) : (
          "-"
        ),
    },
    {
      title: "Ações",
      key: "actions",
      render: (_: unknown, instance: WhatsAppInstanceWithStatus) => (
        <Space>
          {!instance.connected && instance.isActive && (
            <>
              <Button
                type="primary"
                size="small"
                icon={<QrcodeOutlined />}
                onClick={() => handleReconnectInstance(instance)}
                loading={loading && reconnectingInstance?.id === instance.id}
              >
                Reconectar
              </Button>
              <Button
                size="small"
                icon={<ReloadOutlined />}
                onClick={() => handleCheckConnectionStatus(instance)}
                loading={loading}
              >
                Verificar
              </Button>
            </>
          )}
          {instance.connected && (
            <Button
              size="small"
              icon={<ReloadOutlined />}
              onClick={() => handleCheckConnectionStatus(instance)}
              loading={loading}
            >
              Verificar
            </Button>
          )}

          {/* Botão para forçar verificação sempre presente */}
          <Button
            size="small"
            icon={<SyncOutlined />}
            onClick={() => handleForceStatusCheck(instance)}
            loading={loading}
            type="dashed"
          >
            Forçar Status
          </Button>
        </Space>
      ),
    },
  ];

  // Show management interface if we have connected instances OR disconnected instances AND currentStep is 3
  console.log("🎯 Render check - Management interface:", {
    setupStatus: !!setupStatus,
    hasConnectedInstance: setupStatus?.hasConnectedInstance,
    hasDisconnectedInstances: setupStatus?.instances.some((i) => !i.connected && i.isActive),
    currentStep: currentStep,
    shouldShowManagement: setupStatus &&
      (setupStatus.hasConnectedInstance ||
        setupStatus.instances.some((i) => !i.connected && i.isActive)) &&
      currentStep === 3
  });

  if (
    setupStatus &&
    (setupStatus.hasConnectedInstance ||
      setupStatus.instances.some((i) => !i.connected && i.isActive)) &&
    currentStep === 3
  ) {
    const disconnectedInstances = setupStatus.instances.filter(
      (i) => !i.connected && i.isActive
    );

    console.log("🎛️ Showing management interface with disconnected instances:", disconnectedInstances.length);

    return (
      <Card>
        <Title level={3}>Gerenciar Instâncias WhatsApp</Title>
        <Space direction="vertical" className="w-full" size="large">
          {disconnectedInstances.length > 0 ? (
            <Alert
              type="warning"
              message="Instâncias Desconectadas Detectadas"
              description={`${disconnectedInstances.length} instância(s) desconectada(s). Use o botão "Reconectar" para exibir o QR Code e reconectar.`}
              showIcon
              icon={<DisconnectOutlined />}
            />
          ) : (
            <Alert
              type="success"
              message="WhatsApp Configurado"
              description="Todas as instâncias WhatsApp estão conectadas e funcionando."
              showIcon
            />
          )}

          <Table
            columns={instanceColumns}
            dataSource={setupStatus.instances}
            rowKey="id"
            size="small"
            pagination={false}
          />

          {setupStatus.totalInstances < setupStatus.maxAllowedInstances && (
            <Button type="dashed" onClick={() => setCurrentStep(0)}>
              Adicionar Nova Instância
            </Button>
          )}
        </Space>
      </Card>
    );
  }

  return (
    <Card className="max-w-4xl mx-auto">
      <Title level={2} className="text-center mb-8">
        Configuração do WhatsApp
      </Title>

      <Steps current={currentStep} className="mb-8">
        <Step title="Criar Instância" icon={<PhoneOutlined />} />
        <Step title="Configurar" icon={<ExclamationCircleOutlined />} />
        <Step title="Ativar QR Code" icon={<QrcodeOutlined />} />
        <Step title="Concluído" icon={<CheckCircleOutlined />} />
      </Steps>

      {/* Step 0: Create Instance */}
      {currentStep === 0 && (
        <Card title="Criar Nova Instância WhatsApp">
          <Form form={form} onFinish={handleCreateInstance} layout="vertical">
            <Form.Item
              name="phoneNumber"
              label="Número do WhatsApp"
              extra="Digite apenas DDD + número. Ex: 48999999999 será convertido para +55 (48) 99999-9999"
              rules={[
                { required: true, message: "Número é obrigatório" },
                {
                  validator: (_, value) => {
                    if (!value) return Promise.resolve();
                    const validation =
                      whatsappSetupApi.validatePhoneNumber(value);
                    return validation.valid
                      ? Promise.resolve()
                      : Promise.reject(new Error(validation.error));
                  },
                },
              ]}
            >
              <Input
                placeholder="(48) 99999-9999"
                prefix={<PhoneOutlined />}
                maxLength={15}
                onChange={(e) => {
                  const formatted = whatsappSetupApi.formatPhoneForInput(
                    e.target.value
                  );
                  form.setFieldsValue({ phoneNumber: formatted });
                }}
                onBlur={(e) => {
                  // Remove formatting for validation but keep display formatted
                  const formatted = whatsappSetupApi.formatPhoneForInput(
                    e.target.value
                  );
                  if (formatted !== e.target.value) {
                    form.setFieldsValue({ phoneNumber: formatted });
                  }
                }}
              />
            </Form.Item>

            <Form.Item name="displayName" label="Nome de Exibição (Opcional)">
              <Input placeholder="Ex: WhatsApp Vendas" />
            </Form.Item>

            <Form.Item name="provider" label="Provedor" initialValue="Z_API">
              <Select>
                {providers
                  .filter((p) => p.available)
                  .map((provider) => (
                    <Select.Option
                      key={provider.provider}
                      value={provider.provider}
                    >
                      {provider.name} - {provider.description}
                    </Select.Option>
                  ))}
              </Select>
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" loading={loading}>
                  Criar Instância
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>
      )}

      {/* Step 1: Configure Instance */}
      {currentStep === 1 && selectedInstance && (
        <Card title="Configurar Instância Z-API">
          <Alert
            type="info"
            message="Configuração Z-API"
            description="Configure os dados da sua instância Z-API. Você pode obter estes dados no painel da Z-API."
            showIcon
            className="mb-6"
          />

          <Form onFinish={handleConfigureInstance} layout="vertical">
            <Form.Item
              name="instanceId"
              label="ID da Instância Z-API"
              rules={[
                { required: true, message: "ID da instância é obrigatório" },
              ]}
            >
              <Input placeholder="Ex: 3C4E1234567890AB" />
            </Form.Item>

            <Form.Item
              name="accessToken"
              label="Token de Acesso"
              rules={[{ required: true, message: "Token é obrigatório" }]}
            >
              <Input.Password placeholder="Token da Z-API" />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" loading={loading}>
                  Configurar
                </Button>
                <Button onClick={() => setCurrentStep(0)}>Voltar</Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>
      )}

      {/* Step 2: QR Code Activation */}
      {currentStep === 2 && (
        <Card title="Ativar WhatsApp via QR Code">
          <Alert
            type="warning"
            message="Escaneamento Necessário"
            description="Use o WhatsApp do seu celular para escanear o QR code e ativar a instância."
            showIcon
            className="mb-6"
          />

          {/* Status da instância atual */}
          {selectedInstance && (
            <Alert
              type={selectedInstance.connected ? "success" : "info"}
              message={`Status: ${getStatusText(selectedInstance.status)}`}
              description={
                selectedInstance.connected
                  ? "Instância conectada com sucesso! Você pode concluir a configuração."
                  : "Escaneie o QR code abaixo para conectar a instância."
              }
              showIcon
              className="mb-6"
            />
          )}

          <ZApiActivation />

          <div className="text-center mt-6">
            <Space>
              <Button
                type="primary"
                onClick={handleActivateInstance}
                loading={loading}
              >
                Ativar Instância
              </Button>
              <Button
                type="default"
                onClick={handleSetupComplete}
                loading={loading}
                disabled={!selectedInstance?.connected}
              >
                Concluir Configuração
              </Button>
              <Button onClick={() => setCurrentStep(1)} disabled={loading}>
                Voltar
              </Button>
            </Space>
          </div>
        </Card>
      )}

      {/* Step 3: Setup Complete */}
      {currentStep === 3 && (
        <Card title="Configuração Concluída">
          <div className="text-center">
            <CheckCircleOutlined className="text-6xl text-green-500 mb-4" />
            <Title level={3}>WhatsApp Configurado com Sucesso!</Title>
            <Text type="secondary">
              Sua instância WhatsApp está ativa e pronta para uso.
            </Text>

            <div className="mt-6">
              <Button type="primary" size="large" onClick={handleSetupComplete}>
                Começar a Usar
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Reconnection Modal */}
      <Modal
        title="Reconectar Instância WhatsApp"
        open={reconnectModalVisible}
        onCancel={() => setReconnectModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setReconnectModalVisible(false)}>
            Cancelar
          </Button>,
          <Button
            key="done"
            type="primary"
            onClick={() => {
              setReconnectModalVisible(false);
              setCurrentStep(3);
              loadSetupStatus();
            }}
          >
            Concluir
          </Button>,
        ]}
        width={800}
      >
        <div className="text-center mb-4">
          <Alert
            type="info"
            message="Reconexão Necessária"
            description={`Escaneie o QR Code com o WhatsApp do número ${
              reconnectingInstance?.phoneNumber
                ? whatsappSetupApi.formatPhoneNumber(
                    reconnectingInstance.phoneNumber
                  )
                : ""
            } para reconectar a instância.`}
            showIcon
            className="mb-4"
          />
        </div>

        {/* Show QR code component */}
        <ZApiActivation />
      </Modal>
    </Card>
  );
};

export default WhatsAppSetup;
