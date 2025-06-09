import type { Chat, Message, Tag, User } from '../types'

export const mockTags: Tag[] = [
  { id: '1', name: 'Fabiana', color: '#10B981', type: 'comercial' },
  { id: '2', name: 'João', color: '#3B82F6', type: 'suporte' },
  { id: '3', name: 'Maria', color: '#8B5CF6', type: 'vendas' },
  { id: '4', name: 'Lucas', color: '#F59E0B', type: 'comercial' }
]

export const mockUsers: User[] = [
  {
    id: '1',
    name: 'Maria Silva',
    avatar: 'https://images.unsplash.com/photo-1494790108755-2616b172-9e1f?w=150&h=150&fit=crop&crop=face',
    isOnline: true,
    phone: '+55 11 99999-0001',
    email: 'maria.silva@email.com',
    bloodType: 'O+',
    lastDonation: '15/01/2025',
    totalDonations: 8,
    birthDate: '15/08/1985',
    weight: 65,
    height: 165,
    address: 'Rua das Flores, 123 - São Paulo, SP'
  },
  {
    id: '2',
    name: 'João Santos',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face',
    isOnline: false,
    lastSeen: new Date(Date.now() - 1000 * 60 * 30),
    phone: '+55 11 99999-0002',
    email: 'joao.santos@email.com',
    bloodType: 'A-',
    lastDonation: '08/02/2025',
    totalDonations: 15,
    birthDate: '22/03/1990',
    weight: 75,
    height: 178,
    address: 'Av. Paulista, 456 - São Paulo, SP'
  },
  {
    id: '3',
    name: 'Ana Costa',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face',
    isOnline: true,
    phone: '+55 11 99999-0003',
    email: 'ana.costa@email.com',
    bloodType: 'B+',
    lastDonation: '15/01/2025',
    totalDonations: 3,
    birthDate: '10/12/1992',
    weight: 58,
    height: 160,
    address: 'Rua Augusta, 789 - São Paulo, SP'
  },
  {
    id: '4',
    name: 'Carlos Oliveira',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face',
    isOnline: false,
    lastSeen: new Date(Date.now() - 1000 * 60 * 60 * 2),
    phone: '+55 11 99999-0004',
    email: 'carlos.oliveira@email.com',
    bloodType: 'AB+',
    lastDonation: '20/12/2024',
    totalDonations: 12,
    birthDate: '05/07/1988',
    weight: 80,
    height: 182,
    address: 'Rua da Consolação, 321 - São Paulo, SP'
  },
  {
    id: '5',
    name: 'Patricia Lima',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop&crop=face',
    isOnline: true,
    phone: '+55 11 99999-0005',
    email: 'patricia.lima@email.com',
    bloodType: 'O-',
    lastDonation: '10/11/2024',
    totalDonations: 6,
    birthDate: '18/01/1987',
    weight: 62,
    height: 170,
    address: 'Alameda Santos, 654 - São Paulo, SP'
  }
]

const createMessage = (id: string, content: string, isFromUser: boolean, minutesAgo: number): Message => ({
  id,
  content,
  timestamp: new Date(Date.now() - minutesAgo * 60 * 1000),
  senderId: isFromUser ? 'user' : 'agent',
  messageType: 'text',
  status: 'read',
  isFromUser
})

export const mockChats: Chat[] = [
  {
    id: '1',
    contact: mockUsers[0],
    messages: [
      createMessage('1', 'Olá! Gostaria de agendar uma doação de sangue.', true, 120),
      createMessage('2', 'Olá Maria! Claro, posso te ajudar. Temos horários disponíveis esta semana. Qual seria o melhor dia para você?', false, 118),
      createMessage('3', 'Obrigada pela lembrança! Posso doar na próxima semana.', true, 5)
    ],
    status: 'entrada',
    assignedAgent: 'Fabiana',
    tags: [mockTags[0]],
    unreadCount: 1,
    isPinned: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 120),
    updatedAt: new Date(Date.now() - 1000 * 60 * 5)
  },
  {
    id: '2',
    contact: mockUsers[1],
    messages: [
      createMessage('4', 'Preciso reagendar minha doação.', true, 45),
      createMessage('5', 'Claro! Qual seria o melhor horário para você? Temos disponibilidade na próxima semana.', false, 43)
    ],
    status: 'entrada',
    assignedAgent: 'Fabiana',
    tags: [mockTags[0]],
    unreadCount: 0,
    isPinned: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 60),
    updatedAt: new Date(Date.now() - 1000 * 60 * 43)
  },
  {
    id: '3',
    contact: mockUsers[2],
    messages: [
      createMessage('6', 'Qual o horário disponível para amanhã?', true, 180),
      createMessage('7', 'Temos horários às 9h, 14h e 16h. Qual prefere?', false, 175),
      createMessage('8', 'Prefiro às 14h, muito obrigada!', true, 25)
    ],
    status: 'esperando',
    assignedAgent: 'Fabiana',
    tags: [mockTags[0]],
    unreadCount: 0,
    isPinned: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 180),
    updatedAt: new Date(Date.now() - 1000 * 60 * 25)
  },
  {
    id: '4',
    contact: mockUsers[3],
    messages: [
      createMessage('9', 'Posso trazer meu exame de sangue?', true, 90),
      createMessage('10', 'Claro! Isso vai agilizar o processo. Nos vemos amanhã às 14h.', false, 88)
    ],
    status: 'esperando',
    assignedAgent: 'Fabiana',
    tags: [mockTags[0]],
    unreadCount: 0,
    isPinned: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 100),
    updatedAt: new Date(Date.now() - 1000 * 60 * 88)
  },
  {
    id: '5',
    contact: mockUsers[4],
    messages: [
      createMessage('11', 'Muito obrigada pelo atendimento!', true, 720),
      createMessage('12', 'Foi um prazer ajudar! Qualquer dúvida, estamos à disposição.', false, 715)
    ],
    status: 'finalizados',
    assignedAgent: 'Fabiana',
    tags: [mockTags[0]],
    unreadCount: 0,
    isPinned: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 730),
    updatedAt: new Date(Date.now() - 1000 * 60 * 715)
  }
]

mockChats.forEach(chat => {
  if (chat.messages.length > 0) {
    chat.lastMessage = chat.messages[chat.messages.length - 1]
  }
})

export const initializeMockData = () => {
  return mockChats
}