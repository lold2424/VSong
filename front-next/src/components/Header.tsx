"use client";

import Image from "next/image";
import React, { useEffect, useState, Suspense } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import Link from "next/link";

const saveUserInfoToLocalStorage = (userInfo: {
  name: string;
  picture: string;
}) => {
  localStorage.setItem("userInfo", JSON.stringify(userInfo));
};

const getUserInfoFromLocalStorage = () => {
  const storedUserInfo = localStorage.getItem("userInfo");
  return storedUserInfo ? JSON.parse(storedUserInfo) : null;
};

const removeUserInfoFromLocalStorage = () => {
  localStorage.removeItem("userInfo");
};

interface HeaderProps {
  onSearch?: (searchTerm: string, genderFilter: string) => void;
}

const SearchBar = () => {
  const [searchTerm, setSearchTerm] = useState("");
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
      alert("검색어는 두 글자 이상이 필요합니다.");
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
      <input
        type="text"
        placeholder="검색"
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        onKeyPress={handleKeyPress}
        className="border-none bg-transparent outline-none text-[#F8F8F2] text-sm p-1 w-50 placeholder:text-[#F8F8F2] placeholder:opacity-70"
      />
      <button className="bg-transparent border-none cursor-pointer ml-1.5" onClick={handleSearch}>
        <Image src="/images/SearchBar.png" alt="Search" width={20} height={20} className="filter invert" />
      </button>
    </div>
  );
};

const Header: React.FC<HeaderProps> = ({ onSearch }) => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const genderFilter = searchParams.get("gender") || "all";
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userInfo, setUserInfo] = useState<{ name: string; picture: string } | null>(null);

  const fetchUserInfo = async () => {
    try {
      const response = await fetch(
        `/api/login/userinfo`,
        {
          method: "GET",
          credentials: "include",
        }
      );

      if (response.ok) {
        const text = await response.text();
        const data = text ? JSON.parse(text) : null;

        if (data) {
          saveUserInfoToLocalStorage(data);
          setIsLoggedIn(true);
          setUserInfo(data);
        } else {
          removeUserInfoFromLocalStorage();
          setIsLoggedIn(false);
          setUserInfo(null);
        }
      } else {
        removeUserInfoFromLocalStorage();
        setIsLoggedIn(false);
        setUserInfo(null);
      }
    } catch (error) {
      // 로그인하지 않은 사용자의 경우, 리다이렉션으로 인해 네트워크 오류가 발생하는 것은 자연스러운 현상이므로 콘솔에 에러를 표시하지 않음.
      removeUserInfoFromLocalStorage();
      setIsLoggedIn(false);
      setUserInfo(null);
      // ESLint/TypeScript가 빈 catch 블록에 대해 경고하지 않도록
      void(error);
    }
  };

  useEffect(() => {
    fetchUserInfo();
  }, []);

  const handleLogin = () => {
    // next.config.js의 rewrites 설정을 통해 백엔드로 프록시됨
    window.location.href = '/oauth2/authorization/google';
  };

  const handleLogout = async () => {
    try {
      const response = await fetch(
        `/api/logout`,
        {
          method: "GET",
          credentials: "include",
        }
      );
      if (response.ok) {
        removeUserInfoFromLocalStorage();
        setUserInfo(null);
        setIsLoggedIn(false);
        alert("로그아웃되었습니다.");
        router.push("/");
      }
    } catch (error) {}
  };

  return (
    <header className="flex justify-between items-center px-5 py-2.5 bg-[#272822] text-[#F8F8F2] shadow-md">
      <div style={{ display: "flex", alignItems: "center" }}>
        <Link href="/" passHref>
          <Image
            src="/images/V-song.png"
            alt="V-Song Logo"
            className="cursor-pointer mr-5"
            width={40}
            height={40}
            priority
          />
        </Link>
        {isLoggedIn ? (
          <div className="flex items-center gap-2.5 ml-auto">
            {userInfo?.picture && (
              <Image
                src={userInfo.picture}
                alt="User"
                className="w-9 h-9 rounded-full object-cover border-2 border-[#F8F8F2]"
                width={36}
                height={36}
              />
            )}
            <span>{userInfo?.name}</span>
            <button className="px-2.5 py-1 bg-transparent text-[#F8F8F2] border border-[#F8F8F2] rounded-lg cursor-pointer text-sm transition-colors duration-200 hover:bg-[#A6E22E] hover:text-[#272222] hover:border-[#A6E22E]" onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        ) : (
          <button className="px-2.5 py-1 bg-transparent text-[#F8F8F2] border border-[#F8F8F2] rounded-lg cursor-pointer text-sm transition-colors duration-200 mr-[15px] hover:bg-[#A6E22E] hover:text-[#272222] hover:border-[#A6E22E]" onClick={handleLogin}>
            로그인
          </button>
        )}
      </div>
      <Suspense fallback={<div>Loading...</div>}>
        <SearchBar />
      </Suspense>
      <div className="flex gap-2.5">
        <Link href="/?gender=male" passHref>
          <button
            className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${genderFilter === "male" ? "text-white bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
          >
            남성
          </button>
        </Link>
        <Link href="/?gender=female" passHref>
          <button
            className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${genderFilter === "female" ? "text-white bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
          >
            여성
          </button>
        </Link>
        <Link href="/" passHref>
          <button
            className={`px-5 py-2.5 text-sm rounded-full border-2 border-[#3E3D32] cursor-pointer font-bold transition-colors duration-300 text-[#A6E22E] bg-[#3E3D32] outline-none ${genderFilter === "all" ? "text-white bg-[#A6E22E]" : ""} hover:bg-[#A6E22E] hover:text-white hover:border-[#A6E22E]`}
          >
            전체
          </button>
        </Link>
      </div>
    </header>
  );
};

export default Header;