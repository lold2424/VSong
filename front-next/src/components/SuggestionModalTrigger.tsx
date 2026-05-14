"use client";

import React, { useState } from 'react';
import SuggestionModal from './SuggestionModal';

const SuggestionModalTrigger = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <button 
        onClick={() => setIsModalOpen(true)}
        className="hover:text-[#A6E22E] transition-colors text-left"
      >
        건의사항 보내기 (Suggestions)
      </button>
      <SuggestionModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
      />
    </>
  );
};

export default SuggestionModalTrigger;
