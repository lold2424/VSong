"use client";

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';

const VtuberLogDetailPage = () => {
  const { id } = useParams();
  const router = useRouter();
  const [log, setLog] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchLog = async () => {
      try {
        const response = await fetch(`/api/admin/monitoring/vtuber-log/${id}`, {
          credentials: 'include',
        });
        if (response.ok) {
          const data = await response.json();
          if (data.logDetailsJson) data.details = JSON.parse(data.logDetailsJson);
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
        <h1 className="text-2xl font-bold text-cyan-400">버튜버 수집 상세 내역</h1>
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
          <p className="text-xs text-gray-400 uppercase">신규 / 갱신 / 삭제</p>
          <p className="text-xl font-bold">
            <span className="text-cyan-400">{log.newVtubersCount}</span> / 
            <span className="text-blue-400"> {log.updatedVtubersCount}</span> / 
            <span className="text-red-400"> {log.deletedVtubersCount}</span>
          </p>
        </div>
        <div className="bg-gray-900 p-4 rounded-md">
          <p className="text-xs text-gray-400 uppercase">상태</p>
          <p className="text-sm font-bold text-green-400">SUCCESS</p>
        </div>
      </div>

      <div className="bg-gray-900 p-6 rounded-md border border-gray-700">
        <h2 className="text-lg font-bold mb-4 text-gray-300 uppercase tracking-wider">작업 요약</h2>
        <p className="text-gray-300 italic mb-4">"{log.logSummary}"</p>
        
        {log.details ? (
          <div className="space-y-4">
             {/* 상세 내역이 리스트 형태로 저장되어 있다면 여기에 렌더링 */}
             <p className="text-xs text-gray-500">상세 내역은 이후 수집부터 기록됩니다.</p>
          </div>
        ) : (
          <p className="text-gray-500 text-sm">상세 로그 데이터가 없습니다.</p>
        )}
      </div>
    </div>
  );
};

export default VtuberLogDetailPage;
