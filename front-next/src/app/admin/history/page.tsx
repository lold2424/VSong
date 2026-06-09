"use client";

import React from 'react';
import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';
import IngestionLogTracker from '@/components/admin/IngestionLogTracker';
import VtuberLogTracker from '@/components/admin/VtuberLogTracker';

const HistoryAdminPage = () => {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return <div className="text-center p-10 text-white">로딩 중...</div>;
  }

  if (user?.role !== 'ADMIN') {
    return (
      <div className="text-center p-10 text-white">
        <h1 className="text-2xl text-red-500 mb-4">접근 권한 없음</h1>
        <Link href="/" className="text-blue-400 hover:underline">홈으로 돌아가기</Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-8 bg-gray-800 text-white rounded-lg shadow-lg min-h-screen">
      <div className="flex justify-between items-center mb-8">
        <div>
          <Link href="/admin" className="text-sm text-gray-400 hover:text-white transition">← 관리자 대시보드로 돌아가기</Link>
          <h1 className="text-3xl font-bold text-[#66D9EF] mt-2">수집 상세 히스토리 추적</h1>
        </div>
      </div>

      <p className="text-sm text-gray-400 mb-8">
        버튜버 및 노래가 수집 파이프라인에서 어떻게 판정(승인/제외/실패)되었는지 개별 건에 대한 상세 로그를 검색하고 확인할 수 있습니다.
      </p>

      <div className="flex flex-col gap-12">
        <VtuberLogTracker />
        <IngestionLogTracker />
      </div>
    </div>
  );
};

export default HistoryAdminPage;
