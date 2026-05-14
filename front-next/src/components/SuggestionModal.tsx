"use client";

import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { submitSuggestion } from '@/utils/apiClient';

interface SuggestionModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const SuggestionModal: React.FC<SuggestionModalProps> = ({ isOpen, onClose }) => {
  const { user } = useAuth();
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;

    setIsSubmitting(true);
    setMessage(null);

    try {
      await submitSuggestion(content, user?.email, user?.name);
      setMessage({ type: 'success', text: '건의사항이 소중하게 전달되었습니다. 감사합니다!' });
      setContent('');
      setTimeout(() => {
        onClose();
        setMessage(null);
      }, 2000);
    } catch (error) {
      setMessage({ type: 'error', text: '제출 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <div className="bg-[#272822] border border-[#3E3D32] rounded-xl shadow-2xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
        <div className="p-6 border-b border-[#3E3D32] flex justify-between items-center bg-[#1e1f1c]">
          <h2 className="text-xl font-bold text-[#A6E22E]">건의사항 보내기</h2>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-white transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <p className="text-sm text-gray-400 leading-relaxed">
            VSong 프로젝트를 더 좋게 만들 수 있는 아이디어나 불편한 점을 자유롭게 남겨주세요. 
            보내주신 의견은 관리자가 직접 검토합니다.
          </p>

          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="여기에 내용을 입력해주세요..."
            className="w-full h-40 bg-[#1e1f1c] border border-[#49483E] rounded-lg p-4 text-[#F8F8F2] focus:outline-none focus:border-[#66D9EF] transition-colors resize-none placeholder:text-gray-600"
            disabled={isSubmitting}
            required
          />

          {message && (
            <div className={`p-3 rounded-lg text-sm font-medium ${
              message.type === 'success' ? 'bg-[#A6E22E]/10 text-[#A6E22E] border border-[#A6E22E]/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
            }`}>
              {message.text}
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2.5 rounded-lg border border-[#49483E] text-gray-300 hover:bg-[#3E3D32] hover:text-white transition-all font-bold text-sm"
              disabled={isSubmitting}
            >
              취소
            </button>
            <button
              type="submit"
              className="flex-[2] px-4 py-2.5 rounded-lg bg-[#A6E22E] text-black hover:bg-[#8ecb28] disabled:opacity-50 disabled:cursor-not-allowed transition-all font-black text-sm uppercase tracking-wider shadow-lg shadow-[#A6E22E]/20"
              disabled={isSubmitting || !content.trim()}
            >
              {isSubmitting ? '제출 중...' : '제안 제출하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SuggestionModal;
