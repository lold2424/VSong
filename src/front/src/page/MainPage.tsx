import React, { useEffect, useState, useContext } from 'react';
import axios from 'axios';
import './MainPage.css';
import VideoCard from './components/VideoCard';
import { GenderContext } from './components/GenderContext';

const apiUrl = process.env.REACT_APP_API_URL;

const MainPage: React.FC = () => {
    const [data, setData] = useState<any>({
        randomSongs: [],
        top10RecentSongs: [],
        top10DailySongs: [],
        top10WeeklySongs: [],
        randomShorts: [],
        top9RecentShorts: [],
    });
    const [isLoading, setIsLoading] = useState(true);
    const { genderFilter } = useContext(GenderContext);

    interface MainPageApiResponse {
        randomSongs: any[];
        top10RecentSongs: any[];
        top10DailySongs: any[];
        top10WeeklySongs: any[];
        randomShorts: any[];
        top9RecentShorts: any[];
    }

    const fetchMainPageData = (gender: string) => {
        console.log('Fetching main page data with gender:', gender);
        setIsLoading(true);
        axios.get<MainPageApiResponse>(`${apiUrl}/main`, {
            params: { gender },
        })
            .then((response) => {
                setData({
                    randomSongs: response.data.randomSongs || [],
                    top10RecentSongs: response.data.top10RecentSongs || [],
                    top10DailySongs: response.data.top10DailySongs || [],
                    top10WeeklySongs: response.data.top10WeeklySongs || [],
                    randomShorts: response.data.randomShorts || [],
                    top9RecentShorts: response.data.top9RecentShorts || [],
                });
                setIsLoading(false);
            })
            .catch((error) => {
                console.error('백엔드에서 데이터 가져오기 오류:', error);
                setIsLoading(false);
            });
    };

    useEffect(() => {
        fetchMainPageData(genderFilter);
    }, [genderFilter]);

    if (isLoading) {
        return <p>로딩 중...</p>;
    }

    return (
        <div className="main-layout">
            <div className="content">

                <section>
                    <h2>최신 노래</h2>
                    <div className="video-grid">
                        {data.top10RecentSongs?.map((song: any) => (
                            <VideoCard key={song.id} song={song} />
                        ))}
                    </div>
                </section>

                <section>
                    <h2>이 노래 어떠신가요?</h2>
                    <div className="video-grid">
                        {data.randomSongs?.map((song: any) => (
                            <VideoCard key={song.id} song={song} />
                        ))}
                    </div>
                </section>

                <section>
                    <h2>최신 쇼츠</h2>
                    <div className="video-grid">
                        {data.top9RecentShorts && data.top9RecentShorts.length > 0 ? (
                            data.top9RecentShorts.slice(0, 10).map((short: any) => (
                                <VideoCard key={short.id} song={short} />
                            ))
                        ) : (
                            <p>최신 쇼츠가 없습니다.</p>
                        )}
                    </div>
                </section>

                <section>
                    <h2>이 쇼츠 어떠신가요</h2>
                    <div className="video-grid">
                        {data.randomShorts.slice(0, 9).map((short: any) => (
                            <VideoCard key={short.id} song={short} />
                        ))}
                    </div>
                </section>
            </div>
        </div>
    );
};

export default MainPage;
