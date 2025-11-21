"use client";

import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';

const AdminPage = () => {
  const { user, isLoading } = useAuth();

  // 버튜버 추가 상태
  const [channelId, setChannelId] = useState('');
  const [addVtuberMessage, setAddVtuberMessage] = useState('');
  const [isSubmittingVtuber, setIsSubmittingVtuber] = useState(false);

  // 캐시 새로고침 상태
  const [cacheMessage, setCacheMessage] = useState('');
  const [isRefreshingCache, setIsRefreshingCache] = useState(false);

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
        setChannelId(''); // 성공 시 입력 필드 초기화
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
    <div className="max-w-2xl mx-auto p-8 bg-gray-800 text-white rounded-lg shadow-lg">
      <h1 className="text-3xl font-bold mb-6 text-center text-[#A6E22E]">관리자 페이지</h1>
      
      {/* 버튜버 추가 섹션 */}
      <div className="bg-gray-700 p-6 rounded-md mb-8">
        <h2 className="text-xl font-semibold mb-4">버튜버 수동 추가</h2>
        <form onSubmit={handleAddVtuber} className="flex flex-col gap-4">
          <input
            type="text"
            value={channelId}
            onChange={(e) => setChannelId(e.target.value)}
            placeholder="YouTube 채널 ID를 입력하세요 (예: UCO_aKKYxn4tvrqPjcTzZ6EQ)"
            className="p-3 bg-gray-900 rounded border border-gray-600 focus:outline-none focus:ring-2 focus:ring-[#A6E22E] transition"
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
          <p className="mt-4 text-center p-3 rounded bg-gray-600">{addVtuberMessage}</p>
        )}
      </div>

      {/* 캐시 관리 섹션 */}
      <div className="bg-gray-700 p-6 rounded-md">
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
