import React, { useRef } from "react";
import { Paperclip, Send } from "lucide-react";
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
}

export const MessageInput: React.FC<MessageInputProps> = ({
  messageInput,
  attachments,
  onMessageChange,
  onSendMessage,
  onFileUpload,
  onRemoveAttachment,
  onKeyPress,
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
        <div className="max-w-4xl mx-auto mb-3">
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

      <div className="max-w-4xl mx-auto flex gap-3">
        <button
          onClick={() => fileInputRef.current?.click()}
          className="text-gray-600 hover:text-gray-800 p-2 hover:bg-gray-100 rounded-lg transition-colors self-end"
        >
          <Paperclip className="w-4 h-4" />
        </button>

        <textarea
          value={messageInput}
          onChange={(e) => onMessageChange(e.target.value)}
          onKeyPress={onKeyPress}
          placeholder="Digite sua mensagem..."
          rows={1}
          className="flex-1 resize-none border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          style={{ minHeight: "40px", maxHeight: "120px" }}
        />

        <button
          onClick={onSendMessage}
          disabled={!messageInput.trim() && attachments.length === 0}
          className="bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 text-white p-2 rounded-lg transition-colors self-end"
        >
          <Send className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
