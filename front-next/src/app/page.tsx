import React from 'react';
import axios from 'axios';

import VideoCard from '@/components/VideoCard';

interface MainPageApiResponse {
    randomSongs: any[];
    top10RecentSongs: any[];
    top10DailySongs: any[];
    top10WeeklySongs: any[];
    randomShorts: any[];
    top9RecentShorts: any[];
}

async function fetchMainPageData(gender: string) {
    try {
        const response = await axios.get<MainPageApiResponse>(`${process.env.NEXT_PUBLIC_API_URL}/api/main`, {
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
            randomShorts: [],
            top9RecentShorts: [],
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
                    {data.top10RecentSongs?.map((song: any) => (
                        <VideoCard key={song.id} song={song} />
                    ))}
                </div>
            </section>

            <section>
                <h2 className="text-2xl mb-5 mt-10 text-[#A6E22E]">이 노래 어떠신가요?</h2>
                <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5">
                    {data.randomSongs?.map((song: any) => (
                        <VideoCard key={song.id} song={song} />
                    ))}
                </div>
            </section>

            <section>
                <h2 className="text-2xl mb-5 mt-10 text-[#A6E22E]">최신 쇼츠</h2>
                <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5">
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
                <h2 className="text-2xl mb-5 mt-10 text-[#A6E22E]">이 쇼츠 어떠신가요</h2>
                <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5">
                    {data.randomShorts.slice(0, 9).map((short: any) => (
                        <VideoCard key={short.id} song={short} />
                    ))}
                </div>
            </section>
        </>
    );
}