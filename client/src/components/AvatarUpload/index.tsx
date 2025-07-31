import React, { useState } from 'react';
import { Upload, Avatar, Button, message } from 'antd';
import { UploadOutlined, UserOutlined, DeleteOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd/es/upload/interface';

interface AvatarUploadProps {
  value?: string; // Base64 string atual
  onChange?: (base64: string | null) => void; // Callback quando avatar muda
  size?: number; // Tamanho do avatar em pixels
  disabled?: boolean;
  placeholder?: string;
}

export const AvatarUpload: React.FC<AvatarUploadProps> = ({
  value,
  onChange,
  size = 80,
  disabled = false,
  placeholder = 'Clique para fazer upload',
}) => {
  const [uploading, setUploading] = useState(false);

  // Validar tamanho do arquivo (máximo 2MB)
  const validateFileSize = (file: File): boolean => {
    const maxSize = 2 * 1024 * 1024; // 2MB
    if (file.size > maxSize) {
      message.error('Imagem muito grande! Máximo 2MB permitido.');
      return false;
    }
    return true;
  };

  // Validar tipo do arquivo
  const validateFileType = (file: File): boolean => {
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
    if (!allowedTypes.includes(file.type)) {
      message.error('Formato não suportado! Use JPG, PNG ou GIF.');
      return false;
    }
    return true;
  };

  // Converter arquivo para base64
  const fileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        if (reader.result && typeof reader.result === 'string') {
          resolve(reader.result);
        } else {
          reject(new Error('Erro ao processar arquivo'));
        }
      };
      reader.onerror = () => reject(new Error('Erro ao ler arquivo'));
      reader.readAsDataURL(file);
    });
  };

  // Handler do upload customizado
  const handleUpload: UploadProps['customRequest'] = async (options) => {
    const { file, onSuccess, onError } = options;
    const uploadFile = file as File;

    setUploading(true);

    try {
      // Validações
      if (!validateFileType(uploadFile) || !validateFileSize(uploadFile)) {
        onError?.(new Error('Validação falhou'));
        return;
      }

      // Converter para base64
      const base64 = await fileToBase64(uploadFile);
      
      // Atualizar estado
      onChange?.(base64);
      onSuccess?.(base64);
      message.success('Avatar atualizado com sucesso!');
      
    } catch (error) {
      console.error('Erro no upload:', error);
      message.error('Erro ao processar imagem');
      onError?.(error as Error);
    } finally {
      setUploading(false);
    }
  };

  // Remover avatar
  const handleRemove = () => {
    onChange?.(null);
    message.success('Avatar removido');
  };

  // Props do Upload do Ant Design
  const uploadProps: UploadProps = {
    name: 'avatar',
    listType: 'picture',
    showUploadList: false,
    accept: 'image/jpeg,image/jpg,image/png,image/gif',
    customRequest: handleUpload,
    disabled: disabled || uploading,
    beforeUpload: () => false, // Previne upload automático
  };

  return (
    <div className="flex flex-col items-center gap-3">
      {/* Avatar Display */}
      <div className="relative">
        <Avatar
          size={size}
          src={value || undefined}
          icon={!value ? <UserOutlined /> : undefined}
          className="border-2 border-gray-200 shadow-sm"
        />
        
        {/* Botão de remoção quando há avatar */}
        {value && !disabled && (
          <Button
            type="text"
            danger
            size="small"
            icon={<DeleteOutlined />}
            onClick={handleRemove}
            className="absolute -top-1 -right-1 w-6 h-6 rounded-full bg-red-500 text-white border-0 flex items-center justify-center hover:bg-red-600"
            style={{ fontSize: '10px' }}
          />
        )}
      </div>

      {/* Upload Button */}
      <Upload {...uploadProps}>
        <Button
          icon={<UploadOutlined />}
          loading={uploading}
          disabled={disabled}
          size="small"
          type="dashed"
        >
          {uploading ? 'Processando...' : placeholder}
        </Button>
      </Upload>

      {/* Informações de ajuda */}
      <div className="text-xs text-gray-500 text-center max-w-32">
        JPG, PNG, GIF
        <br />
        Máx. 2MB
      </div>
    </div>
  );
};

export default AvatarUpload;