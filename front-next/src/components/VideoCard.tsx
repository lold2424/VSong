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
      <article
        className="group bg-[#222831] border border-[#393E46] rounded-lg overflow-hidden text-center cursor-pointer relative h-64 transition-transform duration-200 flex justify-center items-center hover:scale-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A6E22E] focus-visible:z-10"
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        onClick={handleCardClick}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        role="button"
        aria-haspopup="dialog"
        aria-label={`${displayTitle} - ${song.vtuberName}의 노래 듣기`}
      >
        {!hovered ? (
          <div className="absolute inset-0 w-full h-full flex flex-col justify-center items-center bg-[#393E46] transition-opacity duration-300 text-[#EEEEEE] group-hover:opacity-0 group-focus:opacity-0">
            <Image
              src={thumbnailUrl}
              alt=""
              className="thumbnail"
              width={200}
              height={150}
              priority={isPriority}
              aria-hidden="true"
            />
            <h3 className="px-2 text-sm font-bold line-clamp-2">{displayTitle}</h3>
          </div>
        ) : (
          <div className="absolute inset-0 w-full h-full flex flex-col justify-center items-center bg-[#00ADB5] text-[#222831] p-2.5 opacity-0 transition-opacity duration-300 group-hover:opacity-100 group-focus:opacity-100">
            <h3 className="font-bold text-lg mb-1 leading-tight">{displayTitle}</h3>
            <p
              className="text-blue-900 font-bold cursor-pointer hover:underline mb-1"
              onClick={handleVtuberNameClick}
              role="link"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.stopPropagation();
                  handleVtuberNameClick(e as any);
                }
              }}
            >
              채널명: {song.vtuberName}
            </p>
            <div className="text-sm font-medium opacity-90">
              <p>조회수: {song.viewCount.toLocaleString()}회</p>
              <p>게시일: {new Date(song.publishedAt).toLocaleDateString()}</p>
            </div>
            <span className="mt-3 inline-block bg-[#222831] text-[#A6E22E] px-3 py-1 rounded text-xs font-bold">
              클릭하여 재생
            </span>
          </div>
        )}
      </article>
      {isModalOpen && (
        <VideoModal videoId={song.videoId} onClose={handleCloseModal} />
      )}
    </>
  );
};

export default VideoCard;