"use client";

import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';

const AdminPage = () => {
  const { user, isLoading } = useAuth();

  const [cacheMessage, setCacheMessage] = useState('');
  const [isRefreshingCache, setIsRefreshingCache] = useState(false);

  const [monitoringData, setMonitoringData] = useState<any>(null);
  const [isLoadingMonitoring, setIsLoadingMonitoring] = useState(false);
  const [isUpdatingSongs, setIsUpdatingSongs] = useState(false);
  const [isUpdatingVtubers, setIsUpdatingVtubers] = useState(false);
  const [isCleaningTitles, setIsCleaningTitles] = useState(false);

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

  const handleRunSongUpdate = async () => {
    if (!confirm('노래 수집을 즉시 실행하시겠습니까? (버튜버 수에 따라 수 분이 소요될 수 있습니다)')) return;
    
    setIsUpdatingSongs(true);
    try {
      const response = await fetch('/api/admin/monitoring/run-song-update', {
        method: 'POST',
        headers: {
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        credentials: 'include',
      });
      const data = await response.json();
      if (response.ok) {
        alert(data.message || '노래 수집 작업이 시작되었습니다.');
      } else {
        alert('노래 수집 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('Failed to run song update', error);
      alert('네트워크 오류가 발생했습니다.');
    } finally {
      setIsUpdatingSongs(false);
    }
  };

  const handleCleanTitles = async () => {
    if (!confirm('미정제된 모든 노래 제목을 AI로 정제하시겠습니까? (노래 수에 따라 시간이 소요될 수 있습니다)')) return;
    
    setIsCleaningTitles(true);
    try {
      const response = await fetch('/api/admin/vtubers/clean-titles', {
        method: 'POST',
        headers: {
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        credentials: 'include',
      });
      const resultText = await response.text();
      if (response.ok) {
        alert(resultText || '제목 정제 작업이 완료되었습니다.');
      } else {
        alert('제목 정제 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('Failed to clean titles', error);
      alert('네트워크 오류가 발생했습니다.');
    } finally {
      setIsCleaningTitles(false);
    }
  };

  const handleRunVtuberUpdate = async () => {
    if (!confirm('버튜버 수집 및 동기화를 즉시 실행하시겠습니까?')) return;
    
    setIsUpdatingVtubers(true);
    try {
      const response = await fetch('/api/admin/monitoring/run-vtuber-update', {
        method: 'POST',
        headers: {
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        credentials: 'include',
      });
      const data = await response.json();
      if (response.ok) {
        alert(data.message || '버튜버 수집 작업이 시작되었습니다.');
      } else {
        alert('버튜버 수집 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('Failed to run vtuber update', error);
      alert('네트워크 오류가 발생했습니다.');
    } finally {
      setIsUpdatingVtubers(false);
    }
  };

  React.useEffect(() => {
    if (user?.role === 'ADMIN') {
      fetchMonitoringData();
    }
  }, [user]);

  const getCsrfToken = () => {
    if (typeof document === 'undefined') return '';
    const name = 'XSRF-TOKEN=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
      let c = ca[i];
      while (c.charAt(0) === ' ') {
        c = c.substring(1);
      }
      if (c.indexOf(name) === 0) {
        return c.substring(name.length, c.length);
      }
    }
    return '';
  };

  const handleCacheRefresh = async () => {
    setIsRefreshingCache(true);
    setCacheMessage('');

    try {
      const response = await fetch('/api/admin/cache/refresh-main-page', {
        method: 'POST',
        headers: {
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        credentials: 'include',
      });

      const resultText = await response.text();

      if (response.ok) {
        setCacheMessage(`성공: ${resultText}`);
      } else {
        setCacheMessage(`오류: ${resultText}`);
      }
    } catch {
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
          <div className="flex flex-wrap gap-2 justify-end">
            <button 
              onClick={handleRunVtuberUpdate} 
              className="text-xs bg-cyan-600 text-white hover:bg-cyan-500 px-3 py-1 rounded transition font-bold disabled:bg-gray-500"
              disabled={isUpdatingVtubers}
            >
              {isUpdatingVtubers ? '수집 진행 중...' : '버튜버 수집 즉시 실행'}
            </button>
            <button 
              onClick={handleRunSongUpdate} 
              className="text-xs bg-[#A6E22E] text-gray-900 hover:bg-lime-400 px-3 py-1 rounded transition font-bold disabled:bg-gray-500"
              disabled={isUpdatingSongs}
            >
              {isUpdatingSongs ? '수집 진행 중...' : '노래 수집 즉시 실행'}
            </button>
            <button 
              onClick={handleCleanTitles} 
              className="text-xs bg-purple-600 text-white hover:bg-purple-500 px-3 py-1 rounded transition font-bold disabled:bg-gray-500"
              disabled={isCleaningTitles}
            >
              {isCleaningTitles ? '정제 진행 중...' : '노래 제목 일괄 정제'}
            </button>
            <Link 
              href="/admin/vtubers" 
              className="text-xs bg-blue-600 text-white hover:bg-blue-500 px-3 py-1 rounded transition font-bold text-center h-[26px] flex items-center justify-center"
            >
              버튜버 통합 관리
            </Link>
            <button 
              onClick={fetchMonitoringData} 
              className="text-xs bg-gray-600 hover:bg-gray-500 px-3 py-1 rounded transition"
              disabled={isLoadingMonitoring}
            >
              {isLoadingMonitoring ? '갱신 중...' : '모니터링 새로고침'}
            </button>
          </div>
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
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-gray-900 p-4 rounded-md">
                <h3 className="text-xs font-bold text-gray-400 mb-2 uppercase">최근 노래 수집 요약</h3>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-gray-400">실행 시간:</span>
                    <span className="text-[10px]">{monitoringData.songUpdateStats.lastRunTime ? new Date(monitoringData.songUpdateStats.lastRunTime).toLocaleString() : '기록 없음'}</span>
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
                <h3 className="text-xs font-bold text-gray-400 mb-2 uppercase text-cyan-400">최근 버튜버 수집 요약</h3>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-gray-400">실행 시간:</span>
                    <span className="text-[10px]">{monitoringData.vtuberUpdateStats.lastRunTime ? new Date(monitoringData.vtuberUpdateStats.lastRunTime).toLocaleString() : '기록 없음'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">신규 버튜버:</span>
                    <span className="text-cyan-400 font-bold">{monitoringData.vtuberUpdateStats.newVtubersCount || 0}명</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">정보 갱신:</span>
                    <span className="text-blue-400 font-bold">{monitoringData.vtuberUpdateStats.updatedVtubersCount || 0}명</span>
                  </div>
                </div>
              </div>

              <div className="bg-gray-900 p-4 rounded-md">
                <h3 className="text-xs font-bold text-gray-400 mb-2 uppercase text-yellow-400">최근 조회수 업데이트 요약</h3>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-gray-400">실행 시간:</span>
                    <span className="text-[10px]">{monitoringData.viewUpdateStats.lastRunTime ? new Date(monitoringData.viewUpdateStats.lastRunTime).toLocaleString() : '기록 없음'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">업데이트:</span>
                    <span className="text-yellow-400 font-bold">{monitoringData.viewUpdateStats.updatedCount || 0}개</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">전체 노래:</span>
                    <span className="text-gray-300 font-bold">{monitoringData.viewUpdateStats.totalSongsCount || 0}개</span>
                  </div>
                </div>
              </div>

              <div className="bg-gray-900 p-4 rounded-md border-l-2 border-indigo-500">
                <h3 className="text-xs font-bold text-indigo-400 mb-2 uppercase">AI 추천 통계</h3>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-gray-400">총 요청:</span>
                    <span className="text-white font-bold">{monitoringData.aiRecommendationStats.totalRequests || 0}회</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400 text-[10px]">성공:</span>
                    <span className="text-green-400 font-bold text-[10px]">{monitoringData.aiRecommendationStats.successCount || 0}회</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400 text-[10px]">실패:</span>
                    <span className="text-red-400 font-bold text-[10px]">{monitoringData.aiRecommendationStats.failCount || 0}회</span>
                  </div>
                  <div className="flex justify-between pt-1 border-t border-gray-800">
                    <span className="text-gray-500 text-[9px]">성공률:</span>
                    <span className="text-indigo-300 text-[9px]">
                      {monitoringData.aiRecommendationStats.totalRequests > 0 
                        ? ((monitoringData.aiRecommendationStats.successCount / monitoringData.aiRecommendationStats.totalRequests) * 100).toFixed(1)
                        : 0}%
                    </span>
                  </div>
                </div>
              </div>

              <div className="bg-gray-900 p-4 rounded-md">
                <h3 className="text-xs font-bold text-gray-400 mb-2 uppercase">서버 상태</h3>
                <div className="flex items-center gap-2">
                  <div className={`w-3 h-3 rounded-full ${monitoringData.systemHealth === 'UP' ? 'bg-green-500 shadow-[0_0_8px_#22c55e]' : 'bg-red-500'}`}></div>
                  <span className="font-bold">{monitoringData.systemHealth}</span>
                </div>
                <div className="mt-4 pt-4 border-t border-gray-800">
                   <p className="text-[10px] text-gray-500">데이터는 매일 자정 전후로 자동 갱신됩니다.</p>
                </div>
              </div>
            </div>
            
            {/* 과거 이력 (History) 섹션 */}
            <div className="space-y-4">
              <details className="text-xs bg-gray-900 rounded-md p-3 border border-gray-800">
                <summary className="cursor-pointer text-gray-300 hover:text-white transition font-bold flex justify-between items-center">
                  <span>AI 취향 분석 상세 이력 (최근 100건)</span>
                  <span className="text-[10px] bg-gray-800 px-2 py-0.5 rounded">클릭하여 펼치기</span>
                </summary>
                <div className="mt-3 overflow-x-auto max-h-60 overflow-y-auto">
                  <table className="w-full text-[10px] text-left">
                    <thead className="sticky top-0 bg-gray-900">
                      <tr className="text-gray-500 border-b border-gray-800">
                        <th className="pb-2">요청 시간</th>
                        <th className="pb-2">사용자</th>
                        <th className="pb-2 text-right">소요</th>
                        <th className="pb-2">상태</th>
                        <th className="pb-2">결과/에러</th>
                      </tr>
                    </thead>
                    <tbody>
                      {monitoringData.aiRecommendationHistory?.map((log: any) => (
                        <tr key={log.id} className="border-b border-gray-800 last:border-0 hover:bg-gray-800/50 transition">
                          <td className="py-2 text-gray-400 whitespace-nowrap">{new Date(log.requestedAt).toLocaleString()}</td>
                          <td className="py-2 text-gray-300 max-w-[120px] truncate" title={log.userEmail}>{log.userEmail}</td>
                          <td className="py-2 text-right text-indigo-400">{log.responseTimeMs}ms</td>
                          <td className="py-2">
                            {log.success 
                              ? <span className="text-green-500">SUCCESS</span> 
                              : <span className="text-red-500">FAIL</span>}
                          </td>
                          <td className="py-2 text-gray-500 max-w-[200px] truncate" title={log.success ? log.resultKeywords : log.errorMessage}>
                            {log.success ? log.resultKeywords : log.errorMessage}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </details>

              <details className="text-xs bg-gray-900 rounded-md p-3 border border-gray-800">
                <summary className="cursor-pointer text-gray-300 hover:text-white transition font-bold flex justify-between items-center">
                  <span>과거 조회수 업데이트 상세 이력 (최근 10건)</span>
                  <span className="text-[10px] bg-gray-800 px-2 py-0.5 rounded">클릭하여 펼치기</span>
                </summary>
                <div className="mt-3 overflow-x-auto">
                  <table className="w-full text-[10px] text-left">
                    <thead>
                      <tr className="text-gray-500 border-b border-gray-800">
                        <th className="pb-2">실행 시간</th>
                        <th className="pb-2 text-right">소요</th>
                        <th className="pb-2 text-right text-yellow-400">성공</th>
                        <th className="pb-2 text-right text-orange-400">삭제</th>
                        <th className="pb-2 text-right text-red-500">실패</th>
                      </tr>
                    </thead>
                    <tbody>
                      {monitoringData.viewUpdateHistory?.map((log: any) => (
                        <tr key={log.id} className="border-b border-gray-800 last:border-0 hover:bg-gray-800/50 transition">
                          <td className="py-2 text-gray-400">{new Date(log.runTime).toLocaleString()}</td>
                          <td className="py-2 text-right text-gray-500">{log.durationSeconds}s</td>
                          <td className="py-2 text-right font-bold text-yellow-400">{log.updatedCount}</td>
                          <td className="py-2 text-right text-orange-400">{log.deletedCount}</td>
                          <td className="py-2 text-right text-red-400">{log.failedCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </details>

              <details className="text-xs bg-gray-900 rounded-md p-3 border border-gray-800">
                <summary className="cursor-pointer text-gray-300 hover:text-white transition font-bold flex justify-between items-center">
                  <span>과거 노래 수집 상세 내역 (최근 10건)</span>
                  <span className="text-[10px] bg-gray-800 px-2 py-0.5 rounded">클릭하여 펼치기</span>
                </summary>
                <div className="mt-3 overflow-x-auto">
                  <table className="w-full text-[10px] text-left">
                    <thead>
                      <tr className="text-gray-500 border-b border-gray-800">
                        <th className="pb-2">실행 시간</th>
                        <th className="pb-2 text-right">소요</th>
                        <th className="pb-2 text-right text-[#A6E22E]">추가</th>
                        <th className="pb-2 text-right text-orange-400">제외</th>
                        <th className="pb-2 text-right text-red-500">실패</th>
                      </tr>
                    </thead>
                    <tbody>
                      {monitoringData.songUpdateHistory?.map((log: any) => (
                        <tr key={log.id} className="border-b border-gray-800 last:border-0 hover:bg-gray-800/50 transition">
                          <td className="py-2">
                            <Link href={`/admin/monitoring/song-log/${log.id}`} className="text-blue-400 hover:text-blue-300 transition">
                              {new Date(log.runTime).toLocaleString()}
                            </Link>
                          </td>
                          <td className="py-2 text-right text-gray-500">{log.durationSeconds}s</td>
                          <td className="py-2 text-right font-bold">{log.newSongsCount}</td>
                          <td className="py-2 text-right">{log.excludedSongsCount}</td>
                          <td className="py-2 text-right text-red-400">{log.failedSongsCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </details>

              <details className="text-xs bg-gray-900 rounded-md p-3 border border-gray-800">
                <summary className="cursor-pointer text-gray-300 hover:text-white transition font-bold flex justify-between items-center">
                  <span>과거 버튜버 수집 상세 내역 (최근 10건)</span>
                  <span className="text-[10px] bg-gray-800 px-2 py-0.5 rounded">클릭하여 펼치기</span>
                </summary>
                <div className="mt-3 overflow-x-auto">
                  <table className="w-full text-[10px] text-left">
                    <thead>
                      <tr className="text-gray-500 border-b border-gray-800">
                        <th className="pb-2">실행 시간</th>
                        <th className="pb-2 text-right">소요</th>
                        <th className="pb-2 text-right text-cyan-400">신규</th>
                        <th className="pb-2 text-right text-blue-400">갱신</th>
                        <th className="pb-2 text-right text-red-400">삭제</th>
                      </tr>
                    </thead>
                    <tbody>
                      {monitoringData.vtuberUpdateHistory?.map((log: any) => (
                        <tr key={log.id} className="border-b border-gray-800 last:border-0 hover:bg-gray-800/50 transition">
                          <td className="py-2">
                            <Link href={`/admin/monitoring/vtuber-log/${log.id}`} className="text-cyan-400 hover:text-cyan-300 transition">
                              {new Date(log.runTime).toLocaleString()}
                            </Link>
                          </td>
                          <td className="py-2 text-right text-gray-500">{log.durationSeconds}s</td>
                          <td className="py-2 text-right font-bold text-cyan-400">{log.newVtubersCount}</td>
                          <td className="py-2 text-right text-blue-400">{log.updatedVtubersCount}</td>
                          <td className="py-2 text-right text-red-400">{log.deletedVtubersCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </details>
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
              
              {monitoringData.songUpdateStats.failedSongs?.length > 0 && (
                <details className="text-xs bg-gray-900 rounded-md p-2 col-span-1 md:col-span-2 border border-red-900">
                  <summary className="cursor-pointer text-red-500 hover:text-white transition font-bold">수집 실패(시스템 오류) 목록 보기 ({monitoringData.songUpdateStats.failedSongs.length})</summary>
                  <div className="mt-2 max-h-40 overflow-y-auto space-y-1 p-2 bg-black bg-opacity-30 rounded text-red-300 font-mono">
                    {monitoringData.songUpdateStats.failedSongs.map((info: string, idx: number) => (
                      <p key={idx} className="border-b border-gray-800 pb-1 last:border-0">{info}</p>
                    ))}
                  </div>
                </details>
              )}
            </div>

            {/* 성능 병목 지점 섹션 */}
            {monitoringData.songUpdateStats.slowestChannels?.length > 0 && (
              <details className="text-xs bg-gray-900 rounded-md p-3 border border-gray-800">
                <summary className="cursor-pointer text-red-400 hover:text-white transition font-bold flex justify-between items-center">
                  <span className="flex items-center gap-1">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                    </svg>
                    Performance Bottlenecks (Slowest 20%)
                  </span>
                  <span className="text-[10px] bg-gray-800 px-2 py-0.5 rounded">클릭하여 펼치기</span>
                </summary>
                <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                  {monitoringData.songUpdateStats.slowestChannels.map((item: any, idx: number) => (
                    <div key={idx} className="bg-gray-800 p-2 rounded border-l-2 border-red-500 flex justify-between items-center">
                      <div>
                        <p className="text-xs font-bold truncate max-w-[120px]">{item.name}</p>
                        <p className="text-[9px] text-gray-500">{item.type} {item.addedCount > 0 && `(+${item.addedCount}곡)`}</p>
                        {item.reason && <p className="text-[9px] text-red-400/80 mt-0.5">{item.reason}</p>}
                      </div>
                      <span className="text-xs font-mono text-red-300">{(item.durationMs / 1000).toFixed(1)}s</span>
                    </div>
                  ))}
                </div>
              </details>
            )}
          </div>
        ) : (
          <p className="text-center py-4 text-gray-500 italic">데이터를 불러오는 중입니다...</p>
        )}
      </div>

      <div className="space-y-8">
        {/* 방문자 통계 섹션 */}
        <div className="bg-gray-700 p-6 rounded-md">
          <h2 className="text-xl font-semibold mb-4 text-[#66D9EF]">사이트 통계 및 피드백</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Link href="/admin/visitors" className="block w-full bg-blue-500 text-white font-bold py-3 px-4 rounded hover:bg-blue-600 transition text-center">
              방문자 통계 보기
            </Link>
            <Link href="/admin/suggestions" className="block w-full bg-[#A6E22E] text-gray-900 font-bold py-3 px-4 rounded hover:bg-lime-400 transition text-center">
              건의사항 목록 확인
            </Link>
            <Link href="/admin/history" className="block w-full bg-indigo-500 text-white font-bold py-3 px-4 rounded hover:bg-indigo-400 transition text-center">
              수집 상세 히스토리
            </Link>
          </div>
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
