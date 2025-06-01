import type { Tag } from '../../types'

interface ChatTagsProps {
  tags: Tag[]
  showAgent?: boolean
  compact?: boolean
}

const ChatTags = ({ tags, showAgent = true, compact = false }: ChatTagsProps) => {
  if (tags.length === 0) return null

  const agentTag = tags.find(tag => tag.type === 'comercial' || tag.type === 'suporte' || tag.type === 'vendas')

  const getTagColor = (type: string) => {
    switch (type) {
      case 'comercial':
        return 'bg-blue-100 text-blue-700'
      case 'suporte':
        return 'bg-green-100 text-green-700'
      case 'vendas':
        return 'bg-purple-100 text-purple-700'
      default:
        return 'bg-neutral-200 text-neutral-800'
    }
  }

  return (
    <div className="flex items-center gap-1">
      {showAgent && agentTag && (
        <span
          className={`inline-flex items-center px-2 py-1 rounded-lg text-xs font-medium ${getTagColor(agentTag.type)}`}
        >
          {agentTag.name}
        </span>
      )}
      
      {/* Show additional tags if not compact */}
      {!compact && tags.filter(tag => tag !== agentTag).slice(0, 2).map(tag => (
        <span
          key={tag.id}
          className={`inline-flex items-center px-2 py-1 rounded-lg text-xs font-medium ${getTagColor(tag.type)}`}
        >
          {tag.name}
        </span>
      ))}
      
      {/* Show count if more tags exist */}
      {!compact && tags.filter(tag => tag !== agentTag).length > 2 && (
        <span className="text-xs text-neutral-500 font-medium">
          +{tags.filter(tag => tag !== agentTag).length - 2}
        </span>
      )}
    </div>
  )
}

export default ChatTags