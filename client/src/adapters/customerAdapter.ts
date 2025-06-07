import type { CustomerDTO } from '../api/types'
import type { User } from '../types'

class CustomerAdapter {
  /**
   * Converte CustomerDTO do backend para User do frontend (representa contato)
   */
  toUser(dto: CustomerDTO): User {
    return {
      id: dto.id,
      name: dto.name || this.formatPhoneAsName(dto.phone),
      avatar: dto.profileUrl || this.generateAvatarUrl(dto.name || dto.phone),
      isOnline: false, // Sempre false para customers (eles não têm status online)
      lastSeen: dto.updatedAt ? new Date(dto.updatedAt) : undefined,
      phone: dto.phone
    }
  }

  /**
   * Formata telefone como nome quando não há nome
   */
  private formatPhoneAsName(phone: string): string {
    // Remove caracteres especiais e formata como nome
    const cleaned = phone.replace(/\D/g, '')
    if (cleaned.length === 11 && cleaned.startsWith('55')) {
      // Formato brasileiro: +55 (11) 99999-9999
      const ddd = cleaned.substring(2, 4)
      const number = cleaned.substring(4)
      const firstPart = number.substring(0, number.length - 4)
      const lastPart = number.substring(number.length - 4)
      return `(${ddd}) ${firstPart}-${lastPart}`
    }
    return phone
  }

  /**
   * Gera URL de avatar padrão baseado no nome/telefone
   */
  private generateAvatarUrl(nameOrPhone: string): string {
    // Usa serviço de avatar baseado nas iniciais
    const initials = this.getInitials(nameOrPhone)
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(initials)}&background=random&size=150`
  }

  /**
   * Extrai iniciais do nome ou usa primeiros dígitos do telefone
   */
  private getInitials(nameOrPhone: string): string {
    if (nameOrPhone.includes('(') || nameOrPhone.includes('+')) {
      // É um telefone formatado, usa primeiros dígitos
      const digits = nameOrPhone.replace(/\D/g, '')
      return digits.substring(0, 2)
    }
    
    // É um nome, extrai iniciais
    const words = nameOrPhone.trim().split(' ')
    if (words.length >= 2) {
      return words[0][0] + words[1][0]
    }
    return words[0].substring(0, 2)
  }

  /**
   * Converte array de CustomerDTO para array de User
   */
  toUserArray(dtos: CustomerDTO[]): User[] {
    return dtos.map(dto => this.toUser(dto))
  }

  /**
   * Cria um request DTO para criar novo customer
   */
  toCreateRequest(phone: string, name?: string, whatsappId?: string): {
    phone: string
    name?: string
    whatsappId?: string
  } {
    return {
      phone: this.normalizePhone(phone),
      name,
      whatsappId
    }
  }

  /**
   * Cria um request DTO para atualizar customer
   */
  toUpdateRequest(data: {
    name?: string
    profileUrl?: string
  }): {
    name?: string
    profileUrl?: string
  } {
    return {
      name: data.name?.trim(),
      profileUrl: data.profileUrl
    }
  }

  /**
   * Normaliza número de telefone para formato padrão
   */
  normalizePhone(phone: string): string {
    // Remove todos os caracteres não numéricos
    const cleaned = phone.replace(/\D/g, '')
    
    // Se começar com 55 e tiver 13 dígitos, assume formato brasileiro completo
    if (cleaned.length === 13 && cleaned.startsWith('55')) {
      return `+${cleaned}`
    }
    
    // Se tiver 11 dígitos, assume formato brasileiro sem código país
    if (cleaned.length === 11) {
      return `+55${cleaned}`
    }
    
    // Se tiver 10 dígitos, assume formato brasileiro sem código país e sem 9
    if (cleaned.length === 10) {
      const ddd = cleaned.substring(0, 2)
      const number = cleaned.substring(2)
      return `+55${ddd}9${number}`
    }
    
    // Retorna como estava se não conseguir normalizar
    return phone
  }

  /**
   * Valida formato de telefone brasileiro
   */
  validateBrazilianPhone(phone: string): boolean {
    const normalized = this.normalizePhone(phone)
    const cleaned = normalized.replace(/\D/g, '')
    
    // Deve ter 13 dígitos (55 + DDD + 9 dígitos)
    if (cleaned.length !== 13) return false
    
    // Deve começar com 55
    if (!cleaned.startsWith('55')) return false
    
    // DDD deve estar entre 11 e 99
    const ddd = parseInt(cleaned.substring(2, 4))
    if (ddd < 11 || ddd > 99) return false
    
    // Número deve começar com 9 (celular)
    if (cleaned[4] !== '9') return false
    
    return true
  }

  /**
   * Atualiza um User existente com dados do DTO
   */
  updateUser(existingUser: User, dto: CustomerDTO): User {
    return {
      ...existingUser,
      name: dto.name || this.formatPhoneAsName(dto.phone),
      avatar: dto.profileUrl || existingUser.avatar,
      phone: dto.phone,
      lastSeen: dto.updatedAt ? new Date(dto.updatedAt) : existingUser.lastSeen
    }
  }

  /**
   * Verifica se um customer está bloqueado
   */
  isBlocked(dto: CustomerDTO): boolean {
    return dto.isBlocked
  }

  /**
   * Busca customer por telefone normalizado
   */
  findByPhone(customers: User[], phone: string): User | undefined {
    const normalizedPhone = this.normalizePhone(phone)
    return customers.find(customer => 
      this.normalizePhone(customer.phone || '') === normalizedPhone
    )
  }

  /**
   * Filtra customers ativos (não bloqueados)
   */
  filterActive(customers: User[]): User[] {
    // Como o User do frontend não tem isBlocked, assumimos que todos são ativos
    // Esta informação viria do DTO original se necessário
    return customers
  }

  /**
   * Ordena customers por nome ou telefone
   */
  sortByName(customers: User[]): User[] {
    return [...customers].sort((a, b) => {
      const nameA = a.name.toLowerCase()
      const nameB = b.name.toLowerCase()
      return nameA.localeCompare(nameB, 'pt-BR')
    })
  }

  /**
   * Busca customers por termo (nome ou telefone)
   */
  search(customers: User[], query: string): User[] {
    const searchTerm = query.toLowerCase().trim()
    if (!searchTerm) return customers
    
    return customers.filter(customer => 
      customer.name.toLowerCase().includes(searchTerm) ||
      (customer.phone && customer.phone.includes(searchTerm))
    )
  }

  /**
   * Verifica se um customer precisa ser atualizado
   */
  needsUpdate(user: User, dto: CustomerDTO): boolean {
    const dtoUser = this.toUser(dto)
    return (
      user.name !== dtoUser.name ||
      user.avatar !== dtoUser.avatar ||
      user.phone !== dtoUser.phone
    )
  }

  /**
   * Cria um customer fictício para casos onde não há dados
   */
  createUnknownCustomer(customerId: string): User {
    return {
      id: customerId,
      name: 'Cliente Desconhecido',
      avatar: this.generateAvatarUrl('Cliente Desconhecido'),
      isOnline: false,
      phone: ''
    }
  }
}

export const customerAdapter = new CustomerAdapter()
export default customerAdapter