import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './SearchResultsPage.css';
import { useLocation, useNavigate } from 'react-router-dom';
import VideoCard from './components/VideoCard';

const apiUrl = import.meta.env.VITE_API_URL;

const SearchResultsPage: React.FC = () => {
    const [searchResults, setSearchResults] = useState<{ songs: any[], vtubers: any[] } | null>(null);
    const [visibleSongs, setVisibleSongs] = useState<any[]>([]);
    const [songPage, setSongPage] = useState(1);
    const [hasMoreSongs, setHasMoreSongs] = useState(true);

    const location = useLocation();
    const navigate = useNavigate();
    const query = new URLSearchParams(location.search).get('query');
    const channelId = new URLSearchParams(location.search).get('channelId');

    const queryValue = query ? query : "";

    interface SearchResultsApiResponse {
        songs: any[];
        vtubers: {
            channelId: string;
            channelImg: string;
            name: string;
            subscribers: string;
        }[];
    }

    useEffect(() => {
        if (query || channelId) {
            axios.get<SearchResultsApiResponse>(`${apiUrl}/api/v1/vtubers/search`, {
                params: { query: queryValue, channelId },
            })
                .then((response) => {
                    setSearchResults(response.data);
                    if (response.data && response.data.songs) {
                        setVisibleSongs(response.data.songs.slice(0, 10));
                    } else {
                        setVisibleSongs([]);
                    }
                })
                .catch((error) => {
                    console.error('검색 중 오류 발생:', error);
                });
        }
    }, [query, channelId]);

    const loadMoreSongs = () => {
        if (!searchResults) return;

        const nextPage = songPage + 1;
        const newSongs = searchResults.songs.slice(visibleSongs.length, visibleSongs.length + 10);
        setVisibleSongs([...visibleSongs, ...newSongs]);
        setSongPage(nextPage);

        if (newSongs.length < 10) {
            setHasMoreSongs(false);
        }
    };

    return (
        <div className="search-results-page">
            <h1>‘{queryValue}’에 대한 검색 결과입니다.</h1>

            {searchResults?.vtubers && searchResults.vtubers.length > 0 && (
                <div>
                    <h2>버튜버 채널</h2>
                    <div className="vtuber-grid">
                        {searchResults.vtubers.map((vtuber: any) => (
                            <div className="vtuber-card" key={vtuber.channelId}>
                                <img src={vtuber.channelImg} alt={vtuber.name} />
                                <div className="vtuber-info">
                                    <h3>{vtuber.name}</h3>
                                    <p>구독자 수: {vtuber.subscribers}</p>
                                    <button onClick={() => navigate(`/vtuber/${vtuber.channelId}`)}>
                                        상세 정보 보기
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            <div>
                <div className="video-grid">
                    {visibleSongs.length > 0 ? (
                        visibleSongs.map((song: any) => (
                            <VideoCard key={song.id} song={song} />
                        ))
                    ) : (
                        <p>검색된 노래가 없습니다.</p>
                    )}
                </div>

                {hasMoreSongs && (
                    <button onClick={loadMoreSongs}>더보기</button>
                )}
            </div>
        </div>
    );
};

export default SearchResultsPage;