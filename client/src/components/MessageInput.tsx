import React, { useRef } from "react";
import { Send, Paperclip, Sparkles } from "lucide-react";
import type { FileAttachment, PendingMedia } from "../types/types";

interface MessageInputProps {
  messageInput: string;
  attachments: FileAttachment[];
  pendingMedia: PendingMedia[];
  conversationId?: string;
  draftMessage?: any;
  onMessageChange: (value: string) => void;
  onSendMessage: () => void;
  onFileUpload: (files: FileList) => void;
  onRemoveAttachment: (id: string) => void;
  onMediaSelected: (media: any) => void;
  onRemovePendingMedia: (id: string) => void;
  onKeyPress: (e: React.KeyboardEvent) => void;
  onEnhanceMessage: () => void;
  onError: (error: string) => void;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  messageInput,
  attachments,
  pendingMedia,
  draftMessage,
  onMessageChange,
  onSendMessage,
  onFileUpload,
  onRemoveAttachment,
  onRemovePendingMedia,
  onKeyPress,
  onEnhanceMessage,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSend = async () => {
    onSendMessage();
  };

  const handleFileSelect = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      onFileUpload(e.target.files);
    }
  };

  return (
    <div className="border-t border-gray-200 bg-white p-4">
      {/* Indicador de mensagem DRAFT */}
      {draftMessage && (
        <div className="mb-2 p-2 bg-yellow-50 border border-yellow-200 rounded-md">
          <div className="flex items-center gap-2 text-sm text-yellow-800">
            <Sparkles className="w-4 h-4" />
            <span>Mensagem da campanha carregada - pronta para envio</span>
          </div>
        </div>
      )}

      {/* Anexos pendentes */}
      {attachments.length > 0 && (
        <div className="mb-3 flex flex-wrap gap-2">
          {attachments.map((attachment) => (
            <div
              key={attachment.id}
              className="flex items-center gap-2 bg-gray-100 rounded-lg px-3 py-2"
            >
              <Paperclip className="w-4 h-4 text-gray-500" />
              <span className="text-sm text-gray-700">{attachment.name}</span>
              <button
                onClick={() => onRemoveAttachment(attachment.id)}
                className="text-red-500 hover:text-red-700"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Mídia pendente */}
      {pendingMedia.length > 0 && (
        <div className="mb-3 flex flex-wrap gap-2">
          {pendingMedia.map((media) => (
            <div key={media.id} className="relative bg-gray-100 rounded-lg p-2">
              {media.mimeType.startsWith("image/") && (
                <img
                  src={URL.createObjectURL(media.file)}
                  alt="Preview"
                  className="w-16 h-16 object-cover rounded"
                />
              )}
              <button
                onClick={() => onRemovePendingMedia(media.id)}
                className="absolute -top-1 -right-1 bg-red-500 text-white rounded-xl w-5 h-5 flex items-center justify-center text-xs"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="flex items-end gap-2">
        {/* Input de arquivo oculto */}
        <input
          ref={fileInputRef}
          type="file"
          multiple
          className="hidden"
          onChange={handleFileChange}
        />

        {/* Botões de ação */}
        <div className="flex gap-1">
          <button
            onClick={handleFileSelect}
            className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-md transition-colors"
            title="Anexar arquivo"
          >
            <Paperclip className="w-5 h-5" />
          </button>

          <button
            onClick={onEnhanceMessage}
            className="p-2 text-blue-500 hover:text-blue-700 hover:bg-blue-50 rounded-md transition-colors"
            title="Melhorar mensagem com IA"
          >
            <Sparkles className="w-5 h-5" />
          </button>
        </div>

        {/* Campo de texto */}
        <div className="flex-1 relative">
          <textarea
            value={messageInput}
            onChange={(e) => onMessageChange(e.target.value)}
            onKeyPress={onKeyPress}
            placeholder={
              draftMessage
                ? "Edite a mensagem da campanha se necessário..."
                : "Digite sua mensagem..."
            }
            className="w-full p-3 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent max-h-32 min-h-[44px]"
            rows={1}
            style={{
              height: Math.min(
                Math.max(44, messageInput.split("\n").length * 20 + 24),
                128
              ),
            }}
          />
        </div>

        {/* Botão de envio */}
        <button
          onClick={handleSend}
          disabled={
            !messageInput.trim() &&
            attachments.length === 0 &&
            pendingMedia.length === 0
          }
          className={`p-2 rounded-lg transition-colors ${
            !messageInput.trim() &&
            attachments.length === 0 &&
            pendingMedia.length === 0
              ? "bg-gray-200 text-gray-400 cursor-not-allowed"
              : draftMessage
              ? "bg-yellow-500 hover:bg-yellow-600 text-white"
              : "bg-blue-500 hover:bg-blue-600 text-white"
          }`}
          title={
            draftMessage ? "Enviar mensagem da campanha" : "Enviar mensagem"
          }
        >
          <Send className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
