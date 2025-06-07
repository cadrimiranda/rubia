import { Menu } from 'antd'
import { 
  ArrowRightLeft, 
  Tag as TagIcon, 
  MailMinus, 
  Ban, 
  CheckSquare, 
  ArrowUp, 
  Pin 
} from 'lucide-react'
import type { Chat } from '../../types'
import { useChatStore } from '../../store/useChatStore'

interface MessageOptionsMenuProps {
  chat: Chat
}

const MessageOptionsMenu = ({ chat }: MessageOptionsMenuProps) => {
  const { assignToAgent, blockCustomer, changeStatus, pinConversation } = useChatStore()

  const handleTransferChat = () => {
    const agentId = prompt('ID do agente para transferir:')
    if (agentId) {
      assignToAgent(chat.id, agentId)
    }
  }

  const handleAddTag = () => {
    console.log('Adicionar etiqueta')
  }

  const handleMarkUnread = () => {
    console.log('Marcar como não lida')
  }

  const handleBlockContact = () => {
    if (window.confirm('Tem certeza que deseja bloquear este contato?')) {
      blockCustomer(chat.id)
    }
  }

  const handleFinalizeChat = () => {
    if (window.confirm('Tem certeza que deseja finalizar esta conversa?')) {
      changeStatus(chat.id, 'finalizados')
    }
  }

  const handleRemoveFromWaiting = () => {
    console.log('Retirar dos esperando')
  }

  const handlePinChat = () => {
    pinConversation(chat.id)
  }

  const menuItems = [
    {
      key: 'transfer',
      label: 'Transferir conversa',
      icon: <ArrowRightLeft size={16} />,
      onClick: handleTransferChat
    },
    {
      key: 'addTag',
      label: 'Adicionar etiqueta',
      icon: <TagIcon size={16} />,
      onClick: handleAddTag
    },
    {
      key: 'markUnread',
      label: 'Marcar como não lida',
      icon: <MailMinus size={16} />,
      onClick: handleMarkUnread
    },
    {
      key: 'block',
      label: 'Bloquear contato',
      icon: <Ban size={16} />,
      onClick: handleBlockContact,
      danger: true
    },
    {
      key: 'finalize',
      label: 'Finalizar conversa',
      icon: <CheckSquare size={16} />,
      onClick: handleFinalizeChat
    },
    {
      key: 'removeWaiting',
      label: 'Retirar dos esperando',
      icon: <ArrowUp size={16} />,
      onClick: handleRemoveFromWaiting,
      disabled: chat.status !== 'esperando'
    },
    {
      key: 'pin',
      label: chat.isPinned ? 'Desfixar conversa' : 'Fixar conversa',
      icon: <Pin size={16} />,
      onClick: handlePinChat
    }
  ]

  return (
    <Menu
      className="w-64 shadow-lg border border-gray-200 rounded-lg"
      items={menuItems.map(item => ({
        key: item.key,
        label: (
          <div 
            className={`flex items-center space-x-3 py-2 px-1 rounded-md transition-colors ${
              item.danger 
                ? 'text-red-600 hover:bg-red-50' 
                : 'text-gray-700 hover:bg-gray-50'
            } ${item.disabled ? 'text-gray-400 cursor-not-allowed' : 'cursor-pointer'}`}
            onClick={item.disabled ? undefined : item.onClick}
          >
            <span className="flex-shrink-0">{item.icon}</span>
            <span className="font-medium">{item.label}</span>
          </div>
        ),
        disabled: item.disabled,
        style: { padding: '4px 8px' }
      }))}
    />
  )
}

export default MessageOptionsMenu