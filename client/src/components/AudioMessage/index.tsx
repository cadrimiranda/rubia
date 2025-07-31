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
    <div className="flex items-center space-x-3 min-w-[280px] max-w-[350px]">
      <audio ref={audioRef} preload="metadata">
        <source src={audioUrl} type={mimeType} />
        Seu navegador não suporta áudio.
      </audio>

      {/* Play/Pause Button */}
      <button
        onClick={togglePlayPause}
        disabled={isLoading}
        className={`flex items-center justify-center w-12 h-12 rounded-full transition-all shadow-sm ${
          isFromCustomer
            ? "bg-white text-blue-600 hover:bg-gray-50"
            : "bg-green-500 hover:bg-green-600 text-white"
        } ${isLoading ? "opacity-50 cursor-not-allowed" : ""}`}
      >
        {isLoading ? (
          <div className="w-5 h-5 border-2 border-current border-t-transparent rounded-full animate-spin" />
        ) : isPlaying ? (
          <Pause size={20} />
        ) : (
          <Play size={20} className="ml-0.5" />
        )}
      </button>

      {/* Waveform and Time */}
      <div className="flex-1 space-y-1">
        {/* Waveform visualization */}
        <div className="flex items-center space-x-0.5 h-8">
          {Array.from({ length: 40 }).map((_, i) => {
            const height = Math.random() * 20 + 8; // Random heights between 8-28px
            const isActive = i < (progressPercentage / 100) * 40;
            return (
              <div
                key={i}
                className={`w-1 rounded-full transition-all duration-100 ${
                  isActive
                    ? isFromCustomer
                      ? "bg-white"
                      : "bg-green-600"
                    : isFromCustomer
                    ? "bg-blue-300"
                    : "bg-gray-300"
                }`}
                style={{ height: `${height}px` }}
              />
            );
          })}
        </div>

        {/* Time display */}
        <div className="flex justify-between">
          <span className={`text-xs font-mono ${
            isFromCustomer ? "text-blue-100" : "text-gray-600"
          }`}>
            {formatTime(currentTime)}
          </span>
          <span className={`text-xs font-mono ${
            isFromCustomer ? "text-blue-100" : "text-gray-600"
          }`}>
            {formatTime(audioDuration)}
          </span>
        </div>
      </div>
    </div>
  );
};