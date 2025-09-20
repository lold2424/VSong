import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './VtuberDetailPage.css';
import VideoModal from './components/VideoModal';

const apiUrl = process.env.REACT_APP_API_URL;

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

const VtuberDetailPage: React.FC = () => {
    const { channelId } = useParams<{ channelId: string }>();
    const [vtuberDetail, setVtuberDetail] = useState<VtuberDetail | null>(null);
    const [songs, setSongs] = useState<Song[]>([]);
    const [selectedVideoId, setSelectedVideoId] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        if (channelId) {
            axios
                .get<VtuberDetail>(`${apiUrl}/api/v1/vtubers/${channelId}/details`)
                .then((response) => {
                    setVtuberDetail(response.data);
                })
                .catch((error) => {
                    console.error('버튜버 상세 정보를 가져오는 중 오류 발생:', error);
                });

            axios
                .get<Song[]>(`${apiUrl}/api/v1/vtubers/${channelId}/songs`)
                .then((response) => {
                    setSongs(response.data);
                })
                .catch((error) => {
                    console.error('노래 목록을 가져오는 중 오류 발생:', error);
                });
        }
    }, [channelId]);

    if (!vtuberDetail) {
        return <p>로딩 중...</p>;
    }

    const handleOpenModal = (videoId: string) => {
        setSelectedVideoId(videoId);
    };

    const handleCloseModal = () => {
        setSelectedVideoId(null);
    };

    const genderText =
        vtuberDetail.gender === 'female'
            ? '여성'
            : vtuberDetail.gender === 'male'
                ? '남성'
                : '혼성';

    return (
        <div className="vtuber-detail-page">
            <div className="detail-container">
                <div className="profile-section">
                    <img
                        src={vtuberDetail.channelImg}
                        alt={vtuberDetail.name}
                        className="profile-image"
                    />
                </div>
                <div className="info-section">
                    <h1 className="vtuber-name">{vtuberDetail.name}</h1>
                    <p className="vtuber-info">구독자 수: {vtuberDetail.subscribers.toLocaleString()}</p>
                    <p className="vtuber-info">성별: {genderText}</p>
                    <p className="vtuber-info">등록된 노래 수: {vtuberDetail.songCount}</p>
                    <button
                        onClick={() => window.open(`https://www.youtube.com/channel/${channelId}`, '_blank')}
                        className="action-button youtube-button"
                    >
                        유튜브로 이동
                    </button>
                    <button onClick={() => navigate(-1)} className="action-button back-button">
                        뒤로 가기
                    </button>
                </div>
            </div>
            <div className="songs-section">
                <h2>등록된 노래</h2>
                {songs.length > 0 ? (
                    <ul className="songs-list">
                        {songs.map((song) => (
                            <li key={song.id} className="song-item">
                                <img
                                    src={`https://img.youtube.com/vi/${song.videoId}/0.jpg`}
                                    alt={song.title}
                                    className="song-thumbnail"
                                />
                                <div className="song-info">
                                    <h3 className="song-title">{song.title}</h3>
                                    <p>조회수: {song.viewCount.toLocaleString()}</p>
                                    <p>게시일: {new Date(song.publishedAt).toLocaleDateString()}</p>
                                    <button
                                        onClick={() => handleOpenModal(song.videoId)}
                                        className="action-button song-watch-button"
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

export default VtuberDetailPage;
