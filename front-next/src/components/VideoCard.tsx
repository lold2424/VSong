"use client";

import React, { useState, useRef } from "react";
import { useRouter } from "next/navigation";
import VideoModal from "./VideoModal";
import Image from "next/image";

interface VideoCardProps {
  song: {
    videoId: string;
    title: string;
    parsedTitle?: string;
    songType?: string;
    vtuberName: string;
    publishedAt: string;
    viewCount: number;
    channelId: string;
  };
  isPriority?: boolean;
}

const VideoCard: React.FC<VideoCardProps> = ({ song, isPriority = false }) => {
  const router = useRouter();
  const [hovered, setHovered] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const enterTimeoutRef = useRef<number | null>(null);
  const leaveTimeoutRef = useRef<number | null>(null);
  const thumbnailUrl = `https://img.youtube.com/vi/${song.videoId}/0.jpg`;

  // 화면에 표시할 제목 정제 로직
  const getDisplayTitle = () => {
    if (!song.parsedTitle) return song.title;

    if (song.songType === "ORIGINAL") {
      return `${song.parsedTitle} (Original. ${song.vtuberName})`;
    } else {
      return `${song.parsedTitle} (Cover. ${song.vtuberName})`;
    }
  };

  const displayTitle = getDisplayTitle();

  const handleMouseEnter = () => {
    if (leaveTimeoutRef.current) clearTimeout(leaveTimeoutRef.current);
    enterTimeoutRef.current = window.setTimeout(() => setHovered(true), 250);
  };

  const handleMouseLeave = () => {
    if (enterTimeoutRef.current) clearTimeout(enterTimeoutRef.current);
    leaveTimeoutRef.current = window.setTimeout(() => setHovered(false), 250);
  };

  const handleCardClick = () => {
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleCardClick();
    }
  };

  const handleVtuberNameClick = (
    event: React.MouseEvent<HTMLParagraphElement> | React.KeyboardEvent<HTMLParagraphElement>
  ) => {
    event.stopPropagation();
    router.push(`/search?query=${encodeURIComponent(song.vtuberName)}`);
  };

  return (
    <>
      <div
        className="group bg-[#222831] border border-[#393E46] rounded-lg overflow-hidden text-center cursor-pointer relative h-64 transition-transform duration-200 flex justify-center items-center hover:scale-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A6E22E] focus-visible:z-10"
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        onClick={handleCardClick}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        role="button"
        aria-label={`${displayTitle} 영상 보기`}
      >
        {!hovered ? (
          <div className="absolute inset-0 w-full h-full flex flex-col justify-center items-center bg-[#393E46] transition-opacity duration-300 text-[#EEEEEE] group-hover:opacity-0">
            <Image
              src={thumbnailUrl}
              alt={displayTitle}
              className="thumbnail"
              width={200}
              height={150}
              priority={isPriority}
            />
            <h3 style={{ cursor: "pointer" }}>{displayTitle}</h3>
          </div>
        ) : (
          <div className="absolute inset-0 w-full h-full flex flex-col justify-center items-center bg-[#00ADB5] text-[#222831] p-2.5 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
            <h3 className="cursor-pointer font-bold text-lg mb-1">{displayTitle}</h3>
            <p
              className="text-blue-900 font-bold cursor-pointer hover:underline"
              onClick={handleVtuberNameClick}
            >
              채널명: {song.vtuberName}
            </p>
            <p className="opacity-90 font-medium">조회수: {song.viewCount.toLocaleString()}</p>
            <p className="opacity-90 font-medium">게시일: {new Date(song.publishedAt).toLocaleDateString()}</p>
          </div>
        )}
      </div>
      {isModalOpen && (
        <VideoModal videoId={song.videoId} onClose={handleCloseModal} />
      )}
    </>
  );
};

export default VideoCard;