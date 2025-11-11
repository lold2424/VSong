"use client";

import React, { useEffect, useState, Suspense } from "react";
import axios from "axios";
import { useSearchParams, useRouter } from "next/navigation";
import VideoCard from "@/components/VideoCard";
import "./SearchResultsPage.css";
import Image from "next/image";

const SearchResultsPage: React.FC = () => {
  const [searchResults, setSearchResults] = useState<{
    songs: any[];
    vtubers: any[];
  } | null>(null);
  const [visibleSongs, setVisibleSongs] = useState<any[]>([]);
  const [songPage, setSongPage] = useState(1);
  const [hasMoreSongs, setHasMoreSongs] = useState(true);

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
          `${process.env.NEXT_PUBLIC_API_URL}/api/v1/vtubers/search`,
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
    <div className="search-results-page">
      <h1>‘{queryValue}’에 대한 검색 결과입니다.</h1>

      {searchResults?.vtubers && searchResults.vtubers.length > 0 && (
        <div>
          <h2>버튜버 채널</h2>
          <div className="vtuber-grid">
            {searchResults.vtubers.map((vtuber: any) => (
              <div className="vtuber-card" key={vtuber.channelId}>
                <Image
                  src={vtuber.channelImg}
                  alt={vtuber.name}
                  width={80}
                  height={80}
                />
                <div className="vtuber-info">
                  <h3>{vtuber.name}</h3>
                  <p>구독자 수: {vtuber.subscribers}</p>
                  <button
                    onClick={() => router.push(`/vtuber/${vtuber.channelId}`)}
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
        <div className="video-grid">
          {visibleSongs.length > 0 ? (
            visibleSongs.map((song: any) => (
              <VideoCard key={song.id} song={song} />
            ))
          ) : (
            <p>검색된 노래가 없습니다.</p>
          )}
        </div>

        {hasMoreSongs && <button onClick={loadMoreSongs}>더보기</button>}
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