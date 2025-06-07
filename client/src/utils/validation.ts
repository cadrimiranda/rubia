/**
 * Utilitários para validação e sanitização de dados
 */

export class ValidationError extends Error {
  field?: string
  
  constructor(message: string, field?: string) {
    super(message)
    this.name = 'ValidationError'
    this.field = field
  }
}

export class ValidationUtils {
  /**
   * Valida formato de telefone brasileiro
   */
  static validateBrazilianPhone(phone: string): boolean {
    const cleaned = phone.replace(/\D/g, '')
    
    // Deve ter entre 10 e 13 dígitos
    if (cleaned.length < 10 || cleaned.length > 13) return false
    
    // Se tem 13 dígitos, deve começar com 55
    if (cleaned.length === 13 && !cleaned.startsWith('55')) return false
    
    // Se tem 12 dígitos, deve começar com 55
    if (cleaned.length === 12 && !cleaned.startsWith('55')) return false
    
    // DDD válido (11-99)
    const dddStart = cleaned.length === 13 ? 2 : cleaned.length === 12 ? 2 : 0
    const ddd = parseInt(cleaned.substring(dddStart, dddStart + 2))
    if (ddd < 11 || ddd > 99) return false
    
    return true
  }

  /**
   * Normaliza telefone brasileiro
   */
  static normalizeBrazilianPhone(phone: string): string {
    const cleaned = phone.replace(/\D/g, '')
    
    // Se já tem código do país
    if (cleaned.length === 13 && cleaned.startsWith('55')) {
      return `+${cleaned}`
    }
    
    // Se tem 11 dígitos (DDD + número com 9)
    if (cleaned.length === 11) {
      return `+55${cleaned}`
    }
    
    // Se tem 10 dígitos (DDD + número sem 9)
    if (cleaned.length === 10) {
      const ddd = cleaned.substring(0, 2)
      const number = cleaned.substring(2)
      return `+55${ddd}9${number}`
    }
    
    return phone
  }

  /**
   * Valida email
   */
  static validateEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  }

  /**
   * Valida senha
   */
  static validatePassword(password: string): { valid: boolean; errors: string[] } {
    const errors: string[] = []
    
    if (password.length < 8) {
      errors.push('Senha deve ter pelo menos 8 caracteres')
    }
    
    if (!/[A-Z]/.test(password)) {
      errors.push('Senha deve ter pelo menos uma letra maiúscula')
    }
    
    if (!/[a-z]/.test(password)) {
      errors.push('Senha deve ter pelo menos uma letra minúscula')
    }
    
    if (!/\d/.test(password)) {
      errors.push('Senha deve ter pelo menos um número')
    }
    
    return {
      valid: errors.length === 0,
      errors
    }
  }

  /**
   * Valida nome
   */
  static validateName(name: string): boolean {
    return name.trim().length >= 2 && name.trim().length <= 100
  }

  /**
   * Valida tamanho de mensagem
   */
  static validateMessageLength(content: string): boolean {
    return content.trim().length > 0 && content.length <= 4000
  }

  /**
   * Valida cor hexadecimal
   */
  static validateHexColor(color: string): boolean {
    const hexRegex = /^#[0-9A-F]{6}$/i
    return hexRegex.test(color)
  }

  /**
   * Valida URL
   */
  static validateUrl(url: string): boolean {
    try {
      new URL(url)
      return true
    } catch {
      return false
    }
  }

  /**
   * Valida se string não está vazia
   */
  static validateRequired(value: string): boolean {
    return value.trim().length > 0
  }

  /**
   * Valida tamanho máximo
   */
  static validateMaxLength(value: string, maxLength: number): boolean {
    return value.length <= maxLength
  }

  /**
   * Valida tamanho mínimo
   */
  static validateMinLength(value: string, minLength: number): boolean {
    return value.trim().length >= minLength
  }
}

export class SanitizationUtils {
  /**
   * Sanitiza string removendo caracteres perigosos
   */
  static sanitizeString(input: string): string {
    return input
      .trim()
      .replace(/[<>]/g, '') // Remove < e >
      .replace(/javascript:/gi, '') // Remove javascript:
      .replace(/on\w+\s*=/gi, '') // Remove event handlers
  }

  /**
   * Sanitiza HTML removendo tags perigosas
   */
  static sanitizeHtml(input: string): string {
    return input
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+\s*=/gi, '')
  }

  /**
   * Sanitiza telefone mantendo apenas números
   */
  static sanitizePhone(phone: string): string {
    return phone.replace(/\D/g, '')
  }

  /**
   * Sanitiza email
   */
  static sanitizeEmail(email: string): string {
    return email.trim().toLowerCase()
  }

  /**
   * Sanitiza nome removendo múltiplos espaços
   */
  static sanitizeName(name: string): string {
    return name
      .trim()
      .replace(/\s+/g, ' ') // Remove múltiplos espaços
      .replace(/[<>]/g, '') // Remove < e >
  }

  /**
   * Sanitiza conteúdo de mensagem
   */
  static sanitizeMessageContent(content: string): string {
    return content
      .trim()
      .replace(/\s+/g, ' ') // Normaliza espaços
      .substring(0, 4000) // Limita tamanho
  }

  /**
   * Sanitiza URL
   */
  static sanitizeUrl(url: string): string {
    const trimmed = url.trim()
    
    // Remove javascript: e data: URLs por segurança
    if (trimmed.toLowerCase().startsWith('javascript:') || 
        trimmed.toLowerCase().startsWith('data:')) {
      return ''
    }
    
    return trimmed
  }

  /**
   * Sanitiza cor hex
   */
  static sanitizeHexColor(color: string): string {
    const cleaned = color.replace(/[^0-9A-Fa-f#]/g, '')
    
    if (cleaned.startsWith('#') && cleaned.length === 7) {
      return cleaned.toUpperCase()
    }
    
    if (cleaned.length === 6) {
      return `#${cleaned.toUpperCase()}`
    }
    
    return '#1890FF' // Cor padrão se inválida
  }

  /**
   * Remove caracteres especiais mas mantém acentos
   */
  static sanitizeText(text: string): string {
    return text
      .trim()
      .replace(/[<>]/g, '')
      .replace(/\s+/g, ' ')
  }

  /**
   * Sanitiza dados de formulário
   */
  static sanitizeFormData<T extends Record<string, string>>(data: T): T {
    const sanitized = { ...data }
    
    for (const key in sanitized) {
      const value = sanitized[key]
      
      if (typeof value === 'string') {
        sanitized[key] = this.sanitizeString(value) as T[Extract<keyof T, string>]
      }
    }
    
    return sanitized
  }
}

/**
 * Validador específico para mensagens
 */
export class MessageValidator {
  static validate(content: string): { valid: boolean; error?: string } {
    const sanitized = SanitizationUtils.sanitizeMessageContent(content)
    
    if (!ValidationUtils.validateRequired(sanitized)) {
      return { valid: false, error: 'Mensagem não pode estar vazia' }
    }
    
    if (!ValidationUtils.validateMessageLength(sanitized)) {
      return { valid: false, error: 'Mensagem muito longa (máximo 4000 caracteres)' }
    }
    
    return { valid: true }
  }
}

/**
 * Validador específico para clientes
 */
export class CustomerValidator {
  static validatePhone(phone: string): { valid: boolean; error?: string } {
    const sanitized = SanitizationUtils.sanitizePhone(phone)
    
    if (!ValidationUtils.validateRequired(sanitized)) {
      return { valid: false, error: 'Telefone é obrigatório' }
    }
    
    if (!ValidationUtils.validateBrazilianPhone(sanitized)) {
      return { valid: false, error: 'Formato de telefone inválido' }
    }
    
    return { valid: true }
  }

  static validateName(name?: string): { valid: boolean; error?: string } {
    if (!name) return { valid: true } // Nome é opcional
    
    const sanitized = SanitizationUtils.sanitizeName(name)
    
    if (!ValidationUtils.validateName(sanitized)) {
      return { valid: false, error: 'Nome deve ter entre 2 e 100 caracteres' }
    }
    
    return { valid: true }
  }
}

/**
 * Validador específico para usuários
 */
export class UserValidator {
  static validateEmail(email: string): { valid: boolean; error?: string } {
    const sanitized = SanitizationUtils.sanitizeEmail(email)
    
    if (!ValidationUtils.validateRequired(sanitized)) {
      return { valid: false, error: 'Email é obrigatório' }
    }
    
    if (!ValidationUtils.validateEmail(sanitized)) {
      return { valid: false, error: 'Formato de email inválido' }
    }
    
    return { valid: true }
  }

  static validatePassword(password: string): { valid: boolean; errors: string[] } {
    return ValidationUtils.validatePassword(password)
  }
}

/**
 * Validador específico para tags
 */
export class TagValidator {
  static validateName(name: string): { valid: boolean; error?: string } {
    const sanitized = SanitizationUtils.sanitizeName(name)
    
    if (!ValidationUtils.validateRequired(sanitized)) {
      return { valid: false, error: 'Nome da tag é obrigatório' }
    }
    
    if (!ValidationUtils.validateMaxLength(sanitized, 50)) {
      return { valid: false, error: 'Nome da tag deve ter no máximo 50 caracteres' }
    }
    
    return { valid: true }
  }

  static validateColor(color: string): { valid: boolean; error?: string } {
    const sanitized = SanitizationUtils.sanitizeHexColor(color)
    
    if (!ValidationUtils.validateHexColor(sanitized)) {
      return { valid: false, error: 'Cor deve estar no formato hexadecimal (#FFFFFF)' }
    }
    
    return { valid: true }
  }
}