import type { Tag, ContactType } from '../types'

// Tipos para quando as tags estiverem implementadas no backend
interface TagDTO {
  id: string
  name: string
  color: string
  tagType: 'COMERCIAL' | 'SUPORTE' | 'VENDAS' | 'CUSTOM'
  createdAt: string
}

class TagAdapter {
  /**
   * Converte TagDTO do backend para Tag do frontend
   */
  toTag(dto: TagDTO): Tag {
    return {
      id: dto.id,
      name: dto.name,
      color: dto.color,
      type: this.mapTagType(dto.tagType)
    }
  }

  /**
   * Mapeia tipo de tag do backend para frontend
   */
  private mapTagType(backendType: string): ContactType {
    const typeMap: Record<string, ContactType> = {
      'COMERCIAL': 'comercial',
      'SUPORTE': 'suporte',
      'VENDAS': 'vendas',
      'CUSTOM': 'comercial' // Default para custom
    }
    
    return typeMap[backendType] || 'comercial'
  }

  /**
   * Mapeia tipo de tag do frontend para backend
   */
  mapTagTypeToBackend(frontendType: ContactType): string {
    const typeMap: Record<ContactType, string> = {
      'comercial': 'COMERCIAL',
      'suporte': 'SUPORTE',
      'vendas': 'VENDAS'
    }
    
    return typeMap[frontendType] || 'COMERCIAL'
  }

  /**
   * Converte array de TagDTO para array de Tag
   */
  toTagArray(dtos: TagDTO[]): Tag[] {
    return dtos.map(dto => this.toTag(dto))
  }

  /**
   * Cria um request DTO para criar nova tag
   */
  toCreateRequest(name: string, color: string, type: ContactType): {
    name: string
    color: string
    tagType: string
  } {
    return {
      name: name.trim(),
      color: this.validateColor(color),
      tagType: this.mapTagTypeToBackend(type)
    }
  }

  /**
   * Cria um request DTO para atualizar tag
   */
  toUpdateRequest(data: {
    name?: string
    color?: string
    type?: ContactType
  }): {
    name?: string
    color?: string
    tagType?: string
  } {
    return {
      name: data.name?.trim(),
      color: data.color ? this.validateColor(data.color) : undefined,
      tagType: data.type ? this.mapTagTypeToBackend(data.type) : undefined
    }
  }

  /**
   * Valida e normaliza cor hexadecimal
   */
  private validateColor(color: string): string {
    // Remove # se presente
    const cleaned = color.replace('#', '')
    
    // Verifica se é um hex válido
    if (!/^[0-9A-F]{6}$/i.test(cleaned)) {
      // Retorna cor padrão se inválida
      return '#1890FF'
    }
    
    return `#${cleaned.toUpperCase()}`
  }

  /**
   * Cria tags padrão do sistema
   */
  createDefaultTags(): Tag[] {
    return [
      {
        id: 'default-comercial',
        name: 'Comercial',
        color: '#10B981',
        type: 'comercial'
      },
      {
        id: 'default-suporte',
        name: 'Suporte',
        color: '#3B82F6',
        type: 'suporte'
      },
      {
        id: 'default-vendas',
        name: 'Vendas',
        color: '#8B5CF6',
        type: 'vendas'
      }
    ]
  }

  /**
   * Busca tags por tipo
   */
  filterByType(tags: Tag[], type: ContactType): Tag[] {
    return tags.filter(tag => tag.type === type)
  }

  /**
   * Busca tags por nome
   */
  searchByName(tags: Tag[], query: string): Tag[] {
    const searchTerm = query.toLowerCase().trim()
    if (!searchTerm) return tags
    
    return tags.filter(tag => 
      tag.name.toLowerCase().includes(searchTerm)
    )
  }

  /**
   * Ordena tags por nome
   */
  sortByName(tags: Tag[]): Tag[] {
    return [...tags].sort((a, b) => 
      a.name.localeCompare(b.name, 'pt-BR')
    )
  }

  /**
   * Ordena tags por tipo e depois por nome
   */
  sortByTypeAndName(tags: Tag[]): Tag[] {
    const typeOrder: Record<ContactType, number> = {
      'comercial': 1,
      'suporte': 2,
      'vendas': 3
    }
    
    return [...tags].sort((a, b) => {
      const typeCompare = typeOrder[a.type] - typeOrder[b.type]
      if (typeCompare !== 0) return typeCompare
      return a.name.localeCompare(b.name, 'pt-BR')
    })
  }

  /**
   * Verifica se uma tag já existe (por nome)
   */
  exists(tags: Tag[], name: string): boolean {
    const normalizedName = name.trim().toLowerCase()
    return tags.some(tag => 
      tag.name.toLowerCase() === normalizedName
    )
  }

  /**
   * Cria uma nova tag com dados validados
   */
  createTag(name: string, color: string, type: ContactType): Tag {
    return {
      id: `temp-${Date.now()}-${Math.random()}`,
      name: name.trim(),
      color: this.validateColor(color),
      type
    }
  }

  /**
   * Atualiza uma tag existente
   */
  updateTag(existingTag: Tag, updates: Partial<Pick<Tag, 'name' | 'color' | 'type'>>): Tag {
    return {
      ...existingTag,
      name: updates.name?.trim() || existingTag.name,
      color: updates.color ? this.validateColor(updates.color) : existingTag.color,
      type: updates.type || existingTag.type
    }
  }

  /**
   * Gera cores aleatórias para novas tags
   */
  generateRandomColor(): string {
    const colors = [
      '#F56565', '#ED8936', '#ECC94B', '#48BB78', '#38B2AC',
      '#4299E1', '#667EEA', '#9F7AEA', '#ED64A6', '#F687B3'
    ]
    return colors[Math.floor(Math.random() * colors.length)]
  }

  /**
   * Converte tags para formato de opções para select
   */
  toSelectOptions(tags: Tag[]): Array<{ label: string; value: string; color: string }> {
    return tags.map(tag => ({
      label: tag.name,
      value: tag.id,
      color: tag.color
    }))
  }

  /**
   * Agrupa tags por tipo
   */
  groupByType(tags: Tag[]): Record<ContactType, Tag[]> {
    return tags.reduce((groups, tag) => {
      if (!groups[tag.type]) {
        groups[tag.type] = []
      }
      groups[tag.type].push(tag)
      return groups
    }, {} as Record<ContactType, Tag[]>)
  }

  /**
   * Conta tags por tipo
   */
  countByType(tags: Tag[]): Record<ContactType, number> {
    return tags.reduce((counts, tag) => {
      counts[tag.type] = (counts[tag.type] || 0) + 1
      return counts
    }, {} as Record<ContactType, number>)
  }

  /**
   * Verifica se uma tag é padrão do sistema
   */
  isDefaultTag(tag: Tag): boolean {
    return tag.id.startsWith('default-')
  }

  /**
   * Remove tags duplicadas por nome
   */
  removeDuplicates(tags: Tag[]): Tag[] {
    const seen = new Set<string>()
    return tags.filter(tag => {
      const key = tag.name.toLowerCase()
      if (seen.has(key)) {
        return false
      }
      seen.add(key)
      return true
    })
  }
}

export const tagAdapter = new TagAdapter()
export default tagAdapter