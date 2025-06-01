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
    name: 'Lilia - AT Santa Casa',
    avatar: 'https://images.unsplash.com/photo-1494790108755-2616b172-9e1f?w=150&h=150&fit=crop&crop=face',
    isOnline: true,
    phone: '+55 11 99999-0001'
  },
  {
    id: '2',
    name: 'Kelly Rocha - BIOCOR',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face',
    isOnline: false,
    lastSeen: new Date(Date.now() - 1000 * 60 * 30),
    phone: '+55 11 99999-0002'
  },
  {
    id: '3',
    name: 'Evandro Thiesen - Clínica LeVitá',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face',
    isOnline: true,
    phone: '+55 11 99999-0003'
  },
  {
    id: '4',
    name: 'Maria Luiza - BIOCOR',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face',
    isOnline: false,
    lastSeen: new Date(Date.now() - 1000 * 60 * 60 * 2),
    phone: '+55 11 99999-0004'
  },
  {
    id: '5',
    name: 'Allan Bruno',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop&crop=face',
    isOnline: true,
    phone: '+55 11 99999-0005'
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
      createMessage('1', 'Olá! Gostaria de saber mais sobre os serviços da Santa Casa.', true, 120),
      createMessage('2', 'Olá Lilia! Claro, posso te ajudar. Que tipo de serviço você procura?', false, 118),
      createMessage('3', 'Agradeço a disponibilidade e atenção', true, 5)
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
      createMessage('4', 'Pode me ligar agora se quiser. Não consegui olhar o vídeo de ontem', true, 45),
      createMessage('5', 'Claro! Vou te ligar em instantes. Sobre qual vídeo você está falando?', false, 43)
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
      createMessage('6', 'Vamos agendar uma demonstração?', true, 180),
      createMessage('7', 'Perfeito! Qual o melhor horário para você?', false, 175),
      createMessage('8', 'Podemos dar continuidade à aquisição de um novo sistema', true, 25)
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
      createMessage('9', 'Pode sim', true, 90),
      createMessage('10', 'Ótimo! Te ligo em alguns minutos.', false, 88)
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
      createMessage('11', 'conseguiu olhar o vídeo de ontem?', true, 720),
      createMessage('12', 'Sim! Muito interessante, vamos conversar mais sobre isso.', false, 715)
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