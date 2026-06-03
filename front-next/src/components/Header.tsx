"use client";

import Image from "next/image";
import React, { Suspense, useState, useEffect } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import AlertModal from "@/components/AlertModal";

const SearchBarContent = () => {
  const [searchTerm, setSearchTerm] = useState("");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMessage, setModalMessage] = useState("");
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const genderFilter = searchParams.get("gender") || "all";

  useEffect(() => {
    const queryFromUrl = searchParams.get("query");
    if (pathname === "/search" && queryFromUrl) {
      setSearchTerm(queryFromUrl);
    } else {
      setSearchTerm("");
    }
  }, [pathname, searchParams]);

  const handleSearch = () => {
    const trimmedSearchTerm = searchTerm.trim();
    if (trimmedSearchTerm.length < 2) {
      setModalMessage("검색어는 두 글자 이상이 필요합니다.");
      setIsModalOpen(true);
      return;
    }
    router.push(
      `/search?query=${encodeURIComponent(
        trimmedSearchTerm
      )}&gender=${genderFilter}`
    );
  };

  const handleKeyPress = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter") {
      handleSearch();
    }
  };

  return (
    <div className="flex items-center bg-[#3E3D32] rounded-lg px-2.5 py-1">
      <AlertModal 
        isOpen={isModalOpen} 
        title="검색 안내" 
        message={modalMessage} 
        onClose={() => setIsModalOpen(false)} 
      />
      <input
        type="text"
        placeholder="검색"
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        onKeyDown={handleKeyPress}
        className="border-none bg-transparent outline-none focus-visible:ring-1 focus-visible:ring-[#A6E22E] rounded text-[#F8F8F2] text-sm p-1 w-50 placeholder:text-[#F8F8F2] placeholder:opacity-70"
      />
      <button className="bg-transparent border-none cursor-pointer ml-1.5 focus-visible:ring-2 focus-visible:ring-[#A6E22E] rounded-md outline-none" onClick={handleSearch}>
        <Image src="/images/SearchBar.png" alt="Search" width={20} height={20} className="filter invert" />
      </button>
    </div>
  );
};

const HeaderContent: React.FC = () => {
  const searchParams = useSearchParams();
  const genderFilter = searchParams.get("gender") || "all";
  const { isLoggedIn, user, isLoading, login, logout, checkSession } = useAuth();
  const pathname = usePathname();

  if (isLoading) {
    return <header className="flex justify-between items-center px-5 py-2.5 bg-[#272822] text-[#F8F8F2] shadow-md h-[76px]"></header>;
  }

  return (
    <header className="flex justify-between items-center px-5 py-2.5 bg-[#272822] text-[#F8F8F2] shadow-md">
      <div className="flex items-center">
        <Link href="/" passHref onClick={() => checkSession()}>
          <Image
            src="/images/V-song.png"
            alt="V-Song Logo"
            className="cursor-pointer mr-5"
            width={40}
            height={40}
            priority
          />
        </Link>
        {isLoggedIn && user ? (
          <div className="flex items-center gap-2.5 ml-auto">
            {user.picture && (
              <Image
                src={user.picture}
                alt="User"
                className="w-9 h-9 rounded-full object-cover border-2 border-[#F8F8F2]"
                width={36}
                height={36}
              />
            )}
            <span>{user.name}</span>
            {user.role === 'ADMIN' && (
              <Link href="/admin" passHref>
                <button className="px-2.5 py-1 bg-yellow-500 text-black border border-yellow-500 rounded-lg cursor-pointer text-sm font-bold transition-colors duration-200 hover:bg-yellow-400">
                  관리자
                </button>
              </Link>
            )}
            <button className="px-2.5 py-1 bg-transparent text-[#F8F8F2] border border-[#F8F8F2] rounded-lg cursor-pointer text-sm transition-colors duration-200 hover:bg-[#A6E22E] hover:text-[#272222] hover:border-[#A6E22E]" onClick={logout}>
              로그아웃
            </button>
          </div>
        ) : (
          <button className="px-2.5 py-1 bg-transparent text-[#F8F8F2] border border-[#F8F8F2] rounded-lg cursor-pointer text-sm transition-colors duration-200 mr-[15px] hover:bg-[#A6E22E] hover:text-[#272222] hover:border-[#A6E22E]" onClick={login}>
            로그인
          </button>
        )}
      </div>
      
      <SearchBarContent />

      {pathname === "/" ? (
        <div className="flex gap-2.5">
          <Link href="/?gender=male" passHref>
            <button
              className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${genderFilter === "male" ? "text-[#272222] bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
            >
              남성
            </button>
          </Link>
          <Link href="/?gender=female" passHref>
            <button
              className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${genderFilter === "female" ? "text-[#272222] bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
            >
              여성
            </button>
          </Link>
          <Link href="/" passHref>
            <button
              className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${genderFilter === "all" ? "text-[#272222] bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
            >
              전체
            </button>
          </Link>
        </div>
      ) : (
        <div className="w-[214px]"></div>
      )}
    </header>
  );
};

const Header: React.FC = () => {
  return (
    <Suspense fallback={<header className="bg-[#272822] h-[76px] shadow-md px-5 py-2.5"></header>}>
      <HeaderContent />
    </Suspense>
  );
};

export default Header;
