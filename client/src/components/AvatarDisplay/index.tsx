import React from 'react';
import { Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';

interface AvatarDisplayProps {
  avatarBase64?: string | null;
  size?: number | 'small' | 'default' | 'large';
  className?: string;
  fallbackIcon?: React.ReactNode;
  alt?: string;
}

export const AvatarDisplay: React.FC<AvatarDisplayProps> = ({
  avatarBase64,
  size = 'default',
  className = '',
  fallbackIcon = <UserOutlined />,
  alt = 'Avatar',
}) => {
  // Verificar se o base64 é válido
  const isValidBase64 = avatarBase64 && avatarBase64.startsWith('data:image/');

  return (
    <Avatar
      size={size}
      src={isValidBase64 ? avatarBase64 : undefined}
      icon={!isValidBase64 ? fallbackIcon : undefined}
      alt={alt}
      className={`${className} ${!isValidBase64 ? 'bg-gray-200' : ''}`}
    />
  );
};

export default AvatarDisplay;