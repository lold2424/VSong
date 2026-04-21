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
      <h1 className="text-2xl mb-5 text-[#A6E22E]">‘{queryValue}’에 대한 검색 결과입니다.</h1>

      {searchResults?.vtubers && searchResults.vtubers.length > 0 && (
        <div>
          <h2 className="text-2xl mb-5 text-[#A6E22E]">버튜버 채널</h2>
          <div className="flex flex-col gap-5 mb-10">
            {searchResults.vtubers.map((vtuber: any) => (
              <div className="bg-[#3E3D32] rounded-xl p-5 text-left flex items-center shadow-lg text-[#F8F8F2] gap-5" key={vtuber.channelId}>
                <Image
                  src={vtuber.channelImg}
                  alt={vtuber.name}
                  width={80}
                  height={80}
                  className="w-20 h-20 rounded-full mr-5 border-2 border-[#66D9EF] object-cover"
                />
                <div className="flex-grow flex flex-col justify-center gap-y-2">
                  <h3 className="text-xl m-0 font-bold text-[#A6E22E]">{vtuber.name}</h3>
                  <p className="text-sm text-[#F8F8F2]">구독자 수: {vtuber.subscribers}</p>
                  <button
                    onClick={() => router.push(`/vtuber/${vtuber.channelId}`)}
                    className="px-4 py-3 rounded border-none cursor-pointer flex items-center justify-center font-bold transition-colors duration-300 bg-[#A6E22E] text-[#272822] hover:bg-[#3E3D32] hover:text-[#F8F8F2] mt-4"
                  >
                    상세 정보 보기
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div>
        <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-5">
          {visibleSongs.length > 0 ? (
            visibleSongs.map((song: any) => (
              <VideoCard key={song.id} song={song} />
            ))
          ) : (
            <p>검색된 노래가 없습니다.</p>
          )}
        </div>

        {hasMoreSongs && <button onClick={loadMoreSongs} className="block w-full py-3 bg-[#A6E22E] text-[#272822] border-none rounded cursor-pointer mt-5 transition-colors duration-300 hover:bg-[#3E3D32] hover:text-[#F8F8F2]">더보기</button>}
      </div>
    </div>
  );
};

const SearchPage = () => (
  <Suspense fallback={<div>Loading...</div>}>
    <SearchResultsPage />
  </Suspense>
);

export default SearchPage;
