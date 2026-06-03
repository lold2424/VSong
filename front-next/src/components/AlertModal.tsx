"use client";

import React from 'react';

interface AlertModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  onClose: () => void;
  confirmLabel?: string;
}

const AlertModal: React.FC<AlertModalProps> = ({ isOpen, title, message, onClose, confirmLabel = "확인" }) => {
  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black bg-opacity-80 backdrop-blur-sm animate-in fade-in duration-200"
      role="alert"
      aria-live="assertive"
    >
      <div className="bg-[#272822] border-2 border-[#A6E22E] rounded-2xl p-6 w-full max-w-sm shadow-[0_0_20px_rgba(166,226,46,0.2)] animate-in zoom-in-95 duration-200" role="document">
        <div className="flex items-center gap-3 mb-4">
          <div className="bg-[#3E3D32] p-2 rounded-lg">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-[#A6E22E]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-[#A6E22E]">{title}</h2>
        </div>
        
        <p className="text-gray-300 mb-8 leading-relaxed">
          {message}
        </p>
        
        <button
          onClick={onClose}
          className="w-full bg-[#A6E22E] text-[#272822] font-bold py-3 rounded-xl hover:bg-[#C1F15D] active:scale-[0.98] transition-all shadow-lg shadow-[#A6E22E]/20"
        >
          {confirmLabel}
        </button>
      </div>
    </div>
  );
};

export default AlertModal;
