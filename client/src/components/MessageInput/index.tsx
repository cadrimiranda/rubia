import React, { useRef, useState, useCallback } from "react";
import { Mic, MicOff, Send, Sparkles, Loader2, Play, Pause, Trash2 } from "lucide-react";
import type { FileAttachment as FileAttachmentType, ConversationMedia, PendingMedia } from "../../types/types";
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
  onAudioRecorded?: (audioBlob: Blob) => void;
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
  onAudioRecorded,
  isLoading = false,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  
  const [isRecording, setIsRecording] = useState(false);
  const [recordedAudio, setRecordedAudio] = useState<Blob | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const recordingIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const startRecording = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];
      
      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };
      
      mediaRecorder.onstop = () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/wav' });
        setRecordedAudio(audioBlob);
        const url = URL.createObjectURL(audioBlob);
        setAudioUrl(url);
        stream.getTracks().forEach(track => track.stop());
      };
      
      mediaRecorder.start();
      setIsRecording(true);
      setRecordingTime(0);
      
      recordingIntervalRef.current = setInterval(() => {
        setRecordingTime(prev => prev + 1);
      }, 1000);
    } catch (error) {
      onError?.('Erro ao acessar o microfone');
    }
  }, [onError]);
  
  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      if (recordingIntervalRef.current) {
        clearInterval(recordingIntervalRef.current);
        recordingIntervalRef.current = null;
      }
    }
  }, [isRecording]);
  
  const playAudio = useCallback(() => {
    if (audioUrl && !isPlaying) {
      if (audioRef.current) {
        audioRef.current.pause();
      }
      
      const audio = new Audio(audioUrl);
      audioRef.current = audio;
      
      audio.onended = () => {
        setIsPlaying(false);
        audioRef.current = null;
      };
      
      audio.play();
      setIsPlaying(true);
    }
  }, [audioUrl, isPlaying]);
  
  const pauseAudio = useCallback(() => {
    if (audioRef.current && isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
      audioRef.current = null;
    }
  }, [isPlaying]);
  
  const removeAudio = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current = null;
    }
    if (audioUrl) {
      URL.revokeObjectURL(audioUrl);
    }
    setRecordedAudio(null);
    setAudioUrl(null);
    setIsPlaying(false);
    setRecordingTime(0);
  }, [audioUrl]);
  
  const sendAudio = useCallback(() => {
    if (recordedAudio && onAudioRecorded) {
      onAudioRecorded(recordedAudio);
      removeAudio();
    }
  }, [recordedAudio, onAudioRecorded, removeAudio]);
  
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="border-t border-gray-200 bg-white p-4">

      {recordedAudio && (
        <div className="mb-3 p-3 bg-gray-50 rounded-lg border">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <button
                onClick={isPlaying ? pauseAudio : playAudio}
                className="text-blue-500 hover:text-blue-700 p-1 hover:bg-blue-50 rounded transition-colors"
              >
                {isPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
              </button>
              <span className="text-sm text-gray-600">
                Áudio gravado • {formatTime(recordingTime)}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={removeAudio}
                className="text-red-500 hover:text-red-700 p-1 hover:bg-red-50 rounded transition-colors"
                title="Remover áudio"
              >
                <Trash2 className="w-4 h-4" />
              </button>
              <button
                onClick={sendAudio}
                className="bg-green-500 hover:bg-green-600 text-white px-3 py-1 rounded text-sm transition-colors"
              >
                Enviar
              </button>
            </div>
          </div>
        </div>
      )}

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
                {mediaItem.mediaType === 'IMAGE' && mediaItem.previewUrl ? (
                  <div className="relative">
                    <img 
                      src={mediaItem.previewUrl} 
                      alt={mediaItem.originalFileName}
                      className="w-20 h-20 object-cover rounded-lg border"
                    />
                    <button
                      onClick={() => onRemovePendingMedia?.(mediaItem.id)}
                      className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs hover:bg-red-600"
                    >
                      ×
                    </button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg border">
                    <div className="text-sm">
                      <div className="font-medium">{mediaItem.originalFileName}</div>
                      <div className="text-gray-500">{(mediaItem.fileSizeBytes / 1024).toFixed(1)} KB</div>
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
        <button
          onClick={isRecording ? stopRecording : startRecording}
          className={`p-2 rounded-lg transition-colors self-end ${
            isRecording 
              ? 'text-red-500 hover:text-red-700 bg-red-50 hover:bg-red-100' 
              : 'text-gray-600 hover:text-gray-800 hover:bg-gray-100'
          }`}
          disabled={!!recordedAudio}
          title={isRecording ? 'Parar gravação' : 'Gravar áudio'}
        >
          {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
        </button>
        
        {isRecording && (
          <div className="flex items-center gap-2 px-3 py-2 bg-red-50 rounded-lg self-end">
            <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
            <span className="text-sm text-red-600">{formatTime(recordingTime)}</span>
          </div>
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
          disabled={(!messageInput.trim() && attachments.length === 0 && pendingMedia.length === 0 && !recordedAudio) || isLoading || isRecording}
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
