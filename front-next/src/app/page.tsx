import React from 'react';
import axios from 'axios';
import VideoCard from '@/components/VideoCard';
import RecommendationSection from '@/components/RecommendationSection';

export const dynamic = 'force-dynamic';

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

    const jsonLd = {
        "@context": "https://schema.org",
        "@type": "WebSite",
        "name": "VSong",
        "url": "https://www.vsong.site",
        "description": "버튜버 음악의 모든 순간을 기록합니다. 실시간 수집되는 버추얼 유튜버들의 최신곡과 주간 인기 트렌드를 VSong에서 만나보세요.",
        "potentialAction": {
            "@type": "SearchAction",
            "target": "https://www.vsong.site/search?query={search_term_string}",
            "query-input": "required name=search_term_string"
        }
    };

    return (
        <>
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
            />
            <RecommendationSection />
            
            <section aria-labelledby="recent-songs-title" className="mb-12">
                <div className="flex items-center justify-between mb-6 border-b border-[#3E3D32] pb-4">
                    <h2 id="recent-songs-title" className="text-2xl font-bold text-[#A6E22E] flex items-center gap-3">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        최신 노래
                    </h2>
                    <span className="text-xs text-gray-500 font-mono italic">Freshly updated</span>
                </div>
                <ul className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5 list-none p-0">
                    {data.top10RecentSongs?.map((song: any, index: number) => (
                        <li key={song.videoId || song.id}>
                            <VideoCard song={song} isPriority={index < 4} />
                        </li>
                    ))}
                </ul>
            </section>

            <section aria-labelledby="recommended-songs-title">
                <h2 id="recommended-songs-title" className="text-2xl mb-5 mt-10 text-[#A6E22E] flex items-center gap-3">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
                    </svg>
                    이 노래 어떠신가요?
                </h2>
                <ul className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5 list-none p-0">
                    {data.randomSongs?.map((song: any) => (
                        <li key={song.videoId || song.id}>
                            <VideoCard song={song} />
                        </li>
                    ))}
                </ul>
            </section>
        </>
    );
}
