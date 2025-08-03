import type { Campaign, CampaignData, ConversationTemplate } from '../types/types'
import type { CustomerDTO, ConversationDTO, MessageDTO, CreateCustomerRequest } from '../api/types'
import { mockCampaigns } from './campaigns'

// Nomes brasileiros realistas para gerar contatos
const FIRST_NAMES = [
  'João', 'Maria', 'José', 'Ana', 'Pedro', 'Francisca', 'Antonio', 'Antonia', 
  'Carlos', 'Luiz', 'Paulo', 'Francisco', 'Daniel', 'Marcos', 'Bruno',
  'Rafael', 'Fernanda', 'Juliana', 'Mariana', 'Camila', 'Letícia', 'Gabriela',
  'Rodrigo', 'Ricardo', 'Felipe', 'Gustavo', 'Leonardo', 'Eduardo', 'Henrique',
  'Carolina', 'Amanda', 'Renata', 'Patricia', 'Sandra', 'Luciana', 'Adriana',
  'Roberto', 'Fernando', 'Marcelo', 'André', 'Thiago', 'Diego', 'Vitor',
  'Isabella', 'Sophia', 'Beatriz', 'Larissa', 'Natália', 'Bruna', 'Carla'
]

const LAST_NAMES = [
  'Silva', 'Santos', 'Oliveira', 'Souza', 'Rodrigues', 'Ferreira', 'Alves', 
  'Pereira', 'Lima', 'Gomes', 'Costa', 'Ribeiro', 'Martins', 'Carvalho',
  'Rocha', 'Almeida', 'Lopes', 'Soares', 'Fernandes', 'Vieira', 'Barbosa',
  'Araújo', 'Dias', 'Monteiro', 'Cardoso', 'Reis', 'Nascimento', 'Freitas',
  'Machado', 'Castro', 'Correia', 'Teixeira', 'Miranda', 'Ramos', 'Moreira',
  'Azevedo', 'Cunha', 'Pinto', 'Coelho', 'Mendes', 'Nunes', 'Moura'
]

// const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']

// const CITIES = [
//   'São Paulo', 'Rio de Janeiro', 'Belo Horizonte', 'Brasília', 'Curitiba',
//   'Recife', 'Porto Alegre', 'Salvador', 'Fortaleza', 'Goiânia', 'Manaus',
//   'Belém', 'Vitória', 'João Pessoa', 'Natal', 'Maceió', 'Aracaju', 'Teresina'
// ]

// Estados do contato durante a campanha
type ContactStatus = 'pendente' | 'contatado' | 'respondeu' | 'agendou' | 'doou' | 'recusou'

interface CampaignContact {
  id: string
  campaignId: string
  customer: CustomerDTO
  conversation?: ConversationDTO
  initialMessage?: MessageDTO
  template?: ConversationTemplate
  status: ContactStatus
  createdAt: string
  lastContact?: string
  notes?: string
}

// Storage dos contatos gerados por campanha
let campaignContacts: CampaignContact[] = []

/**
 * Gera um nome completo brasileiro realista
 */
const generateName = (): string => {
  const firstName = FIRST_NAMES[Math.floor(Math.random() * FIRST_NAMES.length)]
  const lastName = LAST_NAMES[Math.floor(Math.random() * LAST_NAMES.length)]
  return `${firstName} ${lastName}`
}

/**
 * Gera um telefone brasileiro válido
 */
const generatePhone = (): string => {
  const ddd = ['11', '21', '31', '41', '51', '61', '71', '81', '85', '86'][Math.floor(Math.random() * 10)]
  const prefix = '9' + Math.floor(Math.random() * 9000 + 1000)
  const suffix = Math.floor(Math.random() * 9000 + 1000)
  return `+55${ddd}${prefix}${suffix}`
}

/**
 * Gera um email baseado no nome
 */
// const generateEmail = (name: string): string => {
//   const domains = ['gmail.com', 'hotmail.com', 'yahoo.com.br', 'outlook.com', 'uol.com.br']
//   const cleanName = name.toLowerCase()
//     .replace(/\s+/g, '.')
//     .normalize('NFD')
//     .replace(/[\u0300-\u036f]/g, '')
//   const domain = domains[Math.floor(Math.random() * domains.length)]
//   return `${cleanName}@${domain}`
// }

/**
 * Gera dados de contato realistas
 */
const generateContactData = (): CreateCustomerRequest => {
  const name = generateName()
  const phone = generatePhone()
  
  return {
    name,
    phone,
    whatsappId: `whatsapp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    profileUrl: `https://via.placeholder.com/150/FF0000/FFFFFF?text=${name.split(' ').map(n => n[0]).join('')}`
  }
}

/**
 * Simula processamento de arquivo CSV/XLSX e geração de contatos
 */
export const processCampaignFile = async (
  campaignData: CampaignData,
  _file: File,
  selectedTemplates: ConversationTemplate[]
): Promise<{
  campaignId: string
  contactsProcessed: number
  conversationsCreated: number
  duplicatesFound: string[]
  templateDistribution: Array<{ template: string; used: number }>
}> => {
  
  // Simular delay de upload
  await new Promise(resolve => setTimeout(resolve, 1500))
  
  // Gerar ID da nova campanha
  const campaignId = `camp_${Date.now()}`
  
  // Fixar em exatamente 20 contatos para novas campanhas criadas
  const contactsProcessed = 20
  
  
  
  // Criar nova campanha
  const newCampaign: Campaign = {
    id: campaignId,
    name: campaignData.name,
    description: campaignData.description || '',
    startDate: campaignData.startDate,
    endDate: campaignData.endDate,
    status: 'active',
    color: ['#3b82f6', '#dc2626', '#059669', '#7c3aed', '#f59e0b'][Math.floor(Math.random() * 5)],
    templatesUsed: selectedTemplates.length,
    totalContacts: 0, // Will be updated after contacts are generated
    contactsReached: 0,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
  
  // Adicionar à lista de campanhas mock
  mockCampaigns.push(newCampaign)
  
  // Gerar contatos para a campanha
  const newContacts: CampaignContact[] = []
  const duplicatesFound: string[] = []
  
  for (let i = 0; i < contactsProcessed; i++) {
    const contactData = generateContactData()
    
    // Reduzir duplicatas para demonstração (apenas 1-2)
    if (Math.random() < 0.05 && duplicatesFound.length < 2) {
      duplicatesFound.push(contactData.name || 'Contato sem nome')
      continue
    }
    
    // Criar customer
    const customer: CustomerDTO = {
      id: `customer_camp_${campaignId}_${i}`,
      phone: contactData.phone,
      name: contactData.name,
      whatsappId: contactData.whatsappId,
      profileUrl: contactData.profileUrl,
      isBlocked: false,
      companyId: 'company_1',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    
    const contact: CampaignContact = {
      id: `contact_${campaignId}_${i}`,
      campaignId,
      customer,
      status: 'pendente',
      createdAt: new Date().toISOString()
    }
    
    newContacts.push(contact)
  }
  
  // Update campaign with actual contact count
  newCampaign.totalContacts = newContacts.length;
  
  // Simular delay de criação de conversas
  await new Promise(resolve => setTimeout(resolve, 1000))
  
  // Criar conversas para TODOS os contatos (100%)
  const conversationsCreated = newContacts.length
  const templateDistribution: Array<{ template: string; used: number }> = 
    selectedTemplates.map(t => ({ template: t.title, used: 0 }))
  
  
  for (let i = 0; i < conversationsCreated; i++) {
    const contact = newContacts[i]
    const template = selectedTemplates[i % selectedTemplates.length]
    
    // Criar conversa - TODAS no status ENTRADA (Ativos)
    const conversation: ConversationDTO = {
      id: `conv_${campaignId}_${i}`,
      customerId: contact.customer.id,
      companyId: 'mock-company',
      status: 'ENTRADA', // Sempre ENTRADA = Ativos
      channel: 'WHATSAPP',
      priority: 1,
      isPinned: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      messageCount: 1
    }
    
    // Criar mensagem inicial baseada no template
    const initialMessage: MessageDTO = {
      id: `msg_${campaignId}_${i}_initial`,
      conversationId: conversation.id,
      content: template.content,
      senderType: 'AI',
      messageType: 'TEXT',
      isAiGenerated: true,
      aiConfidence: 0.95,
      status: 'SENT',
      createdAt: new Date().toISOString()
    }
    
    // Atualizar contato
    contact.conversation = conversation
    contact.initialMessage = initialMessage
    contact.template = template
    contact.status = 'contatado'
    contact.lastContact = new Date().toISOString()
    
    // Atualizar distribuição de templates
    const templateIndex = templateDistribution.findIndex(td => td.template === template.title)
    if (templateIndex >= 0) {
      templateDistribution[templateIndex].used++
    }
    
    // Simular algumas respostas automáticas (10-20% dos contatos)
    if (Math.random() < 0.15) {
      await generateAutomaticResponse(contact)
    }
  }
  
  // Adicionar contatos ao storage
  campaignContacts.push(...newContacts)
  
  
  return {
    campaignId,
    contactsProcessed,
    conversationsCreated,
    duplicatesFound,
    templateDistribution
  }
}

/**
 * Gera resposta automática simulada para alguns contatos
 */
const generateAutomaticResponse = async (
  contact: CampaignContact
): Promise<void> => {
  // const responses = [
  //   'Oi! Obrigado por entrar em contato. Tenho interesse em saber mais sobre a doação.',
  //   'Olá! Gostaria de agendar uma doação, qual o melhor horário?',
  //   'Oi, tudo bem? Fiz doação mês passado, quando posso doar novamente?',
  //   'Olá! Tenho algumas dúvidas sobre o processo de doação.',
  //   'Oi! Posso levar um amigo para doar junto comigo?',
  //   'Olá! Qual o endereço do centro de doação?'
  // ]
  
  // const response = responses[Math.floor(Math.random() * responses.length)]
  
  // Simular delay de resposta (30 segundos a 5 minutos)
  const responseDelay = Math.floor(Math.random() * 270 + 30) * 1000
  
  setTimeout(() => {
    // const responseMessage: MessageDTO = {
    //   id: `msg_${contact.campaignId}_${contact.id}_response`,
    //   conversationId: conversation.id,
    //   content: response,
    //   senderType: 'CUSTOMER',
    //   messageType: 'TEXT',
    //   isAiGenerated: false,
    //   status: 'SENT',
    //   createdAt: new Date().toISOString()
    // }
    
    // Atualizar status do contato
    contact.status = 'respondeu'
    contact.lastContact = new Date().toISOString()
    contact.notes = 'Cliente respondeu automaticamente'
    
    // Atualizar conversa
    if (contact.conversation) {
      contact.conversation.status = 'ESPERANDO'
      contact.conversation.messageCount = (contact.conversation.messageCount || 1) + 1
      contact.conversation.updatedAt = new Date().toISOString()
    }
    
  }, responseDelay)
}

/**
 * Busca contatos por campanha
 */
export const getContactsByCampaign = (campaignId: string): CampaignContact[] => {
  return campaignContacts.filter(contact => contact.campaignId === campaignId)
}

/**
 * Busca todas as conversas de campanhas ativas
 */
export const getAllCampaignConversations = (): ConversationDTO[] => {
  return campaignContacts
    .filter(contact => contact.conversation)
    .map(contact => contact.conversation!)
}

/**
 * Busca mensagens de uma conversa específica de campanha
 */
export const getCampaignConversationMessages = (conversationId: string): MessageDTO[] => {
  const contact = campaignContacts.find(c => c.conversation?.id === conversationId)
  if (!contact || !contact.initialMessage) return []
  
  const messages: MessageDTO[] = [contact.initialMessage]
  
  // Adicionar possíveis respostas se existirem
  if (contact.status === 'respondeu' || contact.status === 'agendou') {
    const responses = [
      'Oi! Obrigado por entrar em contato. Tenho interesse em saber mais sobre a doação.',
      'Olá! Gostaria de agendar uma doação, qual o melhor horário?',
      'Oi, tudo bem? Fiz doação mês passado, quando posso doar novamente?',
      'Olá! Tenho algumas dúvidas sobre o processo de doação.',
      'Oi! Posso levar um amigo para doar junto comigo?',
      'Olá! Qual o endereço do centro de doação?'
    ]
    
    const responseMessage: MessageDTO = {
      id: `msg_${conversationId}_response`,
      conversationId,
      content: responses[Math.floor(Math.random() * responses.length)],
      senderType: 'CUSTOMER',
      messageType: 'TEXT',
      isAiGenerated: false,
      status: 'SENT',
      createdAt: new Date(Date.now() - Math.random() * 300000).toISOString() // Até 5 min atrás
    }
    
    messages.push(responseMessage)
  }
  
  return messages.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
}

/**
 * Simula criação de campanha completa (para usar na configuração)
 */
export const createMockCampaign = async (
  campaignData: CampaignData,
  selectedTemplates: ConversationTemplate[]
): Promise<{
  success: boolean
  campaign: Campaign
  stats: {
    contactsProcessed: number
    conversationsCreated: number
    duplicatesFound: string[]
    templateDistribution: Array<{ template: string; used: number }>
  }
}> => {
  
  // Usar o arquivo real fornecido pelo usuário
  const file = campaignData.file!
  
  const result = await processCampaignFile(campaignData, file, selectedTemplates)
  
  const campaign = mockCampaigns.find(c => c.id === result.campaignId)!
  
  return {
    success: true,
    campaign,
    stats: {
      contactsProcessed: result.contactsProcessed,
      conversationsCreated: result.conversationsCreated,
      duplicatesFound: result.duplicatesFound,
      templateDistribution: result.templateDistribution
    }
  }
}

/**
 * Estatísticas da campanha em tempo real
 */
export const getCampaignStats = (campaignId: string) => {
  const contacts = getContactsByCampaign(campaignId)
  
  const stats = {
    total: contacts.length,
    pendente: contacts.filter(c => c.status === 'pendente').length,
    contatado: contacts.filter(c => c.status === 'contatado').length,
    respondeu: contacts.filter(c => c.status === 'respondeu').length,
    agendou: contacts.filter(c => c.status === 'agendou').length,
    doou: contacts.filter(c => c.status === 'doou').length,
    recusou: contacts.filter(c => c.status === 'recusou').length,
  }
  
  return {
    ...stats,
    conversionRate: stats.total > 0 ? (stats.doou / stats.total * 100).toFixed(1) : '0',
    responseRate: stats.total > 0 ? ((stats.respondeu + stats.agendou + stats.doou) / stats.total * 100).toFixed(1) : '0'
  }
}

/**
 * Sincroniza conversas de campanhas com o chat store
 */
export const syncCampaignConversationsWithStore = (refreshConversations?: () => Promise<void>): void => {
  
  // Só sincroniza se houver uma função de refresh disponível
  if (refreshConversations) {
    // Aguardar um pouco mais para garantir que as conversas foram criadas
    setTimeout(() => {
      refreshConversations()
    }, 2000)
  }
}

/**
 * Simula mudança de status de contatos para demonstrar fluxo
 */
export const simulateContactStatusChanges = (): void => {
  
  // A cada 30 segundos, simular algumas mudanças de status
  setInterval(() => {
    const contacts = campaignContacts.filter(c => c.status === 'contatado')
    if (contacts.length === 0) return
    
    // Selecionar alguns contatos aleatoriamente
    const contactsToUpdate = contacts
      .sort(() => Math.random() - 0.5)
      .slice(0, Math.floor(Math.random() * 3) + 1)
    
    contactsToUpdate.forEach(contact => {
      const newStatus = Math.random() < 0.7 ? 'respondeu' : 'agendou'
      contact.status = newStatus as ContactStatus
      contact.lastContact = new Date().toISOString()
      
      if (contact.conversation) {
        contact.conversation.status = newStatus === 'respondeu' ? 'ESPERANDO' : 'FINALIZADOS'
        contact.conversation.updatedAt = new Date().toISOString()
      }
      
    })
  }, 30000) // 30 segundos
}

/**
 * Limpa dados de campanhas mock (para testes)
 */
export const clearMockCampaigns = (): void => {
  campaignContacts = []
  mockCampaigns.splice(5) // Manter apenas as 5 campanhas originais
}