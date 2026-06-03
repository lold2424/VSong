"use client";

import React, { useState } from "react";
import VideoModal from "./VideoModal";


interface Song {
  id: string;
  title: string;
  vtuberName: string;
  videoId: string;
  parsedTitle?: string;
  songType?: string;
}

interface WeeklyChartProps {
  top10WeeklySongs: Song[];
  top10DailySongs: Song[];
}

const WeeklyChart: React.FC<WeeklyChartProps> = ({
  top10WeeklySongs,
  top10DailySongs,
}) => {
  const [selectedVideoId, setSelectedVideoId] = useState<string | null>(null);
  const [chartType, setChartType] = useState<"weekly" | "daily">(
    "weekly"
  );

  const handleSongClick = (videoId: string) => {
    setSelectedVideoId(videoId);
  };

  const handleCloseModal = () => {
    setSelectedVideoId(null);
  };

  const getDisplayTitle = (song: Song) => {
    if (!song.parsedTitle) return song.title;

    if (song.songType === "ORIGINAL") {
      return `${song.parsedTitle} (Original. ${song.vtuberName})`;
    } else {
      return `${song.parsedTitle} (Cover. ${song.vtuberName})`;
    }
  };

  const getChartData = () => {
    switch (chartType) {
      case "weekly":
        return { data: top10WeeklySongs, title: "주간 인기 노래" };
      case "daily":
        return { data: top10DailySongs, title: "일간 인기 노래" };
      default:
        return { data: [], title: "" };
    }
  };

  const { data, title } = getChartData();

  return (
    <>
      <nav className="flex justify-center gap-2.5 mb-2.5" aria-label="차트 유형 선택">
        <button
          onClick={() => setChartType("weekly")}
          className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none focus-visible:ring-2 focus-visible:ring-[#A6E22E] ${chartType === "weekly" ? "text-[#272222] bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-[#272222] hover:border-[#A6E22E]`}
        >
          주간
        </button>
        <button
          onClick={() => setChartType("daily")}
          className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none focus-visible:ring-2 focus-visible:ring-[#A6E22E] ${chartType === "daily" ? "text-[#272222] bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-[#272222] hover:border-[#A6E22E]`}
        >
          일간
        </button>
      </nav>
      <section className="bg-[#272822] text-[#F8F8F2] rounded-lg p-5 shadow-lg border border-[#3E3D32]" aria-labelledby="chart-title">
        <h3 id="chart-title" className="text-[#A6E22E] text-xl mb-3.5 font-bold">{title}</h3>
        <ul className="list-none p-0 m-0 space-y-3">
          {data.length > 0 ? (
            data.map((song, index) => {
              const displayTitle = getDisplayTitle(song);
              return (
                <li key={song.id}>
                  <button
                    className="w-full flex justify-between items-start p-3 bg-[#3E3D32] rounded-lg text-[#F8F8F2] transition-colors duration-200 hover:bg-[#A6E22E] hover:text-[#272222] focus-visible:ring-2 focus-visible:ring-[#A6E22E] outline-none text-left group"
                    onClick={() => handleSongClick(song.videoId)}
                    aria-label={`${index + 1}위, ${displayTitle} 영상 보기`}
                  >
                    <h4 className="m-0 text-base font-bold text-inherit leading-normal break-words">
                      {index + 1}. {displayTitle}
                    </h4>
                  </button>
                </li>
              );
            })
          ) : (
            <p className="text-gray-400 italic">차트 데이터가 없습니다.</p>
          )}
        </ul>
        <div className="mt-2.5 text-[10px] text-[#F8F8F2] opacity-60 italic text-right">
          갱신: 매일 00:01 (KST) | {chartType === "weekly" ? "최근 7일 상승량" : "어제 대비 상승량"}
        </div>

        {selectedVideoId && (
          <VideoModal videoId={selectedVideoId} onClose={handleCloseModal} />
        )}
      </section>
    </>
  );
};

export default WeeklyChart;
