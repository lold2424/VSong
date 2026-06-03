"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import VideoModal from "@/components/VideoModal";
import Image from "next/image";

interface VtuberDetail {
  name: string;
  subscribers: number;
  gender: string | null;
  songCount: number;
  channelImg: string;
}

interface Song {
  id: number;
  videoId: string;
  title: string;
  publishedAt: string;
  viewCount: number;
}

interface VtuberDetailClientProps {
  vtuberDetail: VtuberDetail;
  songs: Song[];
  channelId: string;
}

const VtuberDetailClient: React.FC<VtuberDetailClientProps> = ({
  vtuberDetail,
  songs,
  channelId,
}) => {
  const router = useRouter();
  const [selectedVideoId, setSelectedVideoId] = useState<string | null>(null);

  const handleOpenModal = (videoId: string) => {
    setSelectedVideoId(videoId);
  };

  const handleCloseModal = () => {
    setSelectedVideoId(null);
  };

  const genderText =
    vtuberDetail.gender === "female"
      ? "여성"
      : vtuberDetail.gender === "male"
      ? "남성"
      : "혼성";

  return (
    <div className="flex flex-col justify-center items-center m-5 bg-[#272822] text-[#F8F8F2]">
      {/* 버튜버 프로필 섹션 */}
      <section 
        aria-labelledby="vtuber-profile-name"
        className="flex flex-row items-center bg-[#3E3D32] text-[#F8F8F2] rounded-lg shadow-xl p-5 max-w-2xl w-full mb-8"
      >
        <div className="flex-shrink-0 mr-5">
          <Image
            src={vtuberDetail.channelImg}
            alt={`${vtuberDetail.name} 프로필 이미지`}
            className="w-24 h-24 rounded-full object-cover border-2 border-[#66D9EF]"
            width={100}
            height={100}
          />
        </div>
        <div className="flex-1">
          <h1 id="vtuber-profile-name" className="text-2xl font-bold mb-2.5">{vtuberDetail.name}</h1>
          <p className="text-base mb-2">
            구독자 수: <span className="font-semibold">{vtuberDetail.subscribers.toLocaleString()}</span>
          </p>
          <p className="text-base mb-2">성별: {genderText}</p>
          <p className="text-base mb-2">
            등록된 노래 수: {vtuberDetail.songCount}
          </p>
          <div className="flex flex-wrap gap-2.5 mt-2.5">
            <button
              onClick={() =>
                window.open(
                  `https://www.youtube.com/channel/${channelId}`,
                  "_blank"
                )
              }
              className="px-3.5 py-2.5 text-sm border-none rounded cursor-pointer text-white bg-red-600 transition-colors duration-200 hover:bg-red-700 focus-visible:ring-2 focus-visible:ring-white outline-none"
            >
              유튜브 채널 방문
            </button>
            <button
              onClick={() => router.back()}
              className="px-3.5 py-2.5 text-sm border-none rounded cursor-pointer text-[#F8F8F2] bg-[#272822] transition-colors duration-200 hover:bg-[#3E3D32] focus-visible:ring-2 focus-visible:ring-[#A6E22E] outline-none"
            >
              목록으로 돌아가기
            </button>
          </div>
        </div>
      </section>

      {/* 등록된 노래 목록 섹션 */}
      <section 
        aria-labelledby="songs-list-title"
        className="w-full max-w-2xl p-5 bg-[#3E3D32] rounded-lg shadow-lg text-[#F8F8F2]"
      >
        <h2 id="songs-list-title" className="text-xl mb-5 text-[#A6E22E] font-bold">등록된 노래</h2>
        {songs.length > 0 ? (
          <ul className="list-none p-0 m-0 space-y-5">
            {songs.map((song) => (
              <li key={song.id}>
                <article 
                  aria-labelledby={`song-title-${song.id}`}
                  className="flex items-center p-2.5 bg-[#272822] border border-[#3E3D32] rounded-lg shadow-md transition-transform duration-200 hover:scale-[1.01]"
                >
                  <div className="flex-shrink-0 mr-5 relative w-32 h-24">
                    <Image
                      src={`https://img.youtube.com/vi/${song.videoId}/0.jpg`}
                      alt={`${song.title} 썸네일`}
                      className="object-cover rounded"
                      fill
                      sizes="128px"
                    />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 id={`song-title-${song.id}`} className="m-0 text-base font-bold text-[#F8F8F2] truncate">
                      {song.title}
                    </h3>
                    <div className="text-sm text-gray-400 mt-1 space-y-0.5">
                      <p>조회수: {song.viewCount.toLocaleString()}회</p>
                      <p>
                        게시일: {new Date(song.publishedAt).toLocaleDateString()}
                      </p>
                    </div>
                    <button
                      onClick={() => handleOpenModal(song.videoId)}
                      aria-label={`${song.title} 노래 듣기`}
                      className="mt-2.5 bg-[#A6E22E] text-[#272822] border-none rounded px-3 py-1.5 text-sm font-bold cursor-pointer transition-colors hover:bg-[#F8F8F2] focus-visible:ring-2 focus-visible:ring-white outline-none"
                    >
                      노래 재생
                    </button>
                  </div>
                </article>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-center py-10 text-gray-400">등록된 노래가 없습니다.</p>
        )}
      </section>
      {selectedVideoId && (
        <VideoModal videoId={selectedVideoId} onClose={handleCloseModal} />
      )}
    </div>
  );
};

export default VtuberDetailClient;
