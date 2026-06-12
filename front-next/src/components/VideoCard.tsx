"use client";

import React, { useState, useRef } from "react";
import { useRouter } from "next/navigation";
import VideoModal from "./VideoModal"; // 모달 컴포넌트 임포트
import Image from "next/image";

interface VideoCardProps {
  song: {
    videoId: string;
    title: string;
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
  const [isModalOpen, setIsModalOpen] = useState(false); // 모달 상태 추가
  const enterTimeoutRef = useRef<number | null>(null);
  const leaveTimeoutRef = useRef<number | null>(null);
  const thumbnailUrl = `https://img.youtube.com/vi/${song.videoId}/0.jpg`;

  const handleMouseEnter = () => {
    if (leaveTimeoutRef.current) clearTimeout(leaveTimeoutRef.current);
    enterTimeoutRef.current = window.setTimeout(() => setHovered(true), 250);
  };

  const handleMouseLeave = () => {
    if (enterTimeoutRef.current) clearTimeout(enterTimeoutRef.current);
    leaveTimeoutRef.current = window.setTimeout(() => setHovered(false), 250);
  };

  const handleCardClick = () => {
    setIsModalOpen(true); // 카드 클릭 시 모달 열기
  };

  const handleCloseModal = () => {
    setIsModalOpen(false); // 모달 닫기
  };

  const handleVtuberNameClick = (
    event: React.MouseEvent<HTMLParagraphElement>
  ) => {
    event.stopPropagation(); // 카드 클릭 이벤트 방지
    router.push(`/search?query=${encodeURIComponent(song.vtuberName)}`);
  };

  return (
    <>
      <div
        className="group bg-[#222831] border border-[#393E46] rounded-lg overflow-hidden text-center cursor-pointer relative h-64 transition-transform duration-200 flex justify-center items-center hover:scale-105"
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        onClick={handleCardClick} // 카드 클릭 시 모달 열기
      >
        {!hovered ? (
          <div className="absolute inset-0 w-full h-full flex flex-col justify-center items-center bg-[#393E46] transition-opacity duration-300 text-[#EEEEEE] group-hover:opacity-0">
            <Image
              src={thumbnailUrl}
              alt={song.title}
              className="thumbnail"
              width={200}
              height={150}
              priority={isPriority}
            />
            <h3 style={{ cursor: "pointer" }}>{song.title}</h3>
          </div>
        ) : (
          <div className="absolute inset-0 w-full h-full flex flex-col justify-center items-center bg-[#00ADB5] text-[#222831] p-2.5 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
            <h3 className="cursor-pointer font-bold text-lg mb-1">{song.title}</h3>
            <p
              className="text-blue-600 cursor-pointer hover:underline"
              onClick={handleVtuberNameClick} // 채널명 클릭 이벤트
            >
              채널명: {song.vtuberName}
            </p>
            <p className="opacity-80">조회수: {song.viewCount.toLocaleString()}</p>
            <p className="opacity-80">게시일: {new Date(song.publishedAt).toLocaleDateString()}</p>
          </div>
        )}
      </div>
      {isModalOpen && (
        <VideoModal videoId={song.videoId} onClose={handleCloseModal} /> // 모달 열기
      )}
    </>
  );
};

export default VideoCard;