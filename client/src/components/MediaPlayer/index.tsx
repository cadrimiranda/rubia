import React, { useState, useRef, useEffect } from "react";
import {
  Play,
  Pause,
  Volume2,
  VolumeX,
  SkipBack,
  SkipForward,
  Download,
} from "lucide-react";
import type { ConversationMedia } from "../../types/types";

interface MediaPlayerProps {
  media: ConversationMedia;
  variant?: "inline" | "modal";
  autoPlay?: boolean;
}

export const MediaPlayer: React.FC<MediaPlayerProps> = ({
  media,
  variant = "inline",
  autoPlay = false,
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);

  const isAudio = media.mediaType === "AUDIO";
  const isVideo = media.mediaType === "VIDEO";

  useEffect(() => {
    const mediaElement = isAudio ? audioRef.current : videoRef.current;

    if (mediaElement) {
      const handleLoadedMetadata = () => {
        setDuration(mediaElement.duration);
        setIsLoaded(true);
      };

      const handleTimeUpdate = () => {
        setCurrentTime(mediaElement.currentTime);
      };

      const handleEnded = () => {
        setIsPlaying(false);
        setCurrentTime(0);
      };

      const handleError = () => {
        setError("Erro ao carregar mídia");
        setIsLoaded(false);
      };

      mediaElement.addEventListener("loadedmetadata", handleLoadedMetadata);
      mediaElement.addEventListener("timeupdate", handleTimeUpdate);
      mediaElement.addEventListener("ended", handleEnded);
      mediaElement.addEventListener("error", handleError);

      return () => {
        mediaElement.removeEventListener(
          "loadedmetadata",
          handleLoadedMetadata
        );
        mediaElement.removeEventListener("timeupdate", handleTimeUpdate);
        mediaElement.removeEventListener("ended", handleEnded);
        mediaElement.removeEventListener("error", handleError);
      };
    }
  }, [isAudio]);

  const togglePlayPause = () => {
    const mediaElement = isAudio ? audioRef.current : videoRef.current;

    if (mediaElement) {
      if (isPlaying) {
        mediaElement.pause();
      } else {
        mediaElement.play();
      }
      setIsPlaying(!isPlaying);
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const mediaElement = isAudio ? audioRef.current : videoRef.current;
    const newTime = Number(e.target.value);

    if (mediaElement) {
      mediaElement.currentTime = newTime;
      setCurrentTime(newTime);
    }
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const mediaElement = isAudio ? audioRef.current : videoRef.current;
    const newVolume = Number(e.target.value);

    if (mediaElement) {
      mediaElement.volume = newVolume;
      setVolume(newVolume);
      setIsMuted(newVolume === 0);
    }
  };

  const toggleMute = () => {
    const mediaElement = isAudio ? audioRef.current : videoRef.current;

    if (mediaElement) {
      if (isMuted) {
        mediaElement.volume = volume;
        setIsMuted(false);
      } else {
        mediaElement.volume = 0;
        setIsMuted(true);
      }
    }
  };

  const skipSeconds = (seconds: number) => {
    const mediaElement = isAudio ? audioRef.current : videoRef.current;

    if (mediaElement) {
      const newTime = Math.max(0, Math.min(duration, currentTime + seconds));
      mediaElement.currentTime = newTime;
      setCurrentTime(newTime);
    }
  };

  const formatTime = (time: number): string => {
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds.toString().padStart(2, "0")}`;
  };

  const handleDownload = () => {
    const link = document.createElement("a");
    link.href = media.fileUrl;
    link.download = media.originalFileName;
    link.click();
  };

  if (error) {
    return (
      <div className="flex items-center justify-center p-4 bg-red-50 border border-red-200 rounded-lg">
        <p className="text-red-600 text-sm">{error}</p>
      </div>
    );
  }

  if (isVideo) {
    return (
      <div
        className={`${
          variant === "modal" ? "max-w-2xl" : "max-w-xs"
        } bg-black rounded-lg overflow-hidden`}
      >
        <video
          ref={videoRef}
          src={media.fileUrl}
          className="w-full"
          controls={variant === "modal"}
          autoPlay={autoPlay}
          onPlay={() => setIsPlaying(true)}
          onPause={() => setIsPlaying(false)}
        />

        {variant === "inline" && (
          <div className="p-2 bg-gray-800 text-white">
            <div className="flex items-center gap-2 text-xs">
              <button
                onClick={togglePlayPause}
                className="text-white hover:text-gray-300"
              >
                {isPlaying ? (
                  <Pause className="w-4 h-4" />
                ) : (
                  <Play className="w-4 h-4" />
                )}
              </button>
              <span className="text-gray-300">
                {formatTime(currentTime)} / {formatTime(duration)}
              </span>
              <button
                onClick={handleDownload}
                className="ml-auto text-white hover:text-gray-300"
              >
                <Download className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  if (isAudio) {
    return (
      <div
        className={`${
          variant === "modal" ? "max-w-md" : "max-w-xs"
        } bg-gray-100 rounded-lg p-3`}
      >
        <audio
          ref={audioRef}
          src={media.fileUrl}
          autoPlay={autoPlay}
          onPlay={() => setIsPlaying(true)}
          onPause={() => setIsPlaying(false)}
        />

        <div className="flex items-center gap-2 mb-2">
          <button
            onClick={togglePlayPause}
            disabled={!isLoaded}
            className="flex items-center justify-center w-8 h-8 bg-blue-500 text-white rounded-xl hover:bg-blue-600 disabled:bg-gray-300"
          >
            {isPlaying ? (
              <Pause className="w-4 h-4" />
            ) : (
              <Play className="w-4 h-4" />
            )}
          </button>

          <div className="flex-1">
            <p className="text-sm font-medium text-gray-800 truncate">
              {media.originalFileName}
            </p>
            <p className="text-xs text-gray-500">
              {formatTime(currentTime)} / {formatTime(duration)}
            </p>
          </div>

          <button
            onClick={handleDownload}
            className="text-gray-500 hover:text-gray-700"
          >
            <Download className="w-4 h-4" />
          </button>
        </div>

        <div className="mb-2">
          <input
            type="range"
            min="0"
            max={duration}
            value={currentTime}
            onChange={handleSeek}
            disabled={!isLoaded}
            className="w-full h-1 bg-gray-300 rounded-lg appearance-none cursor-pointer"
          />
        </div>

        {variant === "modal" && (
          <div className="flex items-center gap-2">
            <button
              onClick={() => skipSeconds(-10)}
              className="text-gray-500 hover:text-gray-700"
            >
              <SkipBack className="w-4 h-4" />
            </button>

            <button
              onClick={toggleMute}
              className="text-gray-500 hover:text-gray-700"
            >
              {isMuted ? (
                <VolumeX className="w-4 h-4" />
              ) : (
                <Volume2 className="w-4 h-4" />
              )}
            </button>

            <input
              type="range"
              min="0"
              max="1"
              step="0.1"
              value={isMuted ? 0 : volume}
              onChange={handleVolumeChange}
              className="w-16 h-1 bg-gray-300 rounded-lg appearance-none cursor-pointer"
            />

            <button
              onClick={() => skipSeconds(10)}
              className="text-gray-500 hover:text-gray-700"
            >
              <SkipForward className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    );
  }

  return null;
};
