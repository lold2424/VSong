"use client";

import React, { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';
import ConfirmModal from '@/components/ConfirmModal';
import AlertModal from '@/components/AlertModal';

interface Vtuber {
  channelId: string;
  name: string;
  description: string;
  subscribers: number;
  channelImg: string;
  addedTime: string;
  gender?: string;
}

const VtuberAdminPage = () => {
  const { user, isLoading: isAuthLoading } = useAuth();
  const [vtubers, setVtubers] = useState<Vtuber[]>([]);
  const [filteredVtubers, setFilteredVtubers] = useState<Vtuber[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isDeleting, setIsDeleting] = useState<string | null>(null);

  // 모달 상태 관리
  const [confirmModal, setConfirmModal] = useState({ isOpen: false, vtuber: null as Vtuber | null, title: '', message: '' });
  const [alertModal, setAlertModal] = useState({ isOpen: false, title: '', message: '' });

  // 정렬을 위한 상태
  const [sortBy, setSortBy] = useState<'subscribers' | 'addedTime' | 'name' | 'gender'>('subscribers');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  // 수동 추가를 위한 상태
  const [newChannelId, setNewChannelId] = useState('');
  const [newGender, setNewGender] = useState('female');
  const [addMessage, setAddMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      fetchVtubers();
    }
  }, [user]);

  useEffect(() => {
    let results = vtubers.filter(v => 
      v.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
      v.channelId.includes(searchTerm)
    );

    // 정렬 로직 적용
    results.sort((a, b) => {
      let comparison = 0;
      if (sortBy === 'subscribers') {
        comparison = (a.subscribers || 0) - (b.subscribers || 0);
      } else if (sortBy === 'addedTime') {
        comparison = new Date(a.addedTime).getTime() - new Date(b.addedTime).getTime();
      } else if (sortBy === 'name') {
        comparison = a.name.localeCompare(b.name);
      } else if (sortBy === 'gender') {
        comparison = (a.gender || 'unknown').localeCompare(b.gender || 'unknown');
      }
      return sortOrder === 'asc' ? comparison : -comparison;
    });

    setFilteredVtubers(results);
  }, [searchTerm, vtubers, sortBy, sortOrder]);

  const fetchVtubers = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('/api/v1/vtubers', {
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();
        // 구독자 순으로 기본 정렬
        const sortedData = [...data].sort((a, b) => (b.subscribers || 0) - (a.subscribers || 0));
        setVtubers(sortedData);
        setFilteredVtubers(sortedData);
      }
    } catch (error) {
      console.error('Failed to fetch vtubers', error);
    } finally {
      setIsLoading(false);
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

  const handleAddVtuber = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newChannelId.trim()) {
      setAddMessage('채널 ID를 입력해주세요.');
      return;
    }

    setIsSubmitting(true);
    setAddMessage('');

    try {
      const response = await fetch('/api/admin/vtuber', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        body: JSON.stringify({ channelId: newChannelId, gender: newGender }),
        credentials: 'include',
      });

      const resultText = await response.text();

      if (response.ok) {
        setAddMessage(`성공: ${resultText}`);
        setNewChannelId('');
        // 목록 새로고침
        fetchVtubers();
      } else {
        setAddMessage(`오류: ${resultText}`);
      }
    } catch {
      setAddMessage('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateGender = async (channelId: string, newGender: string) => {
    try {
      const response = await fetch(`/api/admin/vtubers/${channelId}/gender`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        body: JSON.stringify({ gender: newGender }),
        credentials: 'include',
      });

      if (response.ok) {
        setVtubers(prev => prev.map(v => v.channelId === channelId ? { ...v, gender: newGender } : v));
        setAlertModal({ isOpen: true, title: '성별 업데이트', message: '성별이 성공적으로 업데이트되었습니다.' });
      } else {
        const errorText = await response.text();
        setAlertModal({ isOpen: true, title: '업데이트 실패', message: `성별 업데이트 실패: ${errorText}` });
      }
    } catch (error) {
      console.error('Gender update failed', error);
      setAlertModal({ isOpen: true, title: '오류', message: '네트워크 오류가 발생했습니다.' });
    }
  };

  const handleDeleteVtuber = (vtuber: Vtuber) => {
    setConfirmModal({
      isOpen: true,
      vtuber: vtuber,
      title: '버튜버 영구 제외',
      message: `[위험] '${vtuber.name}' 버튜버와 관련된 모든 노래 데이터를 삭제하고 블랙리스트(영구 제외)에 등록하시겠습니까?\n이 작업은 되돌릴 수 없으며, 앞으로 자동 수집되지 않습니다.`
    });
  };

  const executeDeleteVtuber = async () => {
    const vtuber = confirmModal.vtuber;
    if (!vtuber) return;
    
    setConfirmModal({ ...confirmModal, isOpen: false });
    setIsDeleting(vtuber.channelId);
    
    try {
      const response = await fetch(`/api/admin/vtubers/${vtuber.channelId}`, {
        method: 'DELETE',
        headers: {
          'X-XSRF-TOKEN': getCsrfToken(),
        },
        credentials: 'include',
      });

      if (response.ok) {
        setAlertModal({ isOpen: true, title: '삭제 완료', message: '성공적으로 삭제 및 제외 등록되었습니다.' });
        setVtubers(prev => prev.filter(v => v.channelId !== vtuber.channelId));
      } else {
        const errorText = await response.text();
        setAlertModal({ isOpen: true, title: '삭제 실패', message: `삭제 실패: ${errorText}` });
      }
    } catch (error) {
      console.error('Delete request failed', error);
      setAlertModal({ isOpen: true, title: '오류', message: '네트워크 오류가 발생했습니다.' });
    } finally {
      setIsDeleting(null);
    }
  };

  if (isAuthLoading || isLoading) {
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
    <div className="max-w-6xl mx-auto p-8 bg-gray-800 text-white rounded-lg shadow-lg min-h-screen">
      <div className="flex justify-between items-center mb-8">
        <div>
          <Link href="/admin" className="text-sm text-gray-400 hover:text-white transition">← 관리자 대시보드로 돌아가기</Link>
          <h1 className="text-3xl font-bold text-[#66D9EF] mt-2">버튜버 통합 관리</h1>
        </div>
        <div className="text-right">
          <p className="text-xs text-gray-500">전체 버튜버: <span className="text-white font-bold">{vtubers.length}</span>명</p>
        </div>
      </div>

      {/* 버튜버 수동 추가 섹션 */}
      <div className="bg-gray-900 p-6 rounded-lg mb-6 border border-gray-700">
        <h2 className="text-lg font-bold mb-4 text-[#A6E22E]">버튜버 수동 추가</h2>
        <form onSubmit={handleAddVtuber} className="flex gap-4">
          <input
            type="text"
            value={newChannelId}
            onChange={(e) => setNewChannelId(e.target.value)}
            placeholder="YouTube 채널 ID 입력 (예: UC...)"
            className="flex-1 p-3 bg-gray-800 rounded border border-gray-700 focus:outline-none focus:ring-2 focus:ring-[#A6E22E] transition text-sm"
            disabled={isSubmitting}
          />
          <select
            value={newGender}
            onChange={(e) => setNewGender(e.target.value)}
            className="w-32 p-3 bg-gray-800 rounded border border-gray-700 focus:outline-none focus:ring-2 focus:ring-[#A6E22E] transition text-sm text-gray-300"
            disabled={isSubmitting}
          >
            <option value="female">여성</option>
            <option value="male">남성</option>
            <option value="mixed">혼성/그룹</option>
            <option value="unknown">미정</option>
          </select>
          <button
            type="submit"
            className="bg-[#A6E22E] text-gray-900 font-bold px-6 py-2 rounded hover:bg-lime-400 transition disabled:bg-gray-500 whitespace-nowrap"
            disabled={isSubmitting}
          >
            {isSubmitting ? '추가 중...' : '버튜버 추가'}
          </button>
        </form>
        {addMessage && (
          <p className={`mt-3 text-xs p-2 rounded ${addMessage.startsWith('성공') ? 'bg-green-900/30 text-green-400' : 'bg-red-900/30 text-red-400'}`}>
            {addMessage}
          </p>
        )}
      </div>

      <div className="mb-6 flex flex-col md:flex-row gap-4">
        <div className="flex-1">
          <input
            type="text"
            placeholder="이름 또는 채널 ID로 검색..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full p-3 bg-gray-900 border border-gray-700 rounded-md focus:outline-none focus:ring-2 focus:ring-[#66D9EF] transition text-sm"
          />
        </div>
        <div className="flex gap-2">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="appearance-none py-3 pl-4 pr-10 bg-gray-900 border border-gray-700 rounded-md focus:outline-none focus:ring-2 focus:ring-[#66D9EF] transition text-sm text-gray-300 cursor-pointer"
            style={{ backgroundImage: 'url("data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' fill=\'none\' viewBox=\'0 0 24 24\' stroke=\'%239CA3AF\'%3E%3Cpath stroke-linecap=\'round\' stroke-linejoin=\'round\' stroke-width=\'2\' d=\'M19 9l-7 7-7-7\'%3E%3C/path%3E%3C/svg%3E")', backgroundRepeat: 'no-repeat', backgroundPosition: 'right 0.5rem center', backgroundSize: '1.5em 1.5em' }}
          >
            <option value="subscribers">구독자 순</option>
            <option value="addedTime">등록일 순</option>
            <option value="name">이름 순</option>
            <option value="gender">성별 순</option>
          </select>
          <select
            value={sortOrder}
            onChange={(e) => setSortOrder(e.target.value as any)}
            className="appearance-none py-3 pl-4 pr-10 bg-gray-900 border border-gray-700 rounded-md focus:outline-none focus:ring-2 focus:ring-[#66D9EF] transition text-sm text-gray-300 cursor-pointer"
            style={{ backgroundImage: 'url("data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' fill=\'none\' viewBox=\'0 0 24 24\' stroke=\'%239CA3AF\'%3E%3Cpath stroke-linecap=\'round\' stroke-linejoin=\'round\' stroke-width=\'2\' d=\'M19 9l-7 7-7-7\'%3E%3C/path%3E%3C/svg%3E")', backgroundRepeat: 'no-repeat', backgroundPosition: 'right 0.5rem center', backgroundSize: '1.5em 1.5em' }}
          >
            <option value="desc">내림차순</option>
            <option value="asc">오름차순</option>
          </select>
        </div>
      </div>

      <div className="bg-gray-900 rounded-lg overflow-hidden border border-gray-700">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-gray-800 text-gray-400 text-xs uppercase tracking-wider">
              <th className="px-6 py-4">프로필</th>
              <th className="px-6 py-4">이름 / 채널 ID</th>
              <th className="px-6 py-4 text-right">구독자</th>
              <th className="px-6 py-4 text-center">성별</th>
              <th className="px-6 py-4">등록일</th>
              <th className="px-6 py-4 text-center">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-800">
            {filteredVtubers.length > 0 ? (
              filteredVtubers.map((vtuber) => (
                <tr key={vtuber.channelId} className="hover:bg-gray-800/50 transition">
                  <td className="px-6 py-4">
                    <img 
                      src={vtuber.channelImg || 'https://via.placeholder.com/40'} 
                      alt={vtuber.name} 
                      className="w-10 h-10 rounded-full border border-gray-700"
                    />
                  </td>
                  <td className="px-6 py-4">
                    <div className="font-bold text-white">{vtuber.name}</div>
                    <div className="text-[10px] text-gray-500 font-mono">{vtuber.channelId}</div>
                  </td>
                  <td className="px-6 py-4 text-right font-mono text-cyan-400">
                    {vtuber.subscribers?.toLocaleString() || '0'}
                  </td>
                  <td className="px-6 py-4 text-center">
                    <select
                      value={vtuber.gender || 'unknown'}
                      onChange={(e) => handleUpdateGender(vtuber.channelId, e.target.value)}
                      className="appearance-none bg-gray-800 border border-gray-600 rounded px-2 py-1 text-xs text-gray-300 focus:outline-none focus:ring-1 focus:ring-[#66D9EF] cursor-pointer"
                    >
                      <option value="female">여성</option>
                      <option value="male">남성</option>
                      <option value="mixed">혼성/그룹</option>
                      <option value="unknown">미정</option>
                    </select>
                  </td>
                  <td className="px-6 py-4 text-xs text-gray-400">
                    {new Date(vtuber.addedTime).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-center">
                    <button
                      onClick={() => handleDeleteVtuber(vtuber)}
                      disabled={isDeleting === vtuber.channelId}
                      className="text-xs bg-red-900/50 text-red-400 border border-red-800 hover:bg-red-800 hover:text-white px-3 py-1 rounded transition disabled:bg-gray-700 disabled:text-gray-500"
                    >
                      {isDeleting === vtuber.channelId ? '삭제 중...' : '영구 제외'}
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="px-6 py-10 text-center text-gray-500 italic">
                  검색 결과가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        title={confirmModal.title}
        message={confirmModal.message}
        onConfirm={executeDeleteVtuber}
        onCancel={() => setConfirmModal({ ...confirmModal, isOpen: false })}
        isDanger={true}
        confirmLabel="영구 제외 및 삭제"
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

export default VtuberAdminPage;
