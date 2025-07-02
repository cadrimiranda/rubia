import type { LoginRequest, CustomerDTO, CreateCustomerRequest, PageResponse } from '../api/types';
import type { AuthUser } from '../auth/authService';

// Mock de usuários para teste
export const mockUsers: Record<string, { password: string; user: AuthUser }> = {
  'admin@centrodesangue.com': {
    password: 'admin123',
    user: {
      id: 'user_admin_001',
      name: 'Sofia Administradora',
      email: 'admin@centrodesangue.com',
      role: 'ADMIN',
      department: {
        id: 'dept_001',
        name: 'Administração'
      },
      avatarUrl: '',
      isOnline: true,
      companyId: 'company_001',
      companySlug: 'centro-sangue-sp'
    }
  },
  'supervisor@centrodesangue.com': {
    password: 'super123',
    user: {
      id: 'user_super_001',
      name: 'João Supervisor',
      email: 'supervisor@centrodesangue.com',
      role: 'SUPERVISOR',
      department: {
        id: 'dept_002',
        name: 'Operações'
      },
      avatarUrl: '',
      isOnline: true,
      companyId: 'company_001',
      companySlug: 'centro-sangue-sp'
    }
  },
  'agente@centrodesangue.com': {
    password: 'agente123',
    user: {
      id: 'user_agent_001',
      name: 'Ana Agente',
      email: 'agente@centrodesangue.com',
      role: 'AGENT',
      department: {
        id: 'dept_003',
        name: 'Atendimento'
      },
      avatarUrl: '',
      isOnline: true,
      companyId: 'company_001',
      companySlug: 'centro-sangue-sp'
    }
  }
};

/**
 * Mock do login - simula autenticação local
 */
export const mockLogin = async (credentials: LoginRequest): Promise<AuthUser> => {
  // Simular delay da rede
  await new Promise(resolve => setTimeout(resolve, 800));

  const userMock = mockUsers[credentials.email];
  
  if (!userMock) {
    throw new Error('Usuário não encontrado');
  }

  if (userMock.password !== credentials.password) {
    throw new Error('Senha incorreta');
  }

  console.log('🎭 Mock Login realizado com sucesso:', userMock.user.name);
  
  return userMock.user;
};

/**
 * Mock do logout - simula limpeza no servidor
 */
export const mockLogout = async (): Promise<void> => {
  // Simular delay da rede
  await new Promise(resolve => setTimeout(resolve, 300));
  console.log('🎭 Mock Logout realizado');
};

/**
 * Mock para verificar se token é válido
 */
export const mockValidateToken = async (token: string): Promise<boolean> => {
  // Simular delay da rede
  await new Promise(resolve => setTimeout(resolve, 200));
  
  // Token mock sempre válido por simplicidade
  return token.startsWith('mock_');
};

/**
 * Gera um token mock simples para o usuário
 */
export const generateMockToken = (user: AuthUser): string => {
  return `mock_${user.id.slice(-8)}_${Date.now().toString(36)}`;
};

// Mock de customers/clientes para teste
let mockCustomers: CustomerDTO[] = [
  {
    id: 'customer_001',
    phone: '+5511999999999',
    name: 'João Silva',
    whatsappId: 'whatsapp_001',
    profileUrl: '',
    isBlocked: false,
    createdAt: '2025-01-15T10:00:00Z',
    updatedAt: '2025-01-15T10:00:00Z'
  },
  {
    id: 'customer_002', 
    phone: '+5511888888888',
    name: 'Maria Santos',
    whatsappId: 'whatsapp_002',
    profileUrl: '',
    isBlocked: false,
    createdAt: '2025-01-10T14:30:00Z',
    updatedAt: '2025-01-10T14:30:00Z'
  },
  {
    id: 'customer_003',
    phone: '+5511777777777',
    name: 'Pedro Oliveira',
    whatsappId: 'whatsapp_003', 
    profileUrl: '',
    isBlocked: false,
    createdAt: '2025-01-05T09:15:00Z',
    updatedAt: '2025-01-05T09:15:00Z'
  }
];

/**
 * Mock para buscar todos os customers
 */
export const mockGetAllCustomers = async (filters?: { size?: number; search?: string }): Promise<PageResponse<CustomerDTO>> => {
  // Simular delay da rede
  await new Promise(resolve => setTimeout(resolve, 500));

  let filteredCustomers = [...mockCustomers];
  
  // Aplicar filtro de busca se fornecido
  if (filters?.search) {
    const searchTerm = filters.search.toLowerCase();
    filteredCustomers = filteredCustomers.filter(customer => 
      customer.name?.toLowerCase().includes(searchTerm) ||
      customer.phone.includes(searchTerm)
    );
  }

  const size = filters?.size || 50;
  const totalElements = filteredCustomers.length;
  
  console.log('🎭 Mock getAll customers - retornando:', filteredCustomers.length, 'clientes');
  
  return {
    content: filteredCustomers.slice(0, size),
    totalElements,
    totalPages: Math.ceil(totalElements / size),
    size,
    number: 0,
    first: true,
    last: true
  };
};

/**
 * Mock para buscar customer por telefone
 */
export const mockFindCustomerByPhone = async (phone: string): Promise<CustomerDTO | null> => {
  // Simular delay da rede
  await new Promise(resolve => setTimeout(resolve, 300));

  const customer = mockCustomers.find(c => c.phone === phone);
  
  console.log('🎭 Mock findByPhone - telefone:', phone, 'encontrado:', !!customer);
  
  return customer || null;
};

/**
 * Mock para criar novo customer
 */
export const mockCreateCustomer = async (data: CreateCustomerRequest): Promise<CustomerDTO> => {
  // Simular delay da rede
  await new Promise(resolve => setTimeout(resolve, 800));

  // Verificar se já existe
  const existing = mockCustomers.find(c => c.phone === data.phone);
  if (existing) {
    throw new Error('Cliente com este telefone já existe');
  }

  const newCustomer: CustomerDTO = {
    id: `customer_${Date.now()}`,
    phone: data.phone,
    name: data.name,
    whatsappId: data.whatsappId,
    profileUrl: data.profileUrl,
    isBlocked: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  // Adicionar à lista mock
  mockCustomers.push(newCustomer);
  
  console.log('🎭 Mock createCustomer - criado:', newCustomer.name, newCustomer.phone);
  
  return newCustomer;
};