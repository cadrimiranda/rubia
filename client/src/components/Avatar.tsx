import { Avatar as AntAvatar } from 'antd'

interface AvatarProps {
  src: string
  alt: string
  size?: number | 'small' | 'default' | 'large'
  isOnline?: boolean
  className?: string
}

export function Avatar({ src, alt, size = 'default', isOnline, className = '' }: AvatarProps) {
  return (
    <div className={`relative ${className}`}>
      <AntAvatar
        src={src}
        alt={alt}
        size={size}
        className="border-2 border-gray-200"
      />
      {isOnline !== undefined && (
        <div
          className={`absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-white ${
            isOnline ? 'bg-green-500' : 'bg-gray-400'
          }`}
        />
      )}
    </div>
  )
}