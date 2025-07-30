import React, { useState, useRef, useEffect } from "react";
import { Play, Pause, Volume2 } from "lucide-react";

interface AudioMessageProps {
  audioUrl: string;
  duration?: number;
  isFromCustomer: boolean;
  mimeType?: string;
}

export const AudioMessage: React.FC<AudioMessageProps> = ({
  audioUrl,
  duration,
  isFromCustomer,
  mimeType = "audio/ogg"
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [audioDuration, setAudioDuration] = useState(duration || 0);
  const [isLoading, setIsLoading] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const handleTimeUpdate = () => {
      setCurrentTime(audio.currentTime);
    };

    const handleDurationChange = () => {
      setAudioDuration(audio.duration);
    };

    const handleEnded = () => {
      setIsPlaying(false);
      setCurrentTime(0);
    };

    const handleLoadStart = () => {
      setIsLoading(true);
    };

    const handleCanPlay = () => {
      setIsLoading(false);
    };

    const handleError = () => {
      setIsLoading(false);
      console.error("Error loading audio");
    };

    audio.addEventListener("timeupdate", handleTimeUpdate);
    audio.addEventListener("durationchange", handleDurationChange);
    audio.addEventListener("ended", handleEnded);
    audio.addEventListener("loadstart", handleLoadStart);
    audio.addEventListener("canplay", handleCanPlay);
    audio.addEventListener("error", handleError);

    return () => {
      audio.removeEventListener("timeupdate", handleTimeUpdate);
      audio.removeEventListener("durationchange", handleDurationChange);
      audio.removeEventListener("ended", handleEnded);
      audio.removeEventListener("loadstart", handleLoadStart);
      audio.removeEventListener("canplay", handleCanPlay);
      audio.removeEventListener("error", handleError);
    };
  }, []);

  const togglePlayPause = async () => {
    const audio = audioRef.current;
    if (!audio) return;

    try {
      if (isPlaying) {
        audio.pause();
        setIsPlaying(false);
      } else {
        await audio.play();
        setIsPlaying(true);
      }
    } catch (error) {
      console.error("Error playing audio:", error);
    }
  };

  const formatTime = (time: number) => {
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds.toString().padStart(2, "0")}`;
  };

  const progressPercentage = audioDuration > 0 ? (currentTime / audioDuration) * 100 : 0;

  return (
    <div
      className={`flex items-center space-x-3 p-3 rounded-lg min-w-[200px] ${
        isFromCustomer
          ? "bg-blue-600 text-white"
          : "bg-gray-100 text-gray-800"
      }`}
    >
      <audio ref={audioRef} preload="metadata">
        <source src={audioUrl} type={mimeType} />
        Seu navegador não suporta áudio.
      </audio>

      <button
        onClick={togglePlayPause}
        disabled={isLoading}
        className={`flex items-center justify-center w-10 h-10 rounded-full transition-all ${
          isFromCustomer
            ? "bg-blue-500 hover:bg-blue-400 text-white"
            : "bg-gray-200 hover:bg-gray-300 text-gray-600"
        } ${isLoading ? "opacity-50 cursor-not-allowed" : ""}`}
      >
        {isLoading ? (
          <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
        ) : isPlaying ? (
          <Pause size={16} />
        ) : (
          <Play size={16} className="ml-0.5" />
        )}
      </button>

      <div className="flex-1">
        <div className="flex items-center space-x-2 mb-1">
          <Volume2 size={14} className={isFromCustomer ? "text-blue-200" : "text-gray-500"} />
          <span className={`text-xs ${isFromCustomer ? "text-blue-200" : "text-gray-500"}`}>
            Áudio
          </span>
        </div>

        <div className="flex items-center space-x-2">
          <div className={`flex-1 h-1 rounded-full ${
            isFromCustomer ? "bg-blue-400" : "bg-gray-300"
          }`}>
            <div
              className={`h-full rounded-full transition-all duration-100 ${
                isFromCustomer ? "bg-white" : "bg-blue-500"
              }`}
              style={{ width: `${progressPercentage}%` }}
            />
          </div>

          <span className={`text-xs font-mono ${
            isFromCustomer ? "text-blue-100" : "text-gray-600"
          }`}>
            {formatTime(currentTime)} / {formatTime(audioDuration)}
          </span>
        </div>
      </div>
    </div>
  );
};