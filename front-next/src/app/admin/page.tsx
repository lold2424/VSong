"use client";

import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';

const AdminPage = () => {
  const { user, isLoading } = useAuth();

  const [channelId, setChannelId] = useState('');
  const [addVtuberMessage, setAddVtuberMessage] = useState('');
  const [isSubmittingVtuber, setIsSubmittingVtuber] = useState(false);

  const [cacheMessage, setCacheMessage] = useState('');
  const [isRefreshingCache, setIsRefreshingCache] = useState(false);

  const [monitoringData, setMonitoringData] = useState<any>(null);
  const [isLoadingMonitoring, setIsLoadingMonitoring] = useState(false);

  const fetchMonitoringData = async () => {
    setIsLoadingMonitoring(true);
    try {
      const response = await fetch('/api/admin/monitoring/dashboard', {
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();
        setMonitoringData(data);
      }
    } catch (error) {
      console.error('Failed to fetch monitoring data', error);
    } finally {
      setIsLoadingMonitoring(false);
    }
  };

  React.useEffect(() => {
    if (user?.role === 'ADMIN') {
      fetchMonitoringData();
    }
  }, [user]);

  const handleAddVtuber = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!channelId.trim()) {
      setAddVtuberMessage('채널 ID를 입력해주세요.');
      return;
    }

    setIsSubmittingVtuber(true);
    setAddVtuberMessage('');

    try {
      const response = await fetch('/api/admin/vtuber', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ channelId }),
        credentials: 'include',
      });

      const resultText = await response.text();

      if (response.ok) {
        setAddVtuberMessage(`성공: ${resultText}`);
        setChannelId('');
      } else {
        setAddVtuberMessage(`오류: ${resultText}`);
      }
    } catch (error) {
      setAddVtuberMessage('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsSubmittingVtuber(false);
    }
  };

  const handleCacheRefresh = async () => {
    setIsRefreshingCache(true);
    setCacheMessage('');

    try {
      const response = await fetch('/api/admin/cache/refresh-main-page', {
        method: 'POST',
        credentials: 'include',
      });

      const resultText = await response.text();

      if (response.ok) {
        setCacheMessage(`성공: ${resultText}`);
      } else {
        setCacheMessage(`오류: ${resultText}`);
      }
    } catch (error) {
      setCacheMessage('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsRefreshingCache(false);
    }
  };

  if (isLoading) {
    return <div className="text-center p-10">로딩 중...</div>;
  }

  if (user?.role !== 'ADMIN') {
    return (
      <div className="text-center p-10">
        <h1 className="text-2xl text-red-500 mb-4">접근 권한 없음</h1>
        <p>이 페이지는 관리자만 접근할 수 있습니다.</p>
        <Link href="/" className="text-blue-400 hover:underline mt-4 inline-block">
          홈으로 돌아가기
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-8 bg-gray-800 text-white rounded-lg shadow-lg">
      <h1 className="text-3xl font-bold mb-6 text-center text-[#A6E22E]">관리자 페이지</h1>
      
      {/* 시스템 모니터링 섹션 */}
      <div className="bg-gray-700 p-6 rounded-md mb-8">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-[#66D9EF]">시스템 모니터링</h2>
          <button 
            onClick={fetchMonitoringData} 
            className="text-sm bg-gray-600 hover:bg-gray-500 px-3 py-1 rounded transition"
            disabled={isLoadingMonitoring}
          >
            {isLoadingMonitoring ? '갱신 중...' : '모니터링 새로고침'}
          </button>
        </div>

        {monitoringData ? (
          <div className="space-y-6">
            {/* YouTube API 상태 */}
            <div>
              <h3 className="text-sm font-bold text-gray-400 mb-2 uppercase tracking-wider">YouTube API Quota (할당량)</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left bg-gray-900 rounded-md overflow-hidden">
                  <thead className="bg-gray-800 text-gray-300">
                    <tr>
                      <th className="px-4 py-2">Index</th>
                      <th className="px-4 py-2">Key Prefix</th>
                      <th className="px-4 py-2 text-right">Usage</th>
                      <th className="px-4 py-2 text-center">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {monitoringData.youtubeApi.map((api: any) => (
                      <tr key={api.index} className={`border-t border-gray-800 ${api.isCurrent ? 'bg-gray-800 border-l-4 border-l-[#A6E22E]' : ''}`}>
                        <td className="px-4 py-2">{api.index}</td>
                        <td className="px-4 py-2 font-mono text-xs">{api.keyPrefix}</td>
                        <td className="px-4 py-2 text-right">
                          <span className={api.usage > 9000 ? 'text-red-400' : 'text-green-400'}>{api.usage}</span> / 10,000
                        </td>
                        <td className="px-4 py-2 text-center">
                          {api.isAvailable ? <span className="text-green-500">Available</span> : <span className="text-red-500">Exhausted</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* 수집 통계 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-gray-900 p-4 rounded-md">
                <h3 className="text-xs font-bold text-gray-400 mb-2 uppercase">최근 노래 수집 요약</h3>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-gray-400">실행 시간:</span>
                    <span className="text-xs">{monitoringData.songUpdateStats.lastRunTime ? new Date(monitoringData.songUpdateStats.lastRunTime).toLocaleString() : '기록 없음'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">추가된 노래:</span>
                    <span className="text-[#A6E22E] font-bold">{monitoringData.songUpdateStats.newSongsCount || 0}개</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">제외된 노래:</span>
                    <span className="text-orange-400 font-bold">{monitoringData.songUpdateStats.excludedSongsCount || 0}개</span>
                  </div>
                </div>
              </div>
              <div className="bg-gray-900 p-4 rounded-md">
                <h3 className="text-xs font-bold text-gray-400 mb-2 uppercase">서버 상태</h3>
                <div className="flex items-center gap-2">
                  <div className={`w-3 h-3 rounded-full ${monitoringData.systemHealth === 'UP' ? 'bg-green-500 shadow-[0_0_8px_#22c55e]' : 'bg-red-500'}`}></div>
                  <span className="font-bold">{monitoringData.systemHealth}</span>
                </div>
              </div>
            </div>
            
            {/* 제외 사유 목록 (간략히) */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {monitoringData.songUpdateStats.newSongs?.length > 0 && (
                <details className="text-xs bg-gray-900 rounded-md p-2">
                  <summary className="cursor-pointer text-[#A6E22E] hover:text-white transition font-bold">새로 추가된 노래 목록 보기 ({monitoringData.songUpdateStats.newSongs.length})</summary>
                  <div className="mt-2 max-h-40 overflow-y-auto space-y-1 p-2 bg-black bg-opacity-30 rounded">
                    {monitoringData.songUpdateStats.newSongs.map((title: string, idx: number) => (
                      <p key={idx} className="border-b border-gray-800 pb-1 last:border-0 text-gray-300 italic">{title}</p>
                    ))}
                  </div>
                </details>
              )}
              
              {monitoringData.songUpdateStats.excludedSongs?.length > 0 && (
                <details className="text-xs bg-gray-900 rounded-md p-2">
                  <summary className="cursor-pointer text-orange-400 hover:text-white transition font-bold">제외된 노래 상세 사유 보기 ({monitoringData.songUpdateStats.excludedSongs.length})</summary>
                  <div className="mt-2 max-h-40 overflow-y-auto space-y-1 p-2 bg-black bg-opacity-30 rounded text-gray-400">
                    {monitoringData.songUpdateStats.excludedSongs.map((info: string, idx: number) => (
                      <p key={idx} className="border-b border-gray-800 pb-1 last:border-0">{info}</p>
                    ))}
                  </div>
                </details>
              )}
            </div>

            {/* 성능 병목 지점 섹션 */}
            {monitoringData.songUpdateStats.slowestChannels?.length > 0 && (
              <div className="bg-gray-900 p-4 rounded-md">
                <h3 className="text-xs font-bold text-red-400 mb-2 uppercase tracking-wider flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                  Performance Bottlenecks (Slowest 20%)
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                  {monitoringData.songUpdateStats.slowestChannels.map((item: any, idx: number) => (
                    <div key={idx} className="bg-gray-800 p-2 rounded border-l-2 border-red-500 flex justify-between items-center">
                      <div>
                        <p className="text-xs font-bold truncate max-w-[120px]">{item.name}</p>
                        <p className="text-[10px] text-gray-500">{item.type}</p>
                      </div>
                      <span className="text-xs font-mono text-red-300">{(item.durationMs / 1000).toFixed(1)}s</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : (
          <p className="text-center py-4 text-gray-500 italic">데이터를 불러오는 중입니다...</p>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* 방문자 통계 섹션 */}
        <div className="bg-gray-700 p-6 rounded-md">
        <h2 className="text-xl font-semibold mb-4">사이트 통계</h2>
          <Link href="/admin/visitors" className="block w-full bg-blue-500 text-white font-bold py-3 px-4 rounded hover:bg-blue-600 transition text-center">
            방문자 통계 보기
          </Link>
        </div>

        {/* 버튜버 추가 섹션 */}
        <div className="bg-gray-700 p-6 rounded-md">
          <h2 className="text-xl font-semibold mb-4">버튜버 수동 추가</h2>
          <form onSubmit={handleAddVtuber} className="flex flex-col gap-4">
            <input
              type="text"
              value={channelId}
              onChange={(e) => setChannelId(e.target.value)}
              placeholder="YouTube 채널 ID"
              className="p-3 bg-gray-900 rounded border border-gray-600 focus:outline-none focus:ring-2 focus:ring-[#A6E22E] transition text-sm"
              disabled={isSubmittingVtuber}
            />
            <button
              type="submit"
              className="bg-[#A6E22E] text-gray-900 font-bold py-3 rounded hover:bg-lime-400 transition disabled:bg-gray-500"
              disabled={isSubmittingVtuber}
            >
              {isSubmittingVtuber ? '추가 중...' : '버튜버 추가'}
            </button>
          </form>
          {addVtuberMessage && (
            <p className="mt-4 text-xs text-center p-3 rounded bg-gray-600">{addVtuberMessage}</p>
          )}
        </div>
      </div>

      {/* 캐시 관리 섹션 */}
      <div className="bg-gray-700 p-6 rounded-md mt-8">
        <h2 className="text-xl font-semibold mb-4">캐시 관리</h2>
        <div className="flex flex-col gap-4">
          <p className="text-sm text-gray-400">메인 페이지의 모든 캐시를 삭제하고 새로고침합니다. (주기적으로 자동 실행되지만, 즉시 반영이 필요할 때 사용)</p>
          <button
            onClick={handleCacheRefresh}
            className="bg-orange-500 text-white font-bold py-3 rounded hover:bg-orange-400 transition disabled:bg-gray-500"
            disabled={isRefreshingCache}
          >
            {isRefreshingCache ? '새로고침 중...' : '메인 페이지 캐시 새로고침'}
          </button>
        </div>
        {cacheMessage && (
          <p className="mt-4 text-center p-3 rounded bg-gray-600">{cacheMessage}</p>
        )}
      </div>
    </div>
  );
};

export default AdminPage;
