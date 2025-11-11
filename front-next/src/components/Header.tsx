"use client";

import Image from "next/image";
import React, { useContext, useEffect, useState, Suspense } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import "./Header.css";
import { GenderContext } from "./GenderContext";

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
  const { genderFilter } = useContext(GenderContext);
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

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
    <div className="search-bar">
      <input
        type="text"
        placeholder="검색"
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        onKeyPress={handleKeyPress}
      />
      <button className="search-btn" onClick={handleSearch}>
        <Image src="/images/SearchBar.png" alt="Search" width={20} height={20} />
      </button>
    </div>
  );
};

const Header: React.FC<HeaderProps> = ({ onSearch }) => {
  const { setGenderFilter, genderFilter } = useContext(GenderContext);
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userInfo, setUserInfo] = useState<{ name: string; picture: string } | null>(null);

  const fetchUserInfo = async () => {
    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/api/login/userinfo`,
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
      removeUserInfoFromLocalStorage();
      setIsLoggedIn(false);
      setUserInfo(null);
    }
  };

  useEffect(() => {
    fetchUserInfo();
  }, []);

  const handleLogin = () => {
    window.location.href = `${process.env.NEXT_PUBLIC_API_URL}/oauth2/authorization/google`;
  };

  const handleLogout = async () => {
    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/api/logout`,
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
    <header className="header">
      <div style={{ display: "flex", alignItems: "center" }}>
        <Image
          src="/images/V-song.png"
          alt="V-Song Logo"
          className="logo"
          onClick={() => router.push("/")}
          width={40}
          height={40}
          priority
        />
        {isLoggedIn ? (
          <div className="user-info">
            {userInfo?.picture && (
              <Image
                src={userInfo.picture}
                alt="User"
                className="user-avatar"
                width={36}
                height={36}
              />
            )}
            <span>{userInfo?.name}</span>
            <button className="logout-btn" onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        ) : (
          <button className="login-btn" onClick={handleLogin}>
            로그인
          </button>
        )}
      </div>
      <Suspense fallback={<div>Loading...</div>}>
        <SearchBar />
      </Suspense>
      <div className="gender-filters">
        <button
          className={`gender-btn ${genderFilter === "male" ? "active" : ""}`}
          onClick={() => setGenderFilter("male")}
        >
          남성
        </button>
        <button
          className={`gender-btn ${genderFilter === "female" ? "active" : ""}`}
          onClick={() => setGenderFilter("female")}
        >
          여성
        </button>
        <button
          className={`gender-btn ${genderFilter === "all" ? "active" : ""}`}
          onClick={() => setGenderFilter("all")}
        >
          전체
        </button>
      </div>
    </header>
  );
};

export default Header;