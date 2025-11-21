"use client";

import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';

// 사용자 정보 타입 정의
interface User {
  name: string;
  email: string;
  picture: string;
  role: 'USER' | 'ADMIN';
}

// Context가 가지게 될 값들의 타입 정의
interface AuthContextType {
  isLoggedIn: boolean;
  user: User | null;
  isLoading: boolean;
  login: () => void;
  logout: () => void;
}

// Context 생성
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// AuthProvider 컴포넌트 생성
export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchUserInfo = async () => {
      setIsLoading(true);
      try {
        const response = await fetch('/api/login/userinfo', { credentials: 'include' });

        if (response.ok) {
          const data = await response.json().catch(() => null); // JSON 파싱 에러 방지
          if (data && data.name) { // name 필드가 있는지 확인하여 유효한 사용자 데이터인지 검증
            setUser(data);
            setIsLoggedIn(true);
          } else {
            setUser(null);
            setIsLoggedIn(false);
          }
        } else {
          setUser(null);
          setIsLoggedIn(false);
        }
      } catch (error) {
        // 네트워크 오류 시 사용자 상태 초기화
        setUser(null);
        setIsLoggedIn(false);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserInfo();
  }, []);

  const login = () => {
    window.location.href = '/oauth2/authorization/google';
  };

  const logout = async () => {
    await fetch('/api/logout', { credentials: 'include' });
    setUser(null);
    setIsLoggedIn(false);
    // 페이지를 새로고침하여 상태를 완전히 초기화
    window.location.href = '/';
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// 커스텀 훅 생성
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
