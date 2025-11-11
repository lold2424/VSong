"use client";

import React, { useState } from "react";
import VideoModal from "./VideoModal"; // 모달 컴포넌트 임포트
import "./WeeklyChart.css"; // 스타일 파일

interface Song {
  id: string;
  title: string;
  artist: string;
  videoId: string;
}

interface WeeklyChartProps {
  top10WeeklySongs: Song[];
  top10DailySongs: Song[];
  top10WeeklyShorts: Song[];
}

const WeeklyChart: React.FC<WeeklyChartProps> = ({
  top10WeeklySongs,
  top10DailySongs,
  top10WeeklyShorts,
}) => {
  const [selectedVideoId, setSelectedVideoId] = useState<string | null>(null);
  const [chartType, setChartType] = useState<"weekly" | "daily" | "shorts">(
    "weekly"
  );

  const handleSongClick = (videoId: string) => {
    setSelectedVideoId(videoId); // 클릭한 노래의 videoId를 설정
  };

  const handleCloseModal = () => {
    setSelectedVideoId(null); // 모달 닫기
  };

  const getChartData = () => {
    switch (chartType) {
      case "weekly":
        return { data: top10WeeklySongs, title: "주간 인기 노래" };
      case "daily":
        return { data: top10DailySongs, title: "일간 인기 노래" };
      case "shorts":
        return { data: top10WeeklyShorts, title: "주간 인기 쇼츠" };
      default:
        return { data: [], title: "" };
    }
  };

  const { data, title } = getChartData();

  return (
    <>
      <div className="button-group">
        <button
          onClick={() => setChartType("weekly")}
          className={chartType === "weekly" ? "active" : ""}
        >
          주간
        </button>
        <button
          onClick={() => setChartType("daily")}
          className={chartType === "daily" ? "active" : ""}
        >
          일간
        </button>
        <button
          onClick={() => setChartType("shorts")}
          className={chartType === "shorts" ? "active" : ""}
        >
          쇼츠
        </button>
      </div>
      <div className="weekly-chart">
        <h3>{title}</h3>
        <ul className="chart-list">
          {data.length > 0 ? (
            data.map((song, index) => (
              <li
                key={song.id}
                className="chart-item"
                onClick={() => handleSongClick(song.videoId)} // 클릭 시 모달 열기
                style={{ cursor: "pointer" }}
              >
                <h4>
                  {index + 1}. {song.title}
                </h4>
                <span>{song.artist}</span>
              </li>
            ))
          ) : (
            <p>차트가 없습니다.</p>
          )}
        </ul>
        <div className="chart-footer">월요일 00시 기준</div>

        {selectedVideoId && (
          <VideoModal videoId={selectedVideoId} onClose={handleCloseModal} />
        )}
      </div>
    </>
  );
};

export default WeeklyChart;