import React, { useState } from "react";
import { ArrowLeft, Upload, User, Check, Plus, Edit3, Sparkles, Trash2, MoreVertical } from "lucide-react";
import { Button, Select, Input, Upload as AntUpload, DatePicker, message, Card, Avatar, Radio, Dropdown } from "antd";
import type { UploadProps, RadioChangeEvent } from "antd";
import dayjs from "dayjs";
import { TemplateModal } from "../TemplateModal";

const { Option } = Select;
const { TextArea } = Input;
const { RangePicker } = DatePicker;

interface ConversationTemplate {
  id: string;
  title: string;
  content: string;
  selected: boolean;
  category?: string;
  isCustom?: boolean;
}

interface AgentConfig {
  name: string;
  avatar: string;
  speechProfile: string;
  llmType: string;
}

interface CampaignData {
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  sourceSystem: string;
  file?: File;
}

interface ConfigurationPageProps {
  onBack: () => void;
}

const defaultTemplates: ConversationTemplate[] = [
  {
    id: "1",
    title: "Convite Inicial Amigável",
    content: "Olá! 😊 Sou o assistente do Centro de Sangue. Que tal fazer a diferença na vida de alguém hoje? Você gostaria de saber mais sobre doação?",
    selected: true
  },
  {
    id: "2", 
    title: "Urgência Específica",
    content: "Olá! Estamos com necessidade urgente do seu tipo sanguíneo. Você poderia nos ajudar com uma doação nos próximos dias?",
    selected: false
  },
  {
    id: "3",
    title: "Lembrete Carinhoso",
    content: "Oi! Já faz um tempo que não te vemos por aqui. Que tal agendar uma nova doação? Cada gesto conta! ❤️",
    selected: true
  },
  {
    id: "4",
    title: "Primeira Doação",
    content: "Olá! Vejo que você ainda não é doador. Que tal conhecer mais sobre esse ato de amor? Posso tirar suas dúvidas!",
    selected: false
  },
  {
    id: "5",
    title: "Agradecimento e Convite",
    content: "Obrigado por ser um doador! Sua última doação foi incrível. Já pode doar novamente? Vamos agendar?",
    selected: true
  },
  {
    id: "6",
    title: "Campanha Especial",
    content: "Olá! Estamos com uma campanha especial este mês. Venha doar e ganhe um brinde especial como agradecimento!",
    selected: false
  },
  {
    id: "7",
    title: "Informativo Educativo",
    content: "Você sabia que uma única doação pode salvar até 4 vidas? Que tal ser um herói hoje? Te conto mais detalhes!",
    selected: true
  },
  {
    id: "8",
    title: "Convite para Amigos",
    content: "Olá! Que tal convidar um amigo para doar junto com você? Doação em dupla é ainda mais especial! 👫",
    selected: false
  },
  {
    id: "9",
    title: "Horário Flexível",
    content: "Oi! Sei que sua agenda é corrida. Temos horários flexíveis, inclusive aos sábados. Qual seria melhor para você?",
    selected: false
  },
  {
    id: "10",
    title: "Doação Corporativa",
    content: "Olá! Sua empresa tem interesse em participar de nossa campanha corporativa de doação? Podemos organizar tudo!",
    selected: false
  },
  {
    id: "11",
    title: "Seguimento Pós-Doação",
    content: "Oi! Como você se sentiu após a última doação? Espero que tenha sido uma experiência positiva. Já pode doar novamente!",
    selected: true
  },
  {
    id: "12",
    title: "Motivacional",
    content: "Olá, herói! Sim, você é um herói por salvar vidas através da doação. Que tal continuar essa missão? 🦸‍♂️",
    selected: false
  },
  {
    id: "13",
    title: "Datas Comemorativas",
    content: "Olá! Em comemoração ao Dia Mundial do Doador, que tal fazer uma doação especial? Será um presente para quem precisa!",
    selected: false
  },
  {
    id: "14",
    title: "Incentivo Familiar",
    content: "Oi! Que exemplo lindo você dá para sua família sendo doador! Já conversou com eles sobre doação?",
    selected: false
  },
  {
    id: "15",
    title: "Disponibilidade Estendida",
    content: "Olá! Estamos com atendimento estendido nesta semana. Horários especiais disponíveis! Qual prefere?",
    selected: true
  }
];

export const ConfigurationPage: React.FC<ConfigurationPageProps> = ({ onBack }) => {
  const [activeTab, setActiveTab] = useState<'agent' | 'campaign' | 'templates'>('agent');
  const [agentConfig, setAgentConfig] = useState<AgentConfig>({
    name: "Sofia",
    avatar: "",
    speechProfile: "amigavel",
    llmType: "medio"
  });
  const [campaignData, setCampaignData] = useState<CampaignData>({
    name: "",
    description: "",
    startDate: "",
    endDate: "",
    sourceSystem: ""
  });
  const [templates, setTemplates] = useState<ConversationTemplate[]>(defaultTemplates);
  const [isUploading, setIsUploading] = useState(false);
  const [duplicateUsers, setDuplicateUsers] = useState<string[]>([]);
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<ConversationTemplate | null>(null);

  const handleAgentConfigChange = (field: keyof AgentConfig, value: string) => {
    setAgentConfig(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleCampaignChange = (field: keyof CampaignData, value: string) => {
    setCampaignData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleDateRangeChange = (dates: [dayjs.Dayjs, dayjs.Dayjs] | null) => {
    if (dates && dates.length === 2) {
      setCampaignData(prev => ({
        ...prev,
        startDate: dates[0].format('YYYY-MM-DD'),
        endDate: dates[1].format('YYYY-MM-DD')
      }));
    }
  };

  const handleTemplateToggle = (templateId: string) => {
    setTemplates(prev =>
      prev.map(template =>
        template.id === templateId
          ? { ...template, selected: !template.selected }
          : template
      )
    );
  };

  const handleEditTemplate = (templateId: string) => {
    const template = templates.find(t => t.id === templateId);
    if (template) {
      setEditingTemplate(template);
      setShowTemplateModal(true);
    }
  };

  const handleEnhanceTemplate = (templateId: string) => {
    const template = templates.find(t => t.id === templateId);
    if (template) {
      setEditingTemplate({ ...template, content: template.content });
      setShowTemplateModal(true);
    }
  };

  const handleDeleteTemplate = (templateId: string) => {
    setTemplates(prev => prev.filter(t => t.id !== templateId));
    message.success('Template excluído com sucesso!');
  };

  const handleSaveTemplate = (templateData: { title: string; content: string; category: string }) => {
    if (editingTemplate) {
      // Editar template existente
      setTemplates(prev => 
        prev.map(t => 
          t.id === editingTemplate.id 
            ? { ...t, ...templateData, isCustom: true }
            : t
        )
      );
      message.success('Template atualizado com sucesso!');
    } else {
      // Criar novo template
      const newTemplate: ConversationTemplate = {
        id: Date.now().toString(),
        ...templateData,
        selected: false,
        isCustom: true
      };
      setTemplates(prev => [...prev, newTemplate]);
      message.success('Template criado com sucesso!');
    }
    
    setShowTemplateModal(false);
    setEditingTemplate(null);
  };

  const uploadProps: UploadProps = {
    name: 'file',
    accept: '.xlsx,.csv',
    beforeUpload: (file) => {
      const isValidType = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                          file.type === 'text/csv' ||
                          file.name.endsWith('.xlsx') ||
                          file.name.endsWith('.csv');
      
      if (!isValidType) {
        message.error('Você só pode enviar arquivos XLSX ou CSV!');
        return false;
      }

      setCampaignData(prev => ({ ...prev, file }));
      return false;
    },
    onRemove: () => {
      setCampaignData(prev => ({ ...prev, file: undefined }));
    }
  };

  const handleImportCampaign = async () => {
    if (!campaignData.name || !campaignData.file || !campaignData.startDate || !campaignData.endDate) {
      message.error('Preencha todos os campos obrigatórios!');
      return;
    }

    const selectedTemplates = templates.filter(t => t.selected);
    if (selectedTemplates.length === 0) {
      message.error('Selecione pelo menos um template de conversa!');
      return;
    }

    setIsUploading(true);
    
    // Simular detecção de usuários duplicados
    const mockDuplicates = ['João Silva', 'Maria Santos', 'Ana Costa'];
    
    try {
      // Simular processamento do arquivo CSV/XLSX
      message.loading('Processando arquivo...', 0.5);
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Simular aplicação dos templates
      message.loading('Aplicando templates de conversa...', 0.5);
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Simular criação de conversas com templates selecionados
      const contactsProcessed = Math.floor(Math.random() * 50) + 20; // 20-70 contatos
      const conversationsCreated = Math.floor(contactsProcessed * 0.8); // 80% receberam mensagens
      
      // Simular distribuição dos templates
      const templateDistribution = selectedTemplates.map(template => ({
        template: template.title,
        used: Math.floor(Math.random() * (conversationsCreated / selectedTemplates.length)) + 1
      }));
      
      setDuplicateUsers(mockDuplicates);
      
      // Mensagem de sucesso detalhada
      
      message.success({
        content: (
          <div>
            <div className="font-semibold mb-2">
              Campanha "{campaignData.name}" importada com sucesso! 🎉
            </div>
            <div className="text-sm space-y-1">
              <div>📊 {contactsProcessed} contatos processados</div>
              <div>💬 {conversationsCreated} conversas iniciadas</div>
              <div>📝 {selectedTemplates.length} templates aplicados:</div>
              <div className="pl-4 text-xs text-gray-600">
                {templateDistribution.map((item, index) => (
                  <div key={index}>• {item.template}: {item.used} mensagens</div>
                ))}
              </div>
              <div>⚠️ {mockDuplicates.length} duplicatas detectadas</div>
            </div>
          </div>
        ),
        duration: 8,
      });
      
      // Reset form
      setCampaignData({
        name: "",
        description: "",
        startDate: "",
        endDate: "",
        sourceSystem: "",
        file: undefined
      });
      
      // Log detalhado dos templates utilizados
      console.log('🎯 Campanha importada:', campaignData.name);
      console.log('📋 Templates aplicados:');
      templateDistribution.forEach((item) => {
        const template = selectedTemplates.find(t => t.title === item.template);
        console.log(`
${item.template} (${item.used}x):`);
        console.log(`"${template?.content}"`);
      });
      
      // Voltar para conversas após 3 segundos
      setTimeout(() => {
        onBack();
      }, 3000);
      
    } catch {
      message.error('Erro ao importar campanha!');
    } finally {
      setIsUploading(false);
    }
  };

  const avatarUploadProps: UploadProps = {
    name: 'avatar',
    accept: '.jpg,.jpeg,.png,.gif',
    beforeUpload: (file) => {
      const isImage = file.type.startsWith('image/');
      if (!isImage) {
        message.error('Você só pode enviar arquivos de imagem!');
        return false;
      }
      
      const reader = new FileReader();
      reader.onload = (e) => {
        if (e.target?.result) {
          handleAgentConfigChange('avatar', e.target.result as string);
        }
      };
      reader.readAsDataURL(file);
      
      return false;
    }
  };

  return (
    <div className="flex h-screen bg-white">
      <div className="flex-1 flex flex-col">
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <button
                onClick={onBack}
                className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <h1 className="text-xl font-semibold text-gray-800">Configurações</h1>
            </div>
          </div>

          <div className="flex gap-1 mt-4">
            <button
              onClick={() => setActiveTab('agent')}
              className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                activeTab === 'agent'
                  ? 'bg-blue-500 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              Agente IA
            </button>
            <button
              onClick={() => setActiveTab('campaign')}
              className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                activeTab === 'campaign'
                  ? 'bg-blue-500 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              Nova Campanha
            </button>
            <button
              onClick={() => setActiveTab('templates')}
              className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                activeTab === 'templates'
                  ? 'bg-blue-500 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              Templates
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === 'agent' && (
            <div className="max-w-2xl">
              <h2 className="text-lg font-semibold mb-4">Configuração do Agente IA</h2>
              
              <Card className="mb-6">
                <div className="flex items-center gap-4 mb-6">
                  <div className="relative">
                    <Avatar 
                      size={80} 
                      src={agentConfig.avatar} 
                      icon={<User />}
                      className="bg-blue-500"
                    />
                    <AntUpload {...avatarUploadProps} showUploadList={false}>
                      <Button 
                        size="small" 
                        className="absolute -bottom-2 -right-2 rounded-full"
                        icon={<Upload />}
                      />
                    </AntUpload>
                  </div>
                  <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Nome do Agente
                    </label>
                    <Input
                      value={agentConfig.name}
                      onChange={(e) => handleAgentConfigChange('name', e.target.value)}
                      placeholder="Ex: Sofia, Ana, João..."
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Perfil de Fala
                    </label>
                    <Radio.Group
                      value={agentConfig.speechProfile}
                      onChange={(e: RadioChangeEvent) => handleAgentConfigChange('speechProfile', e.target.value)}
                      className="w-full"
                    >
                      <Radio.Button value="formal" className="w-full mb-2">Formal</Radio.Button>
                      <Radio.Button value="amigavel" className="w-full mb-2">Amigável</Radio.Button>
                      <Radio.Button value="descontraido" className="w-full mb-2">Descontraído</Radio.Button>
                      <Radio.Button value="serio" className="w-full mb-2">Sério</Radio.Button>
                      <Radio.Button value="animado" className="w-full">Animado</Radio.Button>
                    </Radio.Group>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Tipo de LLM
                    </label>
                    <Radio.Group
                      value={agentConfig.llmType}
                      onChange={(e: RadioChangeEvent) => handleAgentConfigChange('llmType', e.target.value)}
                      className="w-full"
                    >
                      <Radio.Button value="barato" className="w-full mb-2">
                        Econômico - Respostas rápidas
                      </Radio.Button>
                      <Radio.Button value="medio" className="w-full mb-2">
                        Padrão - Equilibrio custo/qualidade
                      </Radio.Button>
                      <Radio.Button value="caro" className="w-full">
                        Premium - Máxima qualidade
                      </Radio.Button>
                    </Radio.Group>
                  </div>
                </div>
              </Card>

              <Button type="primary" size="large">
                Salvar Configurações do Agente
              </Button>
            </div>
          )}

          {activeTab === 'campaign' && (
            <div className="max-w-2xl">
              <h2 className="text-lg font-semibold mb-4">Nova Campanha</h2>
              
              <Card>
                <div className="space-y-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Nome da Campanha *
                    </label>
                    <Input
                      value={campaignData.name}
                      onChange={(e) => handleCampaignChange('name', e.target.value)}
                      placeholder="Ex: Campanha Junho 2025"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Descrição
                    </label>
                    <TextArea
                      value={campaignData.description}
                      onChange={(e) => handleCampaignChange('description', e.target.value)}
                      rows={3}
                      placeholder="Descreva o objetivo da campanha..."
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Período da Campanha *
                    </label>
                    <RangePicker
                      value={campaignData.startDate && campaignData.endDate ? 
                        [dayjs(campaignData.startDate), dayjs(campaignData.endDate)] : null}
                      onChange={handleDateRangeChange}
                      className="w-full"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Sistema de Origem
                    </label>
                    <Select
                      value={campaignData.sourceSystem}
                      onChange={(value) => handleCampaignChange('sourceSystem', value)}
                      className="w-full"
                      placeholder="Selecione o sistema"
                    >
                      <Option value="crm">CRM</Option>
                      <Option value="erp">ERP</Option>
                      <Option value="planilha">Planilha</Option>
                      <Option value="outro">Outro</Option>
                    </Select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Arquivo de Contatos *
                    </label>
                    <AntUpload.Dragger {...uploadProps}>
                      <p className="ant-upload-drag-icon">
                        <Upload className="w-12 h-12 text-gray-400 mx-auto" />
                      </p>
                      <p className="ant-upload-text">
                        Clique ou arraste arquivo para esta área
                      </p>
                      <p className="ant-upload-hint">
                        Suporta apenas arquivos .xlsx ou .csv
                      </p>
                    </AntUpload.Dragger>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Templates Selecionados *
                    </label>
                    <div className="border border-gray-200 rounded-lg p-4 bg-gray-50">
                      {templates.filter(t => t.selected).length === 0 ? (
                        <p className="text-sm text-gray-500 text-center">
                          Nenhum template selecionado. 
                          <button 
                            type="button"
                            onClick={() => setActiveTab('templates')}
                            className="text-blue-500 hover:text-blue-700 ml-1"
                          >
                            Clique aqui para selecionar
                          </button>
                        </p>
                      ) : (
                        <div className="space-y-2">
                          <p className="text-sm font-medium text-gray-700 mb-3">
                            {templates.filter(t => t.selected).length} template(s) será(ão) usado(s) na campanha:
                          </p>
                          {templates.filter(t => t.selected).map((template) => (
                            <div key={template.id} className="bg-white p-3 rounded border border-gray-200">
                              <h5 className="font-medium text-sm text-gray-800 mb-1">
                                {template.title}
                              </h5>
                              <p className="text-xs text-gray-600 line-clamp-2">
                                {template.content}
                              </p>
                            </div>
                          ))}
                          <button 
                            type="button"
                            onClick={() => setActiveTab('templates')}
                            className="text-sm text-blue-500 hover:text-blue-700"
                          >
                            Editar seleção de templates
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                  {duplicateUsers.length > 0 && (
                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                      <h4 className="text-sm font-medium text-yellow-800 mb-2">
                        ⚠️ Usuários que responderam negativamente em campanhas anteriores:
                      </h4>
                      <ul className="text-sm text-yellow-700">
                        {duplicateUsers.map((user, index) => (
                          <li key={index}>• {user}</li>
                        ))}
                      </ul>
                    </div>
                  )}

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
                      templates.filter(t => t.selected).length === 0
                    }
                    className="w-full"
                  >
                    {isUploading ? 'Importando e Aplicando Templates...' : 'Importar Campanha'}
                  </Button>
                </div>
              </Card>
            </div>
          )}

          {activeTab === 'templates' && (
            <div className="max-w-4xl">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold">Templates de Conversa</h2>
                <div className="flex items-center gap-4">
                  <div className="text-sm text-gray-600">
                    {templates.filter(t => t.selected).length} de {templates.length} selecionados
                  </div>
                  <Button 
                    type="primary" 
                    icon={<Plus />}
                    onClick={() => setShowTemplateModal(true)}
                  >
                    Novo Template
                  </Button>
                </div>
              </div>

              {templates.filter(t => t.selected).length > 0 && (
                <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <h3 className="font-medium text-blue-800 mb-2">
                    💡 Como os templates funcionam nas campanhas
                  </h3>
                  <ul className="text-sm text-blue-700 space-y-1">
                    <li>• Os templates selecionados serão distribuídos automaticamente entre os contatos importados</li>
                    <li>• Cada contato receberá uma mensagem inicial baseada em um dos templates escolhidos</li>
                    <li>• O sistema alternará entre os templates para diversificar as abordagens</li>
                    <li>• Contatos duplicados (que já responderam negativamente) não receberão mensagens</li>
                  </ul>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {templates.map((template) => (
                  <Card
                    key={template.id}
                    className={`cursor-pointer transition-all ${
                      template.selected 
                        ? 'border-blue-500 bg-blue-50' 
                        : 'hover:border-gray-300'
                    }`}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleTemplateToggle(template.id);
                    }}
                  >
                    <div className="flex items-start justify-between mb-3">
                      <h4 className="font-medium text-gray-800 flex-1">
                        {template.title}
                      </h4>
                      <div className="flex items-center gap-2">
                        <Dropdown
                          trigger={['click']}
                          menu={{
                            items: [
                              {
                                key: 'edit',
                                label: 'Editar',
                                icon: <Edit3 className="w-4 h-4" />,
                                onClick: () => handleEditTemplate(template.id)
                              },
                              {
                                key: 'enhance',
                                label: 'Melhorar com IA',
                                icon: <Sparkles className="w-4 h-4" />,
                                onClick: () => handleEnhanceTemplate(template.id)
                              },
                              {
                                key: 'delete',
                                label: 'Excluir',
                                icon: <Trash2 className="w-4 h-4" />,
                                danger: true,
                                onClick: () => handleDeleteTemplate(template.id)
                              }
                            ]
                          }}
                        >
                          <Button 
                            size="small" 
                            type="text" 
                            icon={<MoreVertical className="w-4 h-4" />}
                            onClick={(e) => e.stopPropagation()}
                          />
                        </Dropdown>
                        <div className={`w-5 h-5 rounded border-2 flex items-center justify-center ${
                          template.selected 
                            ? 'bg-blue-500 border-blue-500' 
                            : 'border-gray-300'
                        }`}>
                          {template.selected && <Check className="w-3 h-3 text-white" />}
                        </div>
                      </div>
                    </div>
                    <p className="text-sm text-gray-600 line-clamp-3">
                      {template.content}
                    </p>
                    <div className="mt-3 pt-3 border-t border-gray-200">
                      <div className="flex items-center justify-between text-xs text-gray-500">
                        <span>{template.category || 'Geral'}</span>
                        {template.isCustom && (
                          <span className="bg-green-100 text-green-800 px-2 py-1 rounded">
                            Personalizado
                          </span>
                        )}
                      </div>
                    </div>
                  </Card>
                ))}
              </div>

              <div className="mt-6 pt-6 border-t border-gray-200">
                <Button type="primary" size="large">
                  Salvar Templates Selecionados
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
      
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