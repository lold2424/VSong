"use client";

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';

const SongLogDetailPage = () => {
  const { id } = useParams();
  const router = useRouter();
  const [log, setLog] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchLog = async () => {
      try {
        const response = await fetch(`/api/admin/monitoring/song-log/${id}`, {
          credentials: 'include',
        });
        if (response.ok) {
          const data = await response.json();
          if (data.newSongsJson) data.newSongs = JSON.parse(data.newSongsJson);
          if (data.excludedSongsJson) data.excludedSongs = JSON.parse(data.excludedSongsJson);
          if (data.failedSongsJson) data.failedSongs = JSON.parse(data.failedSongsJson);
          if (data.errorSummaryJson) data.errorSummary = JSON.parse(data.errorSummaryJson);
          if (data.slowestChannelsJson) data.slowestChannels = JSON.parse(data.slowestChannelsJson);
          setLog(data);
        } else {
          alert('로그를 찾을 수 없습니다.');
          router.push('/admin');
        }
      } catch (error) {
        console.error('Failed to fetch log detail', error);
      } finally {
        setIsLoading(false);
      }
    };

    if (id) fetchLog();
  }, [id, router]);

  if (isLoading) return <div className="text-center p-10 text-white">로딩 중...</div>;
  if (!log) return null;

  return (
    <div className="max-w-5xl mx-auto p-8 bg-gray-800 text-white rounded-lg shadow-lg">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-[#A6E22E]">노래 수집 상세 내역</h1>
        <Link href="/admin" className="bg-gray-700 hover:bg-gray-600 px-4 py-2 rounded text-sm transition">
          ← 뒤로 가기
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-gray-900 p-4 rounded-md">
          <p className="text-xs text-gray-400 uppercase">실행 시간</p>
          <p className="font-mono text-sm">{new Date(log.runTime).toLocaleString()}</p>
        </div>
        <div className="bg-gray-900 p-4 rounded-md">
          <p className="text-xs text-gray-400 uppercase">소요 시간</p>
          <p className="text-xl font-bold">{log.durationSeconds}s</p>
        </div>
        <div className="bg-gray-900 p-4 rounded-md">
          <p className="text-xs text-gray-400 uppercase">추가 / 제외 / 실패</p>
          <p className="text-xl font-bold">
            <span className="text-[#A6E22E]">{log.newSongsCount}</span> / 
            <span className="text-orange-400"> {log.excludedSongsCount}</span> / 
            <span className="text-red-500"> {log.failedSongsCount}</span>
          </p>
        </div>
        <div className="bg-gray-900 p-4 rounded-md">
          <p className="text-xs text-gray-400 uppercase">ID</p>
          <p className="font-mono text-sm">#{log.id}</p>
        </div>
      </div>

      <div className="space-y-8">
        {/* 추가된 노래 */}
        <section>
          <h2 className="text-lg font-bold mb-3 border-l-4 border-[#A6E22E] pl-2 text-[#A6E22E]">추가된 노래 목록 ({log.newSongsCount})</h2>
          <div className="bg-gray-900 rounded-md p-4 max-h-80 overflow-y-auto">
            {log.newSongs?.length > 0 ? (
              <ul className="space-y-1 text-sm">
                {log.newSongs.map((song: string, idx: number) => (
                  <li key={idx} className="border-b border-gray-800 pb-1 last:border-0 text-gray-300 italic">{song}</li>
                ))}
              </ul>
            ) : (
              <p className="text-gray-500 text-sm">추가된 노래가 없습니다.</p>
            )}
          </div>
        </section>

        {/* 제외된 노래 (상세 사유) */}
        <section>
          <h2 className="text-lg font-bold mb-3 border-l-4 border-orange-400 pl-2 text-orange-400">제외된 노래 상세 사유 ({log.excludedSongsCount})</h2>
          <div className="bg-gray-900 rounded-md p-4 max-h-80 overflow-y-auto">
            {log.excludedSongs?.length > 0 ? (
              <ul className="space-y-1 text-xs">
                {log.excludedSongs.map((info: string, idx: number) => (
                  <li key={idx} className="border-b border-gray-800 pb-1 last:border-0 text-gray-400">{info}</li>
                ))}
              </ul>
            ) : (
              <p className="text-gray-500 text-sm">제외된 노래가 없습니다.</p>
            )}
          </div>
        </section>

        {/* 에러 요약 */}
        {log.errorSummary && Object.keys(log.errorSummary).length > 0 && (
          <section>
            <h2 className="text-lg font-bold mb-3 border-l-4 border-red-500 pl-2 text-red-500">에러 요약</h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              {Object.entries(log.errorSummary).map(([key, value]: [string, any]) => (
                <div key={key} className="bg-gray-900 p-2 rounded text-center">
                  <p className="text-[10px] text-gray-500 uppercase">{key}</p>
                  <p className="text-sm font-bold text-red-400">{value}</p>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* 성능 병목 */}
        {log.slowestChannels?.length > 0 && (
          <section>
            <details className="text-xs bg-gray-900 rounded-md p-3 border border-gray-800">
              <summary className="cursor-pointer text-blue-400 hover:text-white transition font-bold flex justify-between items-center">
                <span className="text-lg border-l-4 border-blue-400 pl-2">수집 속도 하위 20% 채널</span>
                <span className="text-[10px] bg-gray-800 px-2 py-0.5 rounded font-normal text-white">클릭하여 펼치기</span>
              </summary>
              <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                {log.slowestChannels.map((item: any, idx: number) => (
                  <div key={idx} className="bg-gray-800 p-3 rounded border-l-2 border-red-500 flex justify-between items-center">
                    <div>
                      <p className="text-xs font-bold truncate max-w-[150px]">{item.name}</p>
                      <p className="text-[10px] text-gray-500">{item.type}</p>
                    </div>
                    <span className="text-xs font-mono text-red-300">{(item.durationMs / 1000).toFixed(1)}s</span>
                  </div>
                ))}
              </div>
            </details>
          </section>
        )}
      </div>
    </div>
  );
};

export default SongLogDetailPage;
