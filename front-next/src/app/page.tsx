import React from 'react';
import axios from 'axios';

import VideoCard from '@/components/VideoCard';

interface MainPageApiResponse {
    randomSongs: any[];
    top10RecentSongs: any[];
    top10DailySongs: any[];
    top10WeeklySongs: any[];
}

async function fetchMainPageData(gender: string) {
    try {
        const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';
        const response = await axios.get<MainPageApiResponse>(`${backendUrl}/api/home`, {
            params: { gender },
        });
        return response.data;
    } catch (error) {
        console.error('백엔드에서 데이터 가져오기 오류:', error);
        return {
            randomSongs: [],
            top10RecentSongs: [],
            top10DailySongs: [],
            top10WeeklySongs: [],
        };
    }
}

export default async function Page({ searchParams }: { searchParams: { [key: string]: string | undefined } }) {
    const gender = searchParams.gender || 'all';
    const data = await fetchMainPageData(gender);

    return (
        <>
            <section>
                <h2 className="text-2xl mb-5 text-[#A6E22E]">최신 노래</h2>
                <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5">
                    {data.top10RecentSongs?.map((song: any, index: number) => (
                        <VideoCard key={song.videoId || song.id} song={song} isPriority={index < 4} />
                    ))}
                </div>
            </section>

            <section>
                <h2 className="text-2xl mb-5 mt-10 text-[#A6E22E]">이 노래 어떠신가요?</h2>
                <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5">
                    {data.randomSongs?.map((song: any) => (
                        <VideoCard key={song.videoId || song.id} song={song} />
                    ))}
                </div>
            </section>
        </>
    );
}
