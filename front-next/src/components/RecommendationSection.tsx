"use client";

import React, { useEffect, useState } from 'react';
import { getYoutubeRecommendations, refreshYoutubeRecommendations } from '@/utils/youtubeApi';
import VideoCard from './VideoCard';
import { useAuth } from '@/context/AuthContext';

const RecommendationSection: React.FC = () => {
    const { isLoggedIn } = useAuth();
    const [recommendations, setRecommendations] = useState<{ keywords: string[], songs: any[] } | null>(null);
    const [loading, setLoading] = useState(false);
    const [refreshing, setRefreshing] = useState(false);

    useEffect(() => {
        if (isLoggedIn) {
            setLoading(true);
            getYoutubeRecommendations()
                .then(data => setRecommendations(data))
                .catch(err => console.error("추천 로딩 실패:", err))
                .finally(() => setLoading(false));
        }
    }, [isLoggedIn]);

    const handleRefresh = async () => {
        if (refreshing) return;
        
        if (confirm("최근 YouTube 활동을 바탕으로 취향을 다시 분석할까요?\n(5분에 한 번만 가능합니다.)")) {
            setRefreshing(true);
            try {
                const data = await refreshYoutubeRecommendations();
                setRecommendations(data);
                alert("취향 분석이 갱신되었습니다!");
            } catch (err: any) {
                if (err.response?.status === 429) {
                    alert(err.response.data || "5분에 한 번만 요청 가능합니다.");
                } else {
                    alert("분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                }
            } finally {
                setRefreshing(false);
            }
        }
    };

    if (!isLoggedIn) {
        return null;
    }

    if (!loading && (!recommendations || recommendations.songs.length === 0)) {
        return (
            <section className="mb-12">
                <div className="bg-[#3E3D32] border border-[#A6E22E] border-opacity-20 rounded-lg p-8 text-center">
                    <h2 className="text-xl font-bold text-[#A6E22E] mb-4">나만의 AI 취향 맞춤 추천을 받아보세요!</h2>
                    <p className="text-gray-400 mb-6">최근 YouTube 활동을 분석하여 유저님의 취향에 딱 맞는 버튜버 곡들을 추천해 드립니다.</p>
                    <button 
                        onClick={handleRefresh}
                        disabled={refreshing}
                        className="inline-flex items-center gap-2 px-6 py-3 bg-[#A6E22E] text-black rounded-md font-bold hover:bg-[#8ecf27] transition-colors"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className={`h-5 w-5 ${refreshing ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                        </svg>
                        {refreshing ? '취향 분석 중...' : '지금 바로 분석하기'}
                    </button>
                </div>
            </section>
        );
    }

    return (
        <section className="mb-12">
            <div className="flex items-center justify-between mb-6 border-b border-[#3E3D32] pb-4">
                <div className="flex items-center gap-3">
                    <h2 className="text-2xl font-bold text-[#A6E22E] flex items-center gap-3">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                        </svg>
                        AI 취향 맞춤 추천
                    </h2>
                    {recommendations?.keywords && (
                        <div className="hidden md:flex gap-2">
                            {recommendations.keywords.map((keyword, i) => (
                                <span key={i} className="text-xs bg-[#3E3D32] text-[#A6E22E] px-2 py-1 rounded-full border border-[#A6E22E] border-opacity-30">
                                    #{keyword}
                                </span>
                            ))}
                        </div>
                    )}
                </div>
                
                <button 
                    onClick={handleRefresh}
                    disabled={refreshing}
                    className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-colors
                        ${refreshing 
                            ? 'bg-[#3E3D32] text-gray-500 cursor-not-allowed' 
                            : 'bg-[#3E3D32] text-[#A6E22E] hover:bg-[#4E4D42] border border-[#A6E22E] border-opacity-30'}`}
                >
                    <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    {refreshing ? '분석 중...' : '취향 재분석'}
                </button>
            </div>
            
            <div className="flex flex-wrap gap-2 mb-4 md:hidden">
                {recommendations?.keywords?.map((keyword, i) => (
                    <span key={i} className="text-[10px] bg-[#3E3D32] text-[#A6E22E] px-2 py-0.5 rounded-full border border-[#A6E22E] border-opacity-30">
                        #{keyword}
                    </span>
                ))}
            </div>
            
            {loading ? (
                <div className="flex justify-center items-center h-40">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#A6E22E]"></div>
                </div>
            ) : (
                <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-2.5">
                    {recommendations?.songs.map((song: any) => (
                        <VideoCard key={song.videoId || song.id} song={song} />
                    ))}
                </div>
            )}
        </section>
    );
};

export default RecommendationSection;
