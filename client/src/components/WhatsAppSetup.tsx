import React, { useState, useEffect } from 'react';
import { Card, Button, Steps, Form, Input, Select, Typography, Alert, Space, Table, Tag, message } from 'antd';
import { PhoneOutlined, QrcodeOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { whatsappSetupApi } from '../api/services/whatsappSetupApi';
import ZApiActivation from './ZApiActivation';
import type { WhatsAppSetupStatus, WhatsAppInstance, MessagingProvider } from '../types';
import type { MessagingProviderInfo } from '../api/services/whatsappSetupApi';

const { Title, Text } = Typography;
const { Step } = Steps;

interface WhatsAppSetupProps {
  onSetupComplete?: () => void;
}

const WhatsAppSetup: React.FC<WhatsAppSetupProps> = ({ onSetupComplete }) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [setupStatus, setSetupStatus] = useState<WhatsAppSetupStatus | null>(null);
  const [selectedInstance, setSelectedInstance] = useState<WhatsAppInstance | null>(null);
  const [providers, setProviders] = useState<MessagingProviderInfo[]>([]);
  
  const [form] = Form.useForm();

  useEffect(() => {
    loadSetupStatus();
    loadProviders();
  }, []);

  const loadSetupStatus = async () => {
    try {
      setLoading(true);
      const status = await whatsappSetupApi.getSetupStatus();
      setSetupStatus(status);
      
      // If already has instances, show management view
      if (!status.requiresSetup) {
        setCurrentStep(3);
      }
    } catch (error) {
      console.error('Error loading setup status:', error);
      message.error('Erro ao carregar status de configuração');
    } finally {
      setLoading(false);
    }
  };

  const loadProviders = async () => {
    try {
      const providersList = await whatsappSetupApi.getAvailableProviders();
      setProviders(providersList);
    } catch (error) {
      console.error('Error loading providers:', error);
    }
  };

  const handleCreateInstance = async (values: { phoneNumber: string; displayName?: string; provider: MessagingProvider }) => {
    try {
      setLoading(true);
      
      // Validate phone number
      const validation = whatsappSetupApi.validatePhoneNumber(values.phoneNumber);
      if (!validation.valid) {
        message.error(validation.error);
        return;
      }

      const instance = await whatsappSetupApi.createInstance({
        phoneNumber: validation.formatted!,
        displayName: values.displayName || `WhatsApp ${validation.formatted}`
      });

      message.success('Instância criada com sucesso!');
      setSelectedInstance(instance);
      setCurrentStep(1);
      await loadSetupStatus();
      
    } catch (error: unknown) {
      console.error('Error creating instance:', error);
      const errorMessage = error instanceof Error ? error.message : 'Erro ao criar instância';
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleConfigureInstance = async (values: { instanceId: string; accessToken: string }) => {
    if (!selectedInstance) return;

    try {
      setLoading(true);
      
      await whatsappSetupApi.configureInstance(selectedInstance.id, {
        instanceId: values.instanceId,
        accessToken: values.accessToken
      });

      message.success('Instância configurada com sucesso!');
      setCurrentStep(2);
      await loadSetupStatus();
      
    } catch (error: unknown) {
      console.error('Error configuring instance:', error);
      const errorMessage = error instanceof Error ? error.message : 'Erro ao configurar instância';
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleActivateInstance = async () => {
    if (!selectedInstance) return;

    try {
      setLoading(true);
      
      const result = await whatsappSetupApi.activateInstance(selectedInstance.id);
      
      if (result.success) {
        message.success('Ativação iniciada! Escaneie o QR code.');
        setCurrentStep(2);
      } else {
        message.error(result.error || 'Erro na ativação');
      }
      
      await loadSetupStatus();
      
    } catch (error: unknown) {
      console.error('Error activating instance:', error);
      const errorMessage = error instanceof Error ? error.message : 'Erro ao ativar instância';
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSetupComplete = () => {
    message.success('Configuração do WhatsApp concluída!');
    if (onSetupComplete) {
      onSetupComplete();
    }
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'CONNECTED': return 'success';
      case 'CONNECTING': 
      case 'CONFIGURING': 
      case 'AWAITING_QR_SCAN': return 'processing';
      case 'ERROR': return 'error';
      case 'DISCONNECTED': return 'warning';
      default: return 'default';
    }
  };

  const getStatusText = (status: string): string => {
    const statusMap: Record<string, string> = {
      'NOT_CONFIGURED': 'Não Configurado',
      'CONFIGURING': 'Configurando',
      'AWAITING_QR_SCAN': 'Aguardando QR Code',
      'CONNECTING': 'Conectando',
      'CONNECTED': 'Conectado',
      'DISCONNECTED': 'Desconectado',
      'ERROR': 'Erro',
      'SUSPENDED': 'Suspenso'
    };
    return statusMap[status] || status;
  };

  const instanceColumns = [
    {
      title: 'Telefone',
      dataIndex: 'phoneNumber',
      key: 'phoneNumber',
      render: (phone: string) => whatsappSetupApi.formatPhoneNumber(phone)
    },
    {
      title: 'Nome',
      dataIndex: 'displayName',
      key: 'displayName'
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>
          {getStatusText(status)}
        </Tag>
      )
    },
    {
      title: 'Principal',
      dataIndex: 'isPrimary',
      key: 'isPrimary',
      render: (isPrimary: boolean) => isPrimary ? <CheckCircleOutlined /> : null
    },
    {
      title: 'Última Conexão',
      dataIndex: 'lastConnectedAt',
      key: 'lastConnectedAt',
      render: (date: string) => date ? new Date(date).toLocaleString() : '-'
    }
  ];

  // If setup is not required, show management interface
  if (setupStatus && !setupStatus.requiresSetup) {
    return (
      <Card>
        <Title level={3}>Gerenciar Instâncias WhatsApp</Title>
        <Space direction="vertical" className="w-full" size="large">
          <Alert
            type="success"
            message="WhatsApp Configurado"
            description="Sua empresa já possui instâncias WhatsApp configuradas."
            showIcon
          />
          
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
              rules={[
                { required: true, message: 'Número é obrigatório' },
                {
                  validator: (_, value) => {
                    if (!value) return Promise.resolve();
                    const validation = whatsappSetupApi.validatePhoneNumber(value);
                    return validation.valid 
                      ? Promise.resolve() 
                      : Promise.reject(new Error(validation.error));
                  }
                }
              ]}
            >
              <Input 
                placeholder="(11) 99999-9999 ou 5511999999999"
                prefix={<PhoneOutlined />}
              />
            </Form.Item>

            <Form.Item
              name="displayName"
              label="Nome de Exibição (Opcional)"
            >
              <Input placeholder="Ex: WhatsApp Vendas" />
            </Form.Item>

            <Form.Item
              name="provider"
              label="Provedor"
              initialValue="Z_API"
            >
              <Select>
                {providers.filter(p => p.available).map(provider => (
                  <Select.Option key={provider.provider} value={provider.provider}>
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
              rules={[{ required: true, message: 'ID da instância é obrigatório' }]}
            >
              <Input placeholder="Ex: 3C4E1234567890AB" />
            </Form.Item>

            <Form.Item
              name="accessToken"
              label="Token de Acesso"
              rules={[{ required: true, message: 'Token é obrigatório' }]}
            >
              <Input.Password placeholder="Token da Z-API" />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" loading={loading}>
                  Configurar
                </Button>
                <Button onClick={() => setCurrentStep(0)}>
                  Voltar
                </Button>
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

          <ZApiActivation />

          <div className="text-center mt-6">
            <Space>
              <Button type="primary" onClick={handleActivateInstance} loading={loading}>
                Ativar Instância
              </Button>
              <Button type="default" onClick={handleSetupComplete}>
                Concluir Configuração
              </Button>
              <Button onClick={() => setCurrentStep(1)}>
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
    </Card>
  );
};

export default WhatsAppSetup;