"use client";

import React, { useEffect, useState } from 'react';
import { getAdminSuggestions } from '@/utils/apiClient';
import Link from 'next/link';

interface Suggestion {
  id: number;
  content: string;
  userEmail: string;
  userName: string;
  createdAt: string;
  status: string;
}

export default function AdminSuggestionsPage() {
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSuggestions = async () => {
      try {
        const data = await getAdminSuggestions();
        setSuggestions(data);
      } catch (err) {
        setError('건의사항 목록을 불러오는 중 오류가 발생했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchSuggestions();
  }, []);

  return (
    <div className="p-8 bg-[#272822] min-h-screen text-[#F8F8F2]">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-8 border-b border-[#3E3D32] pb-6">
          <div>
            <h1 className="text-3xl font-black text-[#A6E22E] tracking-tighter uppercase">Suggestion Management</h1>
            <p className="text-sm text-gray-400 mt-2 font-mono">User feedback and improvement ideas</p>
          </div>
          <Link 
            href="/admin"
            className="px-4 py-2 bg-[#3E3D32] text-[#66D9EF] rounded-lg hover:bg-[#49483E] transition-all text-sm font-bold border border-[#49483E]"
          >
            대시보드로 돌아가기
          </Link>
        </div>

        {isLoading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#A6E22E]"></div>
          </div>
        ) : error ? (
          <div className="bg-red-500/10 border border-red-500/20 p-6 rounded-xl text-red-400 text-center">
            {error}
          </div>
        ) : suggestions.length === 0 ? (
          <div className="bg-[#1e1f1c] border border-[#3E3D32] p-12 rounded-xl text-center">
            <p className="text-gray-500">접수된 건의사항이 없습니다.</p>
          </div>
        ) : (
          <div className="grid gap-4">
            {suggestions.map((suggestion) => (
              <div key={suggestion.id} className="bg-[#1e1f1c] border border-[#3E3D32] rounded-xl overflow-hidden hover:border-[#66D9EF]/50 transition-all group">
                <div className="p-5 border-b border-[#3E3D32] flex justify-between items-center bg-[#272822]/50">
                  <div className="flex items-center gap-4">
                    <span className="px-2 py-1 bg-[#3E3D32] text-[#66D9EF] text-[10px] font-bold rounded uppercase tracking-widest border border-[#49483E]">
                      ID: {suggestion.id}
                    </span>
                    <span className="text-sm font-bold text-[#A6E22E]">
                      {suggestion.userName || '익명'} ({suggestion.userEmail || '이메일 없음'})
                    </span>
                  </div>
                  <span className="text-xs text-gray-500 font-mono italic">
                    {new Date(suggestion.createdAt).toLocaleString('ko-KR')}
                  </span>
                </div>
                <div className="p-6">
                  <p className="text-[#F8F8F2] leading-relaxed whitespace-pre-wrap">
                    {suggestion.content}
                  </p>
                </div>
                <div className="px-5 py-3 bg-[#1e1f1c] border-t border-[#3E3D32] flex justify-end">
                   <span className={`text-[10px] font-black uppercase tracking-widest px-2 py-0.5 rounded border ${
                     suggestion.status === 'PENDING' ? 'text-yellow-500 border-yellow-500/30' : 'text-[#A6E22E] border-[#A6E22E]/30'
                   }`}>
                     {suggestion.status}
                   </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
