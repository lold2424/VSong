import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import VideoModal from './VideoModal'; // 모달 컴포넌트 임포트

interface VideoCardProps {
    song: {
        videoId: string;
        title: string;
        vtuberName: string;
        publishedAt: string;
        viewCount: number;
        channelId: string;
    };
}

const VideoCard: React.FC<VideoCardProps> = ({ song }) => {
    const navigate = useNavigate();
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

    const handleVtuberNameClick = (event: React.MouseEvent<HTMLParagraphElement>) => {
        event.stopPropagation(); // 카드 클릭 이벤트 방지
        navigate(`/search?query=${encodeURIComponent(song.vtuberName)}`);
    };

    return (
        <>
            <div
                className="video-card"
                onMouseEnter={handleMouseEnter}
                onMouseLeave={handleMouseLeave}
                onClick={handleCardClick} // 카드 클릭 시 모달 열기
            >
                {!hovered ? (
                    <div className="thumbnail-content">
                        <img src={thumbnailUrl} alt={song.title} className="thumbnail" />
                        <h3 style={{ cursor: 'pointer' }}>{song.title}</h3>
                    </div>
                ) : (
                    <div className="hover-content">
                        <h3 style={{ cursor: 'pointer' }}>{song.title}</h3>
                        <p
                            style={{ color: 'blue', cursor: 'pointer' }}
                            onClick={handleVtuberNameClick} // 채널명 클릭 이벤트
                        >
                            채널명: {song.vtuberName}
                        </p>
                        <p>조회수: {song.viewCount.toLocaleString()}</p>
                        <p>게시일: {new Date(song.publishedAt).toLocaleDateString()}</p>
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
