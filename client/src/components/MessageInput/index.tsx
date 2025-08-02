import React, { useRef } from "react";
import { Paperclip, Send, Sparkles, Loader2 } from "lucide-react";
import type {
  FileAttachment as FileAttachmentType,
  ConversationMedia,
  PendingMedia,
} from "../../types/types";
import { FileAttachment } from "../FileAttachment";
import { MediaUpload } from "../MediaUpload";
import { MediaPreview } from "../MediaPreview";

interface MessageInputProps {
  messageInput: string;
  attachments: FileAttachmentType[];
  pendingMedia?: PendingMedia[];
  conversationId?: string;
  onMessageChange: (value: string) => void;
  onSendMessage: () => void;
  onFileUpload: (files: FileList | null) => void;
  onRemoveAttachment: (id: string) => void;
  onMediaSelected?: (file: File) => void;
  onRemovePendingMedia?: (mediaId: string) => void;
  onKeyPress: (e: React.KeyboardEvent) => void;
  onEnhanceMessage?: () => void;
  onError?: (error: string) => void;
  isLoading?: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  messageInput,
  attachments,
  pendingMedia = [],
  conversationId,
  onMessageChange,
  onSendMessage,
  onFileUpload,
  onRemoveAttachment,
  onMediaSelected,
  onRemovePendingMedia,
  onKeyPress,
  onEnhanceMessage,
  onError,
  isLoading = false,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  return (
    <div className="border-t border-gray-200 bg-white p-4">
      <input
        type="file"
        ref={fileInputRef}
        onChange={(e) => {
          const files = e.target.files;
          if (files && files.length > 0 && onMediaSelected) {
            // Para o novo fluxo, selecionar apenas um arquivo por vez
            onMediaSelected(files[0]);
          } else if (files) {
            // Fallback para o fluxo antigo
            onFileUpload(files);
          }
          // Limpar o input para permitir re-seleção do mesmo arquivo
          e.target.value = "";
        }}
        accept="image/*,video/*,audio/*,.pdf,.doc,.docx,.txt"
        className="hidden"
      />

      {(attachments.length > 0 || pendingMedia.length > 0) && (
        <div className="mb-3">
          <div className="flex flex-wrap gap-2">
            {attachments.map((attachment) => (
              <FileAttachment
                key={attachment.id}
                attachment={attachment}
                isAI={false}
                variant="preview"
                onRemove={onRemoveAttachment}
              />
            ))}
            {pendingMedia.map((mediaItem) => (
              <div key={mediaItem.id} className="relative">
                {mediaItem.mediaType === "IMAGE" && mediaItem.previewUrl ? (
                  <div className="relative">
                    <img
                      src={mediaItem.previewUrl}
                      alt={mediaItem.originalFileName}
                      className="w-20 h-20 object-cover rounded-lg border"
                    />
                    <button
                      onClick={() => onRemovePendingMedia?.(mediaItem.id)}
                      className="absolute -top-2 -right-2 bg-red-500 text-white rounded-xl w-5 h-5 flex items-center justify-center text-xs hover:bg-red-600"
                    >
                      ×
                    </button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg border">
                    <div className="text-sm">
                      <div className="font-medium">
                        {mediaItem.originalFileName}
                      </div>
                      <div className="text-gray-500">
                        {(mediaItem.fileSizeBytes / 1024).toFixed(1)} KB
                      </div>
                    </div>
                    <button
                      onClick={() => onRemovePendingMedia?.(mediaItem.id)}
                      className="text-red-500 hover:text-red-700"
                    >
                      ×
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex gap-3">
        {onMediaSelected ? (
          <button
            onClick={() => fileInputRef.current?.click()}
            className="text-gray-600 hover:text-gray-800 p-2 hover:bg-gray-100 rounded-lg transition-colors self-end"
          >
            <Paperclip className="w-4 h-4" />
          </button>
        ) : (
          <button
            onClick={() => fileInputRef.current?.click()}
            className="text-gray-600 hover:text-gray-800 p-2 hover:bg-gray-100 rounded-lg transition-colors self-end"
          >
            <Paperclip className="w-4 h-4" />
          </button>
        )}

        <div className="flex-1 relative">
          <textarea
            value={messageInput}
            onChange={(e) => onMessageChange(e.target.value)}
            onKeyPress={onKeyPress}
            placeholder="Digite sua mensagem..."
            rows={1}
            className="w-full resize-none border border-gray-300 rounded-lg px-3 py-2 pr-12 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            style={{ minHeight: "40px", maxHeight: "120px" }}
          />

          {onEnhanceMessage && messageInput.trim() && (
            <button
              onClick={onEnhanceMessage}
              className="absolute right-2 top-1/2 transform -translate-y-1/2 text-purple-500 hover:text-purple-700 hover:bg-purple-50 p-1 rounded transition-colors"
              title="Melhorar mensagem com IA"
            >
              <Sparkles className="w-4 h-4" />
            </button>
          )}
        </div>

        <button
          onClick={onSendMessage}
          disabled={
            (!messageInput.trim() &&
              attachments.length === 0 &&
              pendingMedia.length === 0) ||
            isLoading
          }
          className="bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 text-white p-2 rounded-lg transition-colors self-end"
        >
          {isLoading ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Send className="w-4 h-4" />
          )}
        </button>
      </div>
    </div>
  );
};
