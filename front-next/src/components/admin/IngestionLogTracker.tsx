"use client";

import React, { useState, useEffect } from 'react';
import ConfirmModal from '@/components/ConfirmModal';
import AlertModal from '@/components/AlertModal';

const IngestionLogTracker = () => {
  const [logs, setLogs] = useState<any[]>([]);
  const [searchVideoId, setSearchVideoId] = useState('');
  const [searchTitle, setSearchTitle] = useState('');
  const [searchDecision, setSearchDecision] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  // 모달 상태 관리
  const [confirmModal, setConfirmModal] = useState({ isOpen: false, videoId: '', title: '' });
  const [alertModal, setAlertModal] = useState({ isOpen: false, title: '', message: '' });

  const fetchLogs = async () => {
    setIsLoading(true);
    try {
      let url = `/api/admin/monitoring/ingestion-logs?page=${page}&size=20`;
      if (searchVideoId) url += `&videoId=${searchVideoId}`;
      if (searchTitle) url += `&title=${encodeURIComponent(searchTitle)}`;
      if (searchDecision) url += `&decision=${searchDecision}`;

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
  }, [page, searchDecision]); // 결과 필터 선택 시 즉시 다시 불러오기

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchLogs();
  };

  const handleDecisionChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSearchDecision(e.target.value);
    setPage(0); // 필터 변경 시 첫 페이지로 이동
  };

  const openDeleteConfirm = (videoId: string, title: string) => {
    setConfirmModal({ isOpen: true, videoId, title });
  };

  const handleDeleteSong = async () => {
    const { videoId } = confirmModal;
    setConfirmModal({ ...confirmModal, isOpen: false });

    try {
      const response = await fetch(`/api/admin/vtubers/songs/${videoId}`, {
        method: 'DELETE',
        headers: {
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        credentials: 'include',
      });

      if (response.ok) {
        setAlertModal({ isOpen: true, title: '삭제 완료', message: '노래가 성공적으로 삭제되었습니다.' });
        fetchLogs();
      } else {
        const errorText = await response.text();
        setAlertModal({ isOpen: true, title: '삭제 실패', message: errorText });
      }
    } catch (error) {
      console.error('Delete error', error);
      setAlertModal({ isOpen: true, title: '오류', message: '네트워크 오류가 발생했습니다.' });
    }
  };

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

  return (
    <div className="bg-[#222831] border border-[#393E46] p-6 rounded-lg mt-8 w-full">
      <h2 className="text-xl font-bold mb-4 text-[#00ADB5]">노래 수집 히스토리 추적</h2>
      
      <form onSubmit={handleSearch} className="flex flex-wrap gap-4 mb-6">
        <input
          type="text"
          placeholder="비디오 ID로 검색"
          value={searchVideoId}
          onChange={(e) => setSearchVideoId(e.target.value)}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 flex-1 min-w-[150px] focus:border-[#00ADB5] outline-none"
        />
        <input
          type="text"
          placeholder="제목으로 검색"
          value={searchTitle}
          onChange={(e) => setSearchTitle(e.target.value)}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 flex-1 min-w-[200px] focus:border-[#00ADB5] outline-none"
        />
        <select
          value={searchDecision}
          onChange={handleDecisionChange}
          className="bg-[#393E46] border border-gray-600 rounded px-3 py-2 focus:border-[#00ADB5] outline-none text-gray-300 min-w-[120px]"
        >
          <option value="">전체 결과</option>
          <option value="ACCEPTED">ACCEPTED (승인)</option>
          <option value="REJECTED">REJECTED (제외)</option>
          <option value="DELETED">DELETED (삭제됨)</option>
        </select>
        <button
          type="submit"
          className="bg-[#00ADB5] hover:bg-[#008c94] text-white px-6 py-2 rounded transition font-bold"
        >
          검색
        </button>
      </form>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse min-w-[800px]">
          <thead>
            <tr className="border-b border-gray-700 bg-[#393E46]">
              <th className="p-3 w-40">일시</th>
              <th className="p-3 w-32">버튜버</th>
              <th className="p-3">제목</th>
              <th className="p-3 w-40">결과 / 사유</th>
              <th className="p-3 w-20 text-center">관리</th>
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
                  <td className="p-3 text-sm whitespace-nowrap">{new Date(log.processedAt).toLocaleString()}</td>
                  <td className="p-3 text-sm whitespace-nowrap">{log.vtuberName}</td>
                  <td className="p-3 text-sm max-w-md truncate" title={log.title}>{log.title}</td>
                  <td className="p-3">
                    <span className={`px-2 py-1 rounded text-xs font-bold inline-block mb-1 ${
                      log.decision === 'ACCEPTED' ? 'bg-green-900 text-green-300' : 
                      log.decision === 'DELETED' ? 'bg-orange-900 text-orange-300' :
                      'bg-red-900 text-red-300'
                    }`}>
                      {log.decision}
                    </span>
                    <p className="text-[10px] text-gray-500 max-w-[200px] truncate" title={log.reason || (log.decision === 'DELETED' ? '관리자에 의해 삭제됨' : '')}>
                      {log.reason || (log.decision === 'DELETED' ? '관리자에 의해 삭제됨' : '')}
                    </p>
                  </td>
                  <td className="p-3 text-center whitespace-nowrap">
                    {log.decision === 'ACCEPTED' && (
                      <button
                        onClick={() => openDeleteConfirm(log.videoId, log.title)}
                        className="text-xs bg-red-600 hover:bg-red-500 text-white px-3 py-1.5 rounded transition font-bold"
                      >
                        삭제
                      </button>
                    )}
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

      {/* 커스텀 모달들 */}
      <ConfirmModal 
        isOpen={confirmModal.isOpen}
        title="노래 삭제 확인"
        message={`[${confirmModal.title}] 노래를 정말로 삭제하시겠습니까? 이 작업은 되돌릴 수 없으며 모든 관련 데이터가 삭제됩니다.`}
        confirmLabel="삭제하기"
        cancelLabel="취소"
        isDanger={true}
        onConfirm={handleDeleteSong}
        onCancel={() => setConfirmModal({ ...confirmModal, isOpen: false })}
      />

      <AlertModal 
        isOpen={alertModal.isOpen}
        title={alertModal.title}
        message={alertModal.message}
        onClose={() => setAlertModal({ ...alertModal, isOpen: false })}
      />
    </div>
  );
};

export default IngestionLogTracker;
