import { useChatStore } from '../../store/useChatStore'
import type { ChatStatus } from '../../types'

const TopTabsSwitcher = () => {
  const { currentStatus, setCurrentStatus, chats } = useChatStore()

  const tabs: { key: ChatStatus; label: string }[] = [
    { key: 'entrada', label: 'Entrada' },
    { key: 'esperando', label: 'Esperando' },
    { key: 'finalizados', label: 'Finalizados' }
  ]

  const getTabCount = (status: ChatStatus): number => {
    return chats.filter(chat => chat.status === status).length
  }

  const getBadgeColor = () => {
    return 'bg-ruby-500 text-white'
  }

  return (
    <div className="flex bg-neutral-100 p-1" style={{ borderRadius: '100px' }}>
      {tabs.map((tab) => {
        const count = getTabCount(tab.key)
        const isActive = currentStatus === tab.key
        
        const getButtonClass = () => {
          let baseClass = 'h-8 px-3 text-sm font-medium transition-all duration-200 flex items-center gap-1 flex-1 justify-center'
          
          if (isActive) {
            baseClass += ' bg-white text-ruby-500 shadow-sm'
          } else {
            baseClass += ' bg-transparent text-neutral-600 hover:text-neutral-700 hover:bg-neutral-200/50'
          }
          
          return baseClass
        }
        
        return (
          <button
            key={tab.key}
            onClick={() => setCurrentStatus(tab.key)}
            className={getButtonClass()}
            style={{ borderRadius: '100px' }}
          >
            <span>{tab.label}</span>
            <span
              className={`px-2 py-0.5 rounded-2xl text-xs font-semibold min-w-[20px] text-center ${getBadgeColor()}`}
            >
              {count}
            </span>
          </button>
        )
      })}
    </div>
  )
}

export default TopTabsSwitcher