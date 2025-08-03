import React from "react";
import { FileText, Download, Trash2, Volume2 } from "lucide-react";
import type { ConversationMedia } from "../../types/types";
import { MediaPlayer } from "../MediaPlayer";

interface MediaPreviewProps {
  media: ConversationMedia;
  variant?: "preview" | "message";
  onRemove?: (mediaId: string) => void;
  onDownload?: (mediaId: string) => void;
}

export const MediaPreview: React.FC<MediaPreviewProps> = ({
  media,
  variant = "message",
  onRemove,
  onDownload,
}) => {
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const getFileIcon = (mediaType: string) => {
    switch (mediaType) {
      case "AUDIO":
        return <Volume2 className="w-4 h-4 text-red-500" />;
      case "VIDEO":
        return <Volume2 className="w-4 h-4 text-blue-500" />;
      case "IMAGE":
        return null; // Mostra a imagem
      default:
        return <FileText className="w-4 h-4 text-gray-500" />;
    }
  };

  const handleDownload = () => {
    if (onDownload) {
      onDownload(media.id);
    } else {
      // Download direto
      const link = document.createElement("a");
      link.href = media.fileUrl;
      link.download = media.originalFileName;
      link.click();
    }
  };

  const renderContent = () => {
    switch (media.mediaType) {
      case "IMAGE":
        return (
          <div className="relative">
            <img
              src={media.fileUrl}
              alt={media.originalFileName}
              className="max-w-xs max-h-48 rounded-lg object-cover"
            />
            {variant === "preview" && onRemove && (
              <button
                onClick={() => onRemove(media.id)}
                className="absolute -top-2 -right-2 bg-red-500 text-white rounded-xl p-1 hover:bg-red-600"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            )}
          </div>
        );

      case "VIDEO":
      case "AUDIO":
        return (
          <div className="relative">
            <MediaPlayer
              media={media}
              variant={variant === "preview" ? "modal" : "inline"}
            />
            {variant === "preview" && onRemove && (
              <button
                onClick={() => onRemove(media.id)}
                className="absolute -top-2 -right-2 bg-red-500 text-white rounded-xl p-1 hover:bg-red-600 z-10"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            )}
          </div>
        );

      default:
        return (
          <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg max-w-xs">
            <div className="flex items-center gap-2">
              {getFileIcon(media.mediaType)}
              <div>
                <p className="text-sm font-medium text-gray-800 truncate">
                  {media.originalFileName}
                </p>
                <p className="text-xs text-gray-500">
                  {formatFileSize(media.fileSizeBytes)}
                </p>
              </div>
            </div>
            <div className="flex gap-1">
              <button
                onClick={handleDownload}
                className="p-1 text-gray-500 hover:text-gray-700"
              >
                <Download className="w-4 h-4" />
              </button>
              {variant === "preview" && onRemove && (
                <button
                  onClick={() => onRemove(media.id)}
                  className="p-1 text-red-500 hover:text-red-700"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>
        );
    }
  };

  return <div className="inline-block">{renderContent()}</div>;
};
