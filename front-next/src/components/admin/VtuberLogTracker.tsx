"use client";

import React, { useState, useEffect } from 'react';

const VtuberLogTracker = () => {
  const [logs, setLogs] = useState<any[]>([]);
  const [searchChannelId, setSearchChannelId] = useState('');
  const [searchTitle, setSearchTitle] = useState('');
  const [searchDecision, setSearchDecision] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  const fetchLogs = async () => {
    setIsLoading(true);
    try {
      let url = `/api/admin/monitoring/vtuber-process-logs?page=${page}&size=20`;
      if (searchChannelId) url += `&channelId=${searchChannelId}`;
      if (searchTitle) url += `&channelTitle=${encodeURIComponent(searchTitle)}`;
      if (searchDecision) url += `&decision=${searchDecision}`;

      const response = await fetch(url, { credentials: 'include' });
      if (response.ok) {
        const data = await response.json();
        setLogs(data.content);
        setTotalPages(data.totalPages);
      }
    } catch (error) {
      console.error('Failed to fetch vtuber process logs', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [page, searchDecision]); 

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchLogs();
  };

  const handleDecisionChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSearchDecision(e.target.value);
    setPage(0);
  };

  return (
    <div className="bg-[#222831] border border-[#393E46] p-6 rounded-lg mt-8 w-full">
      <h2 className="text-xl font-bold mb-4 text-[#A6E22E]">버튜버 수집 히스토리 추적</h2>
      
      <form onSubmit={handleSearch} className="flex flex-wrap gap-4 mb-6">
        <input
          type="text"
          placeholder="채널 ID로 검색"
          value={searchChannelId}
          onChange={(e) => setSearchChannelId(e.target.value)}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 flex-1 min-w-[150px] focus:border-[#A6E22E] outline-none"
        />
        <input
          type="text"
          placeholder="채널명으로 검색"
          value={searchTitle}
          onChange={(e) => setSearchTitle(e.target.value)}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 flex-1 min-w-[200px] focus:border-[#A6E22E] outline-none"
        />
        <select
          value={searchDecision}
          onChange={handleDecisionChange}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 focus:border-[#A6E22E] outline-none text-gray-300 min-w-[120px]"
        >
          <option value="">전체 결과</option>
          <option value="ACCEPTED">ACCEPTED (자동 승인)</option>
          <option value="MANUAL_ADDED">MANUAL_ADDED (수동 추가)</option>
          <option value="REJECTED">REJECTED (제외)</option>
          <option value="DELETED">DELETED (삭제됨)</option>
        </select>
        <button
          type="submit"
          className="bg-[#A6E22E] text-gray-900 hover:bg-lime-400 px-6 py-2 rounded transition font-bold"
        >
          검색
        </button>
      </form>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse min-w-[800px]">
          <thead>
            <tr className="border-b border-gray-700 bg-[#393E46]">
              <th className="p-3 w-40">일시</th>
              <th className="p-3">채널명</th>
              <th className="p-3 w-48">채널 ID</th>
              <th className="p-3 w-56">결과 / 사유</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={4} className="p-8 text-center">불러오는 중...</td></tr>
            ) : logs.length === 0 ? (
              <tr><td colSpan={4} className="p-8 text-center text-gray-500">데이터가 없습니다.</td></tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="border-b border-gray-800 hover:bg-[#393E46]/50 transition">
                  <td className="p-3 text-sm whitespace-nowrap">{new Date(log.processedAt).toLocaleString()}</td>
                  <td className="p-3 text-sm max-w-xs truncate" title={log.channelTitle}>{log.channelTitle || '알 수 없음'}</td>
                  <td className="p-3 text-sm font-mono text-gray-400">{log.channelId}</td>
                  <td className="p-3">
                    <span className={`px-2 py-1 rounded text-xs font-bold inline-block mb-1 ${
                      log.decision === 'ACCEPTED' ? 'bg-green-900 text-green-300' : 
                      log.decision === 'MANUAL_ADDED' ? 'bg-blue-900 text-blue-300' :
                      log.decision === 'DELETED' ? 'bg-orange-900 text-orange-300' :
                      'bg-red-900 text-red-300'
                    }`}>
                      {log.decision}
                    </span>
                    <p className="text-[10px] text-gray-500 max-w-[200px] truncate" title={log.reason}>
                      {log.reason}
                    </p>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 rounded bg-[#393E46] hover:bg-gray-600 disabled:opacity-50"
          >
            이전
          </button>
          <span className="px-4 py-2">{page + 1} / {totalPages}</span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page === totalPages - 1}
            className="px-4 py-2 rounded bg-[#393E46] hover:bg-gray-600 disabled:opacity-50"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
};

export default VtuberLogTracker;
