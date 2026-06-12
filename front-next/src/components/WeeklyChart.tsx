"use client";

import React, { useState } from "react";
import VideoModal from "./VideoModal";


interface Song {
  id: string;
  title: string;
  artist: string;
  videoId: string;
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
      <div className="flex justify-center gap-2.5 mb-2.5">
        <button
          onClick={() => setChartType("weekly")}
          className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${chartType === "weekly" ? "text-white bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
        >
          주간
        </button>
        <button
          onClick={() => setChartType("daily")}
          className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${chartType === "daily" ? "text-white bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
        >
          일간
        </button>
      </div>
      <div className="bg-[#272822] text-[#F8F8F2] rounded-lg p-5 shadow-lg">
        <h3 className="text-[#A6E22E] text-xl mb-3.5">{title}</h3>
        <ul className="list-none p-0 m-0">
          {data.length > 0 ? (
            data.map((song, index) => (
              <li
                key={song.id}
                className="flex justify-between items-center mb-2.5 p-2.5 bg-[#3E3D32] rounded-lg text-[#F8F8F2] transition-colors duration-200 hover:bg-[#A6E22E] hover:text-[#272822]"
                onClick={() => handleSongClick(song.videoId)}
                style={{ cursor: "pointer" }}
              >
                <h4 className="m-0 text-base font-bold text-[#F8F8F2]">
                  {index + 1}. {song.title}
                </h4>
                <span className="text-sm text-[#F8F8F2]">{song.artist}</span>
              </li>
            ))
          ) : (
            <p>차트가 없습니다.</p>
          )}
        </ul>
        <div className="mt-2.5 text-xs text-[#F8F8F2] opacity-80">기준: {chartType === "weekly" ? "매주 월요일 00시" : "매일 00시"}</div>

        {selectedVideoId && (
          <VideoModal videoId={selectedVideoId} onClose={handleCloseModal} />
        )}
      </div>
    </>
  );
};

export default WeeklyChart;
