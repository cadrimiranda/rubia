import { Avatar } from 'antd'
import { MessageCircle, Pin } from 'lucide-react'
import type { Chat } from '../../types'
import { formatChatListTime, truncateText } from '../../utils/format'
import { useChatStore } from '../../store/useChatStore'
import ChatTags from '../ChatTags'

interface ChatListItemProps {
  chat: Chat
}

const ChatListItem = ({ chat }: ChatListItemProps) => {
  const { activeChat, setActiveChat } = useChatStore()
  const isActive = activeChat?.id === chat.id

  const handleClick = () => {
    setActiveChat(chat)
  }

  return (
    <div
      onClick={handleClick}
      className={`relative flex items-start px-4 py-3 cursor-pointer transition-all duration-200 group ${
        isActive
          ? 'bg-blue-50 shadow-sm border-l-4 border-l-blue-500'
          : 'hover:bg-blue-50/30'
      }`}
    >
      {/* Avatar Section */}
      <div className="relative mr-4 flex-shrink-0">
        <Avatar
          src={chat.contact.avatar}
          size={48}
          className="ring-2 ring-white shadow-sm"
        >
          {chat.contact.name.charAt(0).toUpperCase()}
        </Avatar>
        
        {/* Status Indicator */}
        <div className="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-success-500 rounded-full flex items-center justify-center border-2 border-white shadow-sm">
          <MessageCircle size={8} className="text-white" />
        </div>
        
        {/* Online Status */}
        {chat.contact.isOnline && (
          <div className="absolute top-0 right-0 w-3 h-3 bg-green-400 border-2 border-white rounded-full"></div>
        )}
        
        {/* Unread Indicator */}
        {chat.unreadCount > 0 && (
          <div className="absolute -top-1 -left-1 w-3 h-3 bg-red-500 rounded-full ring-2 ring-white"></div>
        )}
      </div>

      {/* Content Section */}
      <div className="flex-1 min-w-0 space-y-1">
        {/* Header Row */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 min-w-0 flex-1">
            <h4 className={`truncate text-lg font-semibold ${
              isActive ? 'text-blue-500' : 'text-neutral-900'
            }`}>
              {chat.contact.name}
            </h4>
            {chat.isPinned && (
              <Pin size={12} className="text-gray-400 flex-shrink-0" />
            )}
          </div>
          <div className="flex items-center gap-2 flex-shrink-0">
            {chat.unreadCount > 0 && (
              <span className="bg-red-500 text-white text-xs font-medium px-1.5 py-0.5 rounded-full min-w-[18px] text-center leading-none">
                {chat.unreadCount > 99 ? '99+' : chat.unreadCount}
              </span>
            )}
            <span className="text-xs text-neutral-500 font-normal">
              {chat.lastMessage && formatChatListTime(chat.lastMessage.timestamp)}
            </span>
          </div>
        </div>

        {/* Message Preview */}
        <p className={`text-base truncate leading-relaxed ${
          isActive ? 'text-neutral-800 font-medium' : 'text-neutral-700 font-normal'
        }`}>
          {chat.lastMessage 
            ? truncateText(chat.lastMessage.content, 45)
            : 'Nenhuma mensagem'
          }
        </p>

        {/* Blood Type & Medical Info Row */}
        <div className="flex items-center justify-between pt-0.5">
          <div className="flex items-center gap-2">
            {chat.contact.bloodType && (
              <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-1 rounded">
                {chat.contact.bloodType}
              </span>
            )}
            <ChatTags tags={chat.tags} compact showAgent={false} />
          </div>
          {chat.assignedAgent && (
            <div className={`text-xs font-medium px-2 py-1 rounded-xl border ${
              isActive 
                ? 'bg-white text-blue-500 border-blue-500' 
                : 'bg-blue-50 text-blue-500 border-blue-100'
            }`}>
              {chat.assignedAgent}
            </div>
          )}
        </div>
      </div>

    </div>
  )
}

export default ChatListItem