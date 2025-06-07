import { Skeleton } from 'antd'

interface MessageSkeletonProps {
  count?: number
  isFromUser?: boolean
}

const MessageSkeleton: React.FC<MessageSkeletonProps> = ({ 
  count = 3, 
  isFromUser 
}) => {
  return (
    <div className="space-y-4 p-4">
      {Array.from({ length: count }).map((_, index) => {
        const messageFromUser = isFromUser ?? index % 2 === 0
        
        return (
          <div
            key={index}
            className={`flex ${messageFromUser ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-xs lg:max-w-md p-3 rounded-2xl ${
                messageFromUser 
                  ? 'bg-gray-200 rounded-br-md' 
                  : 'bg-gray-200 rounded-bl-md'
              }`}
            >
              {/* Message Content Skeleton */}
              <div className="space-y-2">
                <Skeleton.Input
                  style={{ 
                    width: `${Math.random() * 100 + 100}px`, 
                    height: '14px' 
                  }}
                  size="small"
                  active
                  className="bg-gray-300"
                />
                {Math.random() > 0.5 && (
                  <Skeleton.Input
                    style={{ 
                      width: `${Math.random() * 80 + 60}px`, 
                      height: '14px' 
                    }}
                    size="small"
                    active
                    className="bg-gray-300"
                  />
                )}
              </div>

              {/* Timestamp Skeleton */}
              <div className="mt-2">
                <Skeleton.Input
                  style={{ width: '50px', height: '10px' }}
                  size="small"
                  active
                  className="bg-gray-300"
                />
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default MessageSkeleton