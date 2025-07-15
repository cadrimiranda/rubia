import React, { useState, useRef, useEffect } from 'react';
import { Camera, RotateCcw, X, Download, Send } from 'lucide-react';

interface CameraCaptureProps {
  onPhotoCapture: (photoBlob: Blob) => void;
  onCancel: () => void;
}

export const CameraCapture: React.FC<CameraCaptureProps> = ({
  onPhotoCapture,
  onCancel
}) => {
  const [isStreaming, setIsStreaming] = useState(false);
  const [capturedPhoto, setCapturedPhoto] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('user');

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  useEffect(() => {
    startCamera();
    
    return () => {
      stopCamera();
    };
  }, [facingMode]); // eslint-disable-line react-hooks/exhaustive-deps

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { 
          facingMode: facingMode,
          width: { ideal: 1280 },
          height: { ideal: 720 }
        }
      });

      streamRef.current = stream;
      
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
        setIsStreaming(true);
        setError(null);
      }
    } catch (err) {
      console.error('Erro ao acessar câmera:', err);
      setError('Erro ao acessar câmera. Verifique as permissões.');
      setIsStreaming(false);
    }
  };

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    setIsStreaming(false);
  };

  const capturePhoto = () => {
    if (videoRef.current && canvasRef.current) {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      const context = canvas.getContext('2d');

      if (context) {
        // Definir dimensões do canvas baseadas no vídeo
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        // Desenhar frame atual do vídeo no canvas
        context.drawImage(video, 0, 0, canvas.width, canvas.height);

        // Converter para imagem
        const imageData = canvas.toDataURL('image/jpeg', 0.8);
        setCapturedPhoto(imageData);
        
        // Parar câmera após captura
        stopCamera();
      }
    }
  };

  const switchCamera = () => {
    setFacingMode(prev => prev === 'user' ? 'environment' : 'user');
  };

  const retakePhoto = () => {
    setCapturedPhoto(null);
    startCamera();
  };

  const handleSend = () => {
    if (capturedPhoto && canvasRef.current) {
      canvasRef.current.toBlob((blob) => {
        if (blob) {
          onPhotoCapture(blob);
        }
      }, 'image/jpeg', 0.8);
    }
  };

  const downloadPhoto = () => {
    if (capturedPhoto) {
      const link = document.createElement('a');
      link.download = `foto_${Date.now()}.jpg`;
      link.href = capturedPhoto;
      link.click();
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-lg border overflow-hidden min-w-[400px]">
      <div className="flex items-center justify-between p-4 border-b">
        <h3 className="text-lg font-medium text-gray-800">
          {capturedPhoto ? 'Foto Capturada' : 'Capturar Foto'}
        </h3>
        <button
          onClick={onCancel}
          className="text-gray-400 hover:text-gray-600"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-b border-red-200 text-red-600 text-sm">
          {error}
        </div>
      )}

      <div className="relative bg-black">
        {!capturedPhoto ? (
          <>
            <video
              ref={videoRef}
              autoPlay
              muted
              playsInline
              className="w-full h-64 object-cover"
            />
            
            {isStreaming && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="w-48 h-48 border-2 border-white rounded-lg opacity-50"></div>
              </div>
            )}
          </>
        ) : (
          <img
            src={capturedPhoto}
            alt="Foto capturada"
            className="w-full h-64 object-cover"
          />
        )}

        <canvas
          ref={canvasRef}
          className="hidden"
        />
      </div>

      <div className="p-4">
        {!capturedPhoto && isStreaming && (
          <div className="flex justify-center gap-3 mb-4">
            <button
              onClick={switchCamera}
              className="flex items-center gap-2 px-3 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              Trocar
            </button>

            <button
              onClick={capturePhoto}
              className="flex items-center gap-2 px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
            >
              <Camera className="w-4 h-4" />
              Capturar
            </button>
          </div>
        )}

        {capturedPhoto && (
          <div className="flex justify-center gap-2">
            <button
              onClick={retakePhoto}
              className="flex items-center gap-2 px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              Refazer
            </button>

            <button
              onClick={downloadPhoto}
              className="flex items-center gap-2 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
            >
              <Download className="w-4 h-4" />
              Baixar
            </button>

            <button
              onClick={handleSend}
              className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
            >
              <Send className="w-4 h-4" />
              Enviar
            </button>
          </div>
        )}

        {!isStreaming && !capturedPhoto && !error && (
          <div className="text-center">
            <button
              onClick={startCamera}
              className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors mx-auto"
            >
              <Camera className="w-4 h-4" />
              Iniciar Câmera
            </button>
          </div>
        )}
      </div>

      <div className="px-4 pb-4">
        <div className="text-xs text-gray-500 text-center">
          {!capturedPhoto && isStreaming ? 'Posicione-se no quadro e clique em "Capturar"' : 
           capturedPhoto ? 'Foto capturada com sucesso' : 
           'Clique em "Iniciar Câmera" para começar'}
        </div>
      </div>
    </div>
  );
};