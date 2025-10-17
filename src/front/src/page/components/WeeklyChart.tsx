import React, { useState } from 'react';
import VideoModal from './VideoModal'; // 모달 컴포넌트 임포트
import './WeeklyChart.css'; // 스타일 파일

interface WeeklyChartProps {
    top10WeeklySongs: {
        id: string;
        title: string;
        artist: string;
        videoId: string;
    }[];
    title: string; // 제목을 props로 받도록 추가
}

const WeeklyChart: React.FC<WeeklyChartProps> = ({ top10WeeklySongs, title }) => {
    const [selectedVideoId, setSelectedVideoId] = useState<string | null>(null);

    const handleSongClick = (videoId: string) => {
        setSelectedVideoId(videoId); // 클릭한 노래의 videoId를 설정
    };

    const handleCloseModal = () => {
        setSelectedVideoId(null); // 모달 닫기
    };

    return (
        <div className="weekly-chart">
            <h3>{title}</h3>
            <ul className="chart-list">
                {top10WeeklySongs.length > 0 ? (
                    top10WeeklySongs.map((song, index) => (
                        <li
                            key={song.id}
                            className="chart-item"
                            onClick={() => handleSongClick(song.videoId)} // 클릭 시 모달 열기
                            style={{ cursor: 'pointer', color: 'black' }}
                        >
                            <h4>{index + 1}. {song.title}</h4>
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
    );
};

export default WeeklyChart;
