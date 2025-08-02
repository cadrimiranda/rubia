import React, { useState, useRef } from "react";
import { Avatar, Button, message } from "antd";
import {
  UploadOutlined,
  UserOutlined,
  DeleteOutlined,
} from "@ant-design/icons";

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
  placeholder = "Clique para fazer upload",
}) => {
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Validar tamanho do arquivo (máximo 2MB)
  const validateFileSize = (file: File): boolean => {
    const maxSize = 2 * 1024 * 1024; // 2MB
    if (file.size > maxSize) {
      message.error("Imagem muito grande! Máximo 2MB permitido.");
      return false;
    }
    return true;
  };

  // Validar tipo do arquivo
  const validateFileType = (file: File): boolean => {
    const allowedTypes = ["image/jpeg", "image/jpg", "image/png", "image/gif"];
    if (!allowedTypes.includes(file.type)) {
      message.error("Formato não suportado! Use JPG, PNG ou GIF.");
      return false;
    }
    return true;
  };

  // Converter arquivo para base64
  const fileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        if (reader.result && typeof reader.result === "string") {
          resolve(reader.result);
        } else {
          reject(new Error("Erro ao processar arquivo"));
        }
      };
      reader.onerror = () => reject(new Error("Erro ao ler arquivo"));
      reader.readAsDataURL(file);
    });
  };

  // Handler do upload customizado com input nativo
  const handleFileChange = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;

    console.log("🔵 AvatarUpload: Starting upload", {
      fileName: file.name,
      fileSize: file.size,
      fileType: file.type,
    });

    setUploading(true);

    try {
      // Validações
      if (!validateFileType(file)) {
        console.log("❌ File type validation failed");
        return;
      }

      if (!validateFileSize(file)) {
        console.log("❌ File size validation failed");
        return;
      }

      console.log("✅ File validations passed");

      // Converter para base64
      const base64 = await fileToBase64(file);
      console.log(
        "✅ Base64 conversion successful",
        base64.substring(0, 50) + "..."
      );

      // Atualizar estado
      console.log("🔄 Calling onChange with base64");
      onChange?.(base64);
      message.success("Avatar atualizado com sucesso!");
    } catch (error) {
      console.error("❌ Erro no upload:", error);
      message.error("Erro ao processar imagem");
    } finally {
      setUploading(false);
      // Reset input value to allow selecting the same file again
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  // Trigger file input click
  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  // Remover avatar
  const handleRemove = () => {
    onChange?.(null);
    message.success("Avatar removido");
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
            className="absolute -top-1 -right-1 w-6 h-6 rounded-xl bg-red-500 text-white border-0 flex items-center justify-center hover:bg-red-600"
            style={{ fontSize: "10px" }}
          />
        )}
      </div>

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/jpg,image/png,image/gif"
        onChange={handleFileChange}
        style={{ display: "none" }}
        disabled={disabled || uploading}
      />

      {/* Upload Button */}
      <Button
        icon={<UploadOutlined />}
        loading={uploading}
        disabled={disabled}
        size="small"
        type="dashed"
        onClick={handleUploadClick}
      >
        {uploading ? "Processando..." : placeholder}
      </Button>

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
