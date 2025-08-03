import React, { useRef, useState, useCallback, useEffect } from "react";
import { Mic, MicOff, Send, Sparkles, Loader2, Play, Pause, Trash2 } from "lucide-react";
import type { FileAttachment as FileAttachmentType, ConversationMedia, PendingMedia } from "../../types/types";
import { FileAttachment } from "../FileAttachment";

interface MessageInputProps {
  messageInput: string;
  attachments: FileAttachmentType[];
  pendingMedia?: PendingMedia[];
  conversationId?: string;
  draftMessage?: any;
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
  isAudioSending?: boolean;
  recordingCooldownMs?: number;
  maxRecordingTimeMs?: number;
  maxFileSizeMB?: number;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  messageInput,
  attachments,
  pendingMedia = [],
  conversationId,
  draftMessage,
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
  isAudioSending = false,
  recordingCooldownMs = 2000,
  maxRecordingTimeMs = 300000, // 5 minutos
  maxFileSizeMB = 16,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  
  const [isRecording, setIsRecording] = useState(false);
  const [recordedAudio, setRecordedAudio] = useState<Blob | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const [lastRecordingTime, setLastRecordingTime] = useState(0);
  const [audioError, setAudioError] = useState<string | null>(null);
  const recordingIntervalRef = useRef<number | null>(null);
  const recordingTimeoutRef = useRef<number | null>(null);

  // Validação de MIME types para áudio
  const ALLOWED_AUDIO_MIME_TYPES = ['audio/wav', 'audio/mp3', 'audio/ogg', 'audio/webm'];
  const MAX_RECORDING_TIME_SECONDS = Math.floor(maxRecordingTimeMs / 1000);

  const showAudioError = useCallback((error: string) => {
    setAudioError(error);
    onError?.(error);
    setTimeout(() => setAudioError(null), 4000);
  }, [onError]);

  const cleanupRecording = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    if (mediaRecorderRef.current) {
      mediaRecorderRef.current = null;
    }
    if (recordingIntervalRef.current) {
      clearInterval(recordingIntervalRef.current);
      recordingIntervalRef.current = null;
    }
    if (recordingTimeoutRef.current) {
      window.clearTimeout(recordingTimeoutRef.current);
      recordingTimeoutRef.current = null;
    }
  }, []);

  const startRecording = useCallback(async () => {
    // Rate limiting
    const now = Date.now();
    if (now - lastRecordingTime < recordingCooldownMs) {
      showAudioError(`Aguarde ${Math.ceil((recordingCooldownMs - (now - lastRecordingTime)) / 1000)} segundos antes de gravar novamente`);
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
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
        
        // Validação de MIME type
        if (!ALLOWED_AUDIO_MIME_TYPES.includes(audioBlob.type)) {
          showAudioError('Formato de áudio não suportado');
          cleanupRecording();
          return;
        }
        
        // Validação de tamanho do arquivo
        const fileSizeMB = audioBlob.size / (1024 * 1024);
        if (fileSizeMB > maxFileSizeMB) {
          showAudioError(`Arquivo muito grande. Tamanho máximo: ${maxFileSizeMB}MB`);
          cleanupRecording();
          return;
        }
        
        setRecordedAudio(audioBlob);
        const url = URL.createObjectURL(audioBlob);
        setAudioUrl(url);
        setLastRecordingTime(Date.now());
        cleanupRecording();
      };
      
      mediaRecorder.start();
      setIsRecording(true);
      setRecordingTime(0);
      
      recordingIntervalRef.current = setInterval(() => {
        setRecordingTime(prev => {
          const newTime = prev + 1;
          // Auto-stop quando atinge o tempo máximo
          if (newTime >= MAX_RECORDING_TIME_SECONDS) {
            stopRecording();
          }
          return newTime;
        });
      }, 1000);
      
      // Timeout de segurança
      recordingTimeoutRef.current = window.setTimeout(() => {
        if (isRecording) {
          showAudioError(`Gravação limitada a ${MAX_RECORDING_TIME_SECONDS / 60} minutos`);
          stopRecording();
        }
      }, maxRecordingTimeMs);
    } catch (error) {
      showAudioError('Erro ao acessar o microfone');
      cleanupRecording();
    }
  }, [showAudioError, lastRecordingTime, recordingCooldownMs, cleanupRecording]);
  
  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
    }
    // Cleanup será feito pelo cleanupRecording no onstop
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

  // Cleanup para evitar memory leaks
  useEffect(() => {
    return () => {
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current = null;
      }
      cleanupRecording();
    };
  }, [audioUrl, cleanupRecording]);
  
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
      {/* Indicador de mensagem DRAFT */}
      {draftMessage && (
        <div className="mb-2 p-2 bg-yellow-50 border border-yellow-200 rounded-md">
          <div className="flex items-center gap-2 text-sm text-yellow-800">
            <Sparkles className="w-4 h-4" />
            <span>Mensagem da campanha carregada - pronta para envio</span>
          </div>
        </div>
      )}

      {audioError && (
        <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-lg">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 bg-red-500 rounded-full"></div>
            <span className="text-sm text-red-700">{audioError}</span>
          </div>
        </div>
      )}

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
                disabled={isAudioSending}
                className={`px-3 py-1 rounded text-sm transition-colors flex items-center gap-1 ${
                  isAudioSending 
                    ? 'bg-gray-400 cursor-not-allowed' 
                    : 'bg-green-500 hover:bg-green-600'
                } text-white`}
              >
                {isAudioSending ? (
                  <>
                    <Loader2 className="w-3 h-3 animate-spin" />
                    Enviando...
                  </>
                ) : (
                  'Enviar'
                )}
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
            placeholder={
              draftMessage 
                ? "Edite a mensagem da campanha se necessário..."
                : "Digite sua mensagem..."
            }
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
          className={`p-2 rounded-lg transition-colors self-end ${
            ((!messageInput.trim() && attachments.length === 0 && pendingMedia.length === 0 && !recordedAudio) || isLoading || isRecording)
              ? "bg-gray-300 text-gray-500 cursor-not-allowed"
              : draftMessage
              ? "bg-yellow-500 hover:bg-yellow-600 text-white"
              : "bg-blue-500 hover:bg-blue-600 text-white"
          }`}
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
