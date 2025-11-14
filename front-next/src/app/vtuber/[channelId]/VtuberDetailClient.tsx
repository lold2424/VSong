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
      <div className="flex flex-row items-center bg-[#3E3D32] text-[#F8F8F2] rounded-lg shadow-xl p-5 max-w-2xl w-full mb-7.5">
        <div className="flex-shrink-0 mr-5">
          <Image
            src={vtuberDetail.channelImg}
            alt={vtuberDetail.name}
            className="w-25 h-25 rounded-full object-cover border-2 border-[#66D9EF]"
            width={100}
            height={100}
          />
        </div>
        <div className="flex-1">
          <h1 className="text-2xl font-bold mb-2.5">{vtuberDetail.name}</h1>
          <p className="text-base mb-2">
            구독자 수: {vtuberDetail.subscribers.toLocaleString()}
          </p>
          <p className="text-base mb-2">성별: {genderText}</p>
          <p className="text-base mb-2">
            등록된 노래 수: {vtuberDetail.songCount}
          </p>
          <button
            onClick={() =>
              window.open(
                `https://www.youtube.com/channel/${channelId}`,
                "_blank"
              )
            }
            className="mt-2.5 px-3.5 py-2.5 text-sm border-none rounded cursor-pointer text-white bg-red-600 mr-3.5 transition-colors duration-200 hover:bg-red-700"
          >
            유튜브로 이동
          </button>
          <button
            onClick={() => router.back()}
            className="mt-2.5 px-3.5 py-2.5 text-sm border-none rounded cursor-pointer text-[#F8F8F2] bg-[#272822] transition-colors duration-200 hover:bg-[#3E3D32]"
          >
            뒤로 가기
          </button>
        </div>
      </div>
      <div className="w-full max-w-2xl p-5 bg-[#3E3D32] rounded-lg shadow-lg text-[#F8F8F2]">
        <h2 className="text-xl mb-5 text-[#A6E22E]">등록된 노래</h2>
        {songs.length > 0 ? (
          <ul className="list-none p-0 m-0">
            {songs.map((song) => (
              <li
                key={song.id}
                className="flex items-center mb-5 p-2.5 bg-[#272822] border border-[#3E3D32] rounded-lg shadow-md"
              >
                <Image
                  src={`https://img.youtube.com/vi/${song.videoId}/0.jpg`}
                  alt={song.title}
                  className="w-30 h-22.5 object-cover mr-5 rounded"
                  width={120}
                  height={90}
                />
                <div className="flex-1">
                  <h3 className="m-0 text-base font-bold text-[#F8F8F2]">
                    {song.title}
                  </h3>
                  <p>조회수: {song.viewCount.toLocaleString()}</p>
                  <p>
                    게시일: {new Date(song.publishedAt).toLocaleDateString()}
                  </p>
                  <button
                    onClick={() => handleOpenModal(song.videoId)}
                    className="bg-[#A6E22E] text-[#272822] border-none rounded px-2.5 py-1 cursor-pointer hover:bg-[#F8F8F2] hover:text-[#272822]"
                  >
                    노래 보기
                  </button>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p>등록된 노래가 없습니다.</p>
        )}
      </div>
      {selectedVideoId && (
        <VideoModal videoId={selectedVideoId} onClose={handleCloseModal} />
      )}
    </div>
  );
};

export default VtuberDetailClient;
