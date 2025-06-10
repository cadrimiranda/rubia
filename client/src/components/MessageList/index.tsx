import React, { useRef, useEffect } from "react";
import { Upload } from "lucide-react";
import type { Message as MessageType } from "../../types/types";
import { Message } from "../Message";

interface MessageListProps {
  messages: MessageType[];
  isDragging: boolean;
  onDragOver: (e: React.DragEvent) => void;
  onDragLeave: (e: React.DragEvent) => void;
  onDrop: (e: React.DragEvent) => void;
}

export const MessageList: React.FC<MessageListProps> = ({
  messages,
  isDragging,
  onDragOver,
  onDragLeave,
  onDrop,
}) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  return (
    <div
      className={`flex-1 overflow-y-auto p-4 bg-gray-50 ${
        isDragging ? "bg-blue-50 border-2 border-dashed border-blue-300" : ""
      }`}
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
    >
      {isDragging && (
        <div className="absolute inset-4 flex items-center justify-center bg-blue-50 bg-opacity-90 rounded-lg">
          <div className="text-center">
            <Upload className="w-12 h-12 text-blue-500 mx-auto mb-2" />
            <p className="text-blue-600 font-medium">Solte os arquivos aqui</p>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {messages.map((message) => (
          <Message key={message.id} message={message} />
        ))}
        <div ref={messagesEndRef} />
      </div>
    </div>
  );
};
