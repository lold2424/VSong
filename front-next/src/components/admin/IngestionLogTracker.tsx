"use client";

import React, { useState, useEffect } from 'react';

const IngestionLogTracker = () => {
  const [logs, setLogs] = useState<any[]>([]);
  const [searchVideoId, setSearchVideoId] = useState('');
  const [searchTitle, setSearchTitle] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  const fetchLogs = async () => {
    setIsLoading(true);
    try {
      let url = `/api/admin/monitoring/ingestion-logs?page=${page}&size=20`;
      if (searchVideoId) url += `&videoId=${searchVideoId}`;
      if (searchTitle) url += `&title=${encodeURIComponent(searchTitle)}`;

      const response = await fetch(url, { credentials: 'include' });
      if (response.ok) {
        const data = await response.json();
        setLogs(data.content);
        setTotalPages(data.totalPages);
      }
    } catch (error) {
      console.error('Failed to fetch ingestion logs', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [page]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchLogs();
  };

  return (
    <div className="bg-[#222831] border border-[#393E46] p-6 rounded-lg mt-8">
      <h2 className="text-xl font-bold mb-4 text-[#00ADB5]">노래 수집 히스토리 추적</h2>
      
      <form onSubmit={handleSearch} className="flex gap-4 mb-6">
        <input
          type="text"
          placeholder="비디오 ID로 검색"
          value={searchVideoId}
          onChange={(e) => setSearchVideoId(e.target.value)}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 flex-1 focus:border-[#00ADB5] outline-none"
        />
        <input
          type="text"
          placeholder="제목으로 검색"
          value={searchTitle}
          onChange={(e) => setSearchTitle(e.target.value)}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 flex-1 focus:border-[#00ADB5] outline-none"
        />
        <button
          type="submit"
          className="bg-[#00ADB5] hover:bg-[#008c94] text-white px-6 py-2 rounded transition font-bold"
        >
          검색
        </button>
      </form>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-gray-700 bg-[#393E46]">
              <th className="p-3">일시</th>
              <th className="p-3">버튜버</th>
              <th className="p-3">제목</th>
              <th className="p-3">결과</th>
              <th className="p-3">판별 사유</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={5} className="p-8 text-center">불러오는 중...</td></tr>
            ) : logs.length === 0 ? (
              <tr><td colSpan={5} className="p-8 text-center text-gray-500">데이터가 없습니다.</td></tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="border-b border-gray-800 hover:bg-[#393E46]/50 transition">
                  <td className="p-3 text-sm">{new Date(log.processedAt).toLocaleString()}</td>
                  <td className="p-3 text-sm">{log.vtuberName}</td>
                  <td className="p-3 text-sm max-w-xs truncate" title={log.title}>{log.title}</td>
                  <td className="p-3">
                    <span className={`px-2 py-1 rounded text-xs font-bold ${
                      log.decision === 'ACCEPTED' ? 'bg-green-900 text-green-300' : 'bg-red-900 text-red-300'
                    }`}>
                      {log.decision}
                    </span>
                  </td>
                  <td className="p-3 text-sm text-gray-400">{log.reason}</td>
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

export default IngestionLogTracker;
