import { Skeleton } from 'antd'

interface ChatListSkeletonProps {
  count?: number
}

const ChatListSkeleton: React.FC<ChatListSkeletonProps> = ({ count = 5 }) => {
  return (
    <div className="space-y-1">
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className="flex items-center space-x-3 p-3 bg-white hover:bg-gray-50 cursor-pointer transition-colors duration-200"
        >
          {/* Avatar Skeleton */}
          <div className="flex-shrink-0">
            <Skeleton.Avatar size={44} className="bg-gray-200" />
          </div>

          {/* Content Skeleton */}
          <div className="flex-1 min-w-0 space-y-2">
            {/* Name and Time */}
            <div className="flex items-center justify-between">
              <Skeleton.Input
                style={{ width: '120px', height: '14px' }}
                size="small"
                active
                className="bg-gray-200"
              />
              <Skeleton.Input
                style={{ width: '40px', height: '12px' }}
                size="small"
                active
                className="bg-gray-200"
              />
            </div>

            {/* Last Message */}
            <div className="flex items-center justify-between">
              <Skeleton.Input
                style={{ width: '180px', height: '12px' }}
                size="small"
                active
                className="bg-gray-200"
              />
              
              {/* Unread Badge Placeholder */}
              {index % 3 === 0 && (
                <div className="w-5 h-5 bg-gray-200 rounded-full animate-pulse" />
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

export default ChatListSkeleton