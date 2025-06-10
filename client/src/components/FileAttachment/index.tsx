import React from "react";
import { Image, FileText, Download, X } from "lucide-react";
import type { FileAttachment as FileAttachmentType } from "../../types/types";
import { formatFileSize } from "../../utils";

interface FileAttachmentProps {
  attachment: FileAttachmentType;
  isAI: boolean;
  variant?: "preview" | "message";
  onRemove?: (id: string) => void;
}

export const FileAttachment: React.FC<FileAttachmentProps> = ({
  attachment,
  isAI,
  variant = "message",
  onRemove,
}) => {
  const getFileIcon = (type: string) => {
    if (type.startsWith("image/")) return <Image className="w-4 h-4" />;
    return <FileText className="w-4 h-4" />;
  };

  if (variant === "preview") {
    return (
      <div className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg">
        {getFileIcon(attachment.type)}
        <span className="text-xs text-gray-700 max-w-32 truncate">
          {attachment.name}
        </span>
        {onRemove && (
          <button
            onClick={() => onRemove(attachment.id)}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-3 h-3" />
          </button>
        )}
      </div>
    );
  }

  return (
    <div
      className={`flex items-center gap-2 p-2 rounded-lg ${
        isAI ? "bg-gray-50" : "bg-blue-600"
      }`}
    >
      {getFileIcon(attachment.type)}
      <div className="flex-1 min-w-0">
        <p
          className={`text-xs font-medium truncate m-0 ${
            isAI ? "text-gray-800" : "text-white"
          }`}
        >
          {attachment.name}
        </p>
        <p className={`text-xs ${isAI ? "text-gray-500" : "text-blue-100"}`}>
          {formatFileSize(attachment.size)}
        </p>
      </div>
      <button
        className={`${
          isAI
            ? "text-gray-600 hover:text-gray-800"
            : "text-blue-100 hover:text-white"
        }`}
      >
        <Download className="w-3 h-3" />
      </button>
    </div>
  );
};
