import type { LoginRequest } from '../api/types';
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
  return token.startsWith('mock_token_');
};

/**
 * Gera um token mock para o usuário
 */
export const generateMockToken = (user: AuthUser): string => {
  return `mock_token_${user.id}_${Date.now()}`;
};