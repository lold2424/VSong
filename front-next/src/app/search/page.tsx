"use client";

import React, { useEffect, useState, Suspense } from "react";
import axios from "axios";
import { useSearchParams, useRouter } from "next/navigation";
import VideoCard from "@/components/VideoCard";
import Image from "next/image";
import AlertModal from "@/components/AlertModal";

const SearchResultsPage: React.FC = () => {
  const [searchResults, setSearchResults] = useState<{
    songs: any[];
    vtubers: any[];
  } | null>(null);
  const [visibleSongs, setVisibleSongs] = useState<any[]>([]);
  const [songPage, setSongPage] = useState(1);
  const [hasMoreSongs, setHasMoreSongs] = useState(true);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMessage, setModalMessage] = useState("");

  const searchParams = useSearchParams();
  const router = useRouter();
  const query = searchParams.get("query");
  const channelId = searchParams.get("channelId");

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
      axios
        .get<SearchResultsApiResponse>(
          `/api/v1/vtubers/search`,
          {
            params: { query: queryValue, channelId },
            withCredentials: true,
          }
        )
        .then((response) => {
          setSearchResults(response.data);
          if (response.data && response.data.songs) {
            setVisibleSongs(response.data.songs.slice(0, 10));
          } else {
            setVisibleSongs([]);
          }
        })
        .catch((error) => {
          console.error("검색 중 오류 발생:", error);
          if (axios.isAxiosError(error) && error.response) {
            setModalMessage(error.response.data.message || "검색 중 오류가 발생했습니다.");
            setIsModalOpen(true);
          }
        });
    }
  }, [query, channelId, queryValue]);

  const loadMoreSongs = () => {
    if (!searchResults) return;

    const nextPage = songPage + 1;
    const newSongs = searchResults.songs.slice(
      visibleSongs.length,
      visibleSongs.length + 10
    );
    setVisibleSongs([...visibleSongs, ...newSongs]);
    setSongPage(nextPage);

    if (newSongs.length < 10) {
      setHasMoreSongs(false);
    }
  };

  return (
    <div className="p-5 max-w-6xl mx-auto bg-[#272822] text-[#F8F8F2]">
      <AlertModal 
        isOpen={isModalOpen} 
        title="검색 안내" 
        message={modalMessage} 
        onClose={() => {
          setIsModalOpen(false);
          router.push("/");
        }} 
      />
      <h1 id="search-results-title" className="text-2xl mb-8 text-[#A6E22E] font-bold">
        ‘{queryValue}’에 대한 검색 결과입니다.
      </h1>

      {searchResults?.vtubers && searchResults.vtubers.length > 0 && (
        <section aria-labelledby="vtuber-channels-title" className="mb-16">
          <h2 id="vtuber-channels-title" className="text-2xl mb-6 text-[#A6E22E] font-bold border-b border-[#3E3D32] pb-2">
            버튜버 채널
          </h2>
          <div className="flex flex-col gap-5">
            {searchResults.vtubers.map((vtuber: any) => (
              <article 
                key={vtuber.channelId}
                aria-labelledby={`vtuber-name-${vtuber.channelId}`}
                className="bg-[#3E3D32] rounded-xl p-5 text-left flex items-center shadow-lg text-[#F8F8F2] gap-5 transition-transform duration-200 hover:scale-[1.01]"
              >
                <Image
                  src={vtuber.channelImg}
                  alt={`${vtuber.name} 프로필 이미지`}
                  width={80}
                  height={80}
                  className="w-20 h-20 rounded-full border-2 border-[#66D9EF] object-cover"
                />
                <div className="flex-grow flex flex-col justify-center gap-y-2">
                  <h3 id={`vtuber-name-${vtuber.channelId}`} className="text-xl m-0 font-bold text-[#A6E22E]">
                    {vtuber.name}
                  </h3>
                  <p className="text-sm text-[#F8F8F2]">구독자 수: <span className="font-semibold">{vtuber.subscribers}</span></p>
                  <button
                    onClick={() => router.push(`/vtuber/${vtuber.channelId}`)}
                    aria-label={`${vtuber.name} 상세 정보 보기`}
                    className="w-fit px-6 py-2 rounded-lg border-none cursor-pointer font-bold transition-all duration-300 bg-[#A6E22E] text-[#272822] hover:bg-[#8ecb28] focus-visible:ring-2 focus-visible:ring-white outline-none mt-2"
                  >
                    상세 정보 보기
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}

      <section aria-labelledby="songs-results-title">
        <h2 id="songs-results-title" className="text-2xl mb-6 text-[#A6E22E] font-bold border-b border-[#3E3D32] pb-2">
          등록된 노래
        </h2>
        <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-5">
          {visibleSongs.length > 0 ? (
            visibleSongs.map((song: any) => (
              <VideoCard key={song.id} song={song} />
            ))
          ) : (
            <p className="text-gray-400 italic py-10 text-center col-span-full">검색된 노래가 없습니다.</p>
          )}
        </div>

        {hasMoreSongs && (
          <button 
            onClick={loadMoreSongs} 
            className="block w-full py-4 bg-[#3E3D32] text-[#A6E22E] border border-[#A6E22E] rounded-lg cursor-pointer mt-8 transition-all duration-300 hover:bg-[#A6E22E] hover:text-[#272822] font-bold focus-visible:ring-2 focus-visible:ring-[#A6E22E] outline-none"
            aria-label="노래 결과 더 보기"
          >
            노래 더 보기
          </button>
        )}
      </section>
    </div>
  );
};

const SearchPage = () => (
  <Suspense fallback={<div>Loading...</div>}>
    <SearchResultsPage />
  </Suspense>
);

export default SearchPage;
