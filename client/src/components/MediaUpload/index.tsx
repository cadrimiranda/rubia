import React, { useRef, useState } from 'react';
import { Paperclip, Camera, Mic, Image, FileText, X } from 'lucide-react';
import { mediaApi } from '../../api/services/mediaApi';
import type { ConversationMedia } from '../../types/types';
import { AudioRecorder } from '../AudioRecorder';
import { CameraCapture } from '../CameraCapture';

interface MediaUploadProps {
  conversationId: string;
  onMediaUploaded: (media: ConversationMedia) => void;
  onError: (error: string) => void;
}

export const MediaUpload: React.FC<MediaUploadProps> = ({
  conversationId,
  onMediaUploaded,
  onError
}) => {
  const [isUploading, setIsUploading] = useState(false);
  const [showOptions, setShowOptions] = useState(false);
  const [showAudioRecorder, setShowAudioRecorder] = useState(false);
  const [showCameraCapture, setShowCameraCapture] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = async (files: FileList | null) => {
    if (!files || files.length === 0) return;

    const file = files[0];
    
    // Validar arquivo
    const validation = mediaApi.validateFile(file);
    if (!validation.valid) {
      onError(validation.error || 'Arquivo inválido');
      return;
    }

    setIsUploading(true);

    try {
      const mediaType = mediaApi.getMediaType(file);
      
      const uploadResponse = await mediaApi.upload({
        conversationId,
        file,
        mediaType
      });

      // Converter para ConversationMedia
      const media: ConversationMedia = {
        id: uploadResponse.id,
        conversationId,
        fileUrl: uploadResponse.fileUrl,
        mediaType: uploadResponse.mediaType as ConversationMedia['mediaType'],
        mimeType: uploadResponse.mimeType,
        originalFileName: uploadResponse.originalFileName,
        fileSizeBytes: uploadResponse.fileSizeBytes,
        uploadedAt: uploadResponse.uploadedAt
      };

      onMediaUploaded(media);
      setShowOptions(false);
    } catch (error) {
      console.error('Erro ao fazer upload:', error);
      
      // Tratamento específico para erro 403
      if (error instanceof Error && error.message.includes('403')) {
        onError('Acesso negado. Verifique suas permissões para enviar arquivos.');
      } else {
        onError(`Erro ao fazer upload: ${error instanceof Error ? error.message : 'Erro desconhecido'}`);
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleOptionClick = (type: 'file' | 'image' | 'camera' | 'audio') => {
    switch (type) {
      case 'file':
        fileInputRef.current?.click();
        break;
      case 'image':
        imageInputRef.current?.click();
        break;
      case 'camera':
        setShowCameraCapture(true);
        setShowOptions(false);
        break;
      case 'audio':
        setShowAudioRecorder(true);
        setShowOptions(false);
        break;
    }
  };

  const handleBlobUpload = async (blob: Blob, fileName: string, mediaType: 'AUDIO' | 'IMAGE') => {
    setIsUploading(true);
    
    try {
      const file = new File([blob], fileName, { type: blob.type });
      
      const uploadResponse = await mediaApi.upload({
        conversationId,
        file,
        mediaType
      });

      const media: ConversationMedia = {
        id: uploadResponse.id,
        conversationId,
        fileUrl: uploadResponse.fileUrl,
        mediaType: uploadResponse.mediaType as ConversationMedia['mediaType'],
        mimeType: uploadResponse.mimeType,
        originalFileName: uploadResponse.originalFileName,
        fileSizeBytes: uploadResponse.fileSizeBytes,
        uploadedAt: uploadResponse.uploadedAt
      };

      onMediaUploaded(media);
    } catch (error) {
      console.error('Erro ao fazer upload:', error);
      onError('Erro ao fazer upload do arquivo');
    } finally {
      setIsUploading(false);
    }
  };

  const handleAudioRecorded = (audioBlob: Blob) => {
    handleBlobUpload(audioBlob, `audio_${Date.now()}.webm`, 'AUDIO');
    setShowAudioRecorder(false);
  };

  const handlePhotoCapture = (photoBlob: Blob) => {
    handleBlobUpload(photoBlob, `photo_${Date.now()}.jpg`, 'IMAGE');
    setShowCameraCapture(false);
  };

  return (
    <div className="relative">
      <input
        ref={fileInputRef}
        type="file"
        onChange={(e) => handleFileSelect(e.target.files)}
        accept=".pdf,.doc,.docx,.txt"
        className="hidden"
      />
      
      <input
        ref={imageInputRef}
        type="file"
        onChange={(e) => handleFileSelect(e.target.files)}
        accept="image/*"
        className="hidden"
      />

      <button
        onClick={() => setShowOptions(!showOptions)}
        disabled={isUploading}
        className="text-gray-600 hover:text-gray-800 p-2 hover:bg-gray-100 rounded-lg transition-colors"
      >
        <Paperclip className="w-4 h-4" />
      </button>

      {showOptions && (
        <div className="absolute bottom-full left-0 mb-2 bg-white rounded-lg shadow-lg border border-gray-200 py-2 min-w-[200px] z-50">
          <div className="flex justify-between items-center px-3 pb-2 border-b border-gray-100">
            <span className="text-sm font-medium text-gray-700">Anexar arquivo</span>
            <button
              onClick={() => setShowOptions(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
          
          <div className="py-1">
            <button
              onClick={() => handleOptionClick('image')}
              className="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            >
              <Image className="w-4 h-4 text-green-500" />
              Imagem
            </button>
            
            <button
              onClick={() => handleOptionClick('file')}
              className="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            >
              <FileText className="w-4 h-4 text-blue-500" />
              Documento
            </button>
            
            <button
              onClick={() => handleOptionClick('camera')}
              className="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            >
              <Camera className="w-4 h-4 text-purple-500" />
              Câmera
            </button>
            
            <button
              onClick={() => handleOptionClick('audio')}
              className="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            >
              <Mic className="w-4 h-4 text-red-500" />
              Áudio
            </button>
          </div>
        </div>
      )}

      {showAudioRecorder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <AudioRecorder
            onRecordingComplete={handleAudioRecorded}
            onCancel={() => setShowAudioRecorder(false)}
          />
        </div>
      )}

      {showCameraCapture && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <CameraCapture
            onPhotoCapture={handlePhotoCapture}
            onCancel={() => setShowCameraCapture(false)}
          />
        </div>
      )}
    </div>
  );
};