import React, { useRef } from "react";
import { Paperclip, Send, Sparkles, Loader2 } from "lucide-react";
import type { FileAttachment as FileAttachmentType } from "../../types/types";
import { FileAttachment } from "../FileAttachment";

interface MessageInputProps {
  messageInput: string;
  attachments: FileAttachmentType[];
  onMessageChange: (value: string) => void;
  onSendMessage: () => void;
  onFileUpload: (files: FileList | null) => void;
  onRemoveAttachment: (id: string) => void;
  onKeyPress: (e: React.KeyboardEvent) => void;
  onEnhanceMessage?: () => void;
  isLoading?: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  messageInput,
  attachments,
  onMessageChange,
  onSendMessage,
  onFileUpload,
  onRemoveAttachment,
  onKeyPress,
  onEnhanceMessage,
  isLoading = false,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  return (
    <div className="border-t border-gray-200 bg-white p-4">
      <input
        type="file"
        ref={fileInputRef}
        onChange={(e) => onFileUpload(e.target.files)}
        multiple
        accept="image/*,.pdf,.doc,.docx,.txt"
        className="hidden"
      />

      {attachments.length > 0 && (
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
          </div>
        </div>
      )}

      <div className="flex gap-3">
        <button
          onClick={() => fileInputRef.current?.click()}
          className="text-gray-600 hover:text-gray-800 p-2 hover:bg-gray-100 rounded-lg transition-colors self-end"
        >
          <Paperclip className="w-4 h-4" />
        </button>

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
          disabled={(!messageInput.trim() && attachments.length === 0) || isLoading}
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
