"use client";

import React, { createContext, useState, useEffect, useContext, ReactNode, useCallback } from 'react';
import { apiClient, fetchUserInfoApi, logoutApi } from '@/utils/apiClient';
import AlertModal from '@/components/AlertModal';

interface User {
  name: string;
  email: string;
  picture: string;
  role: 'USER' | 'ADMIN';
}

interface AuthContextType {
  isLoggedIn: boolean;
  user: User | null;
  isLoading: boolean;
  login: () => void;
  logout: () => void;
  checkSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const [isModalOpen, setIsModalOpen] = useState(false);

  const fetchUserInfo = useCallback(async () => {
    console.log("[AuthContext] fetchUserInfo 호출됨...");
    try {
      const data = await fetchUserInfoApi();
      console.log("[AuthContext] 유저 정보 로드 성공:", data?.email);
      if (data && data.name) {
        setUser(data);
        setIsLoggedIn(true);
      }
    } catch (error: any) {
      console.log("[AuthContext] 유저 정보 로드 중 오류 (비로그인 또는 세션 만료):", error.response?.status);
      setUser(null);
      setIsLoggedIn(false);
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn]);

  useEffect(() => {
    console.log("[AuthContext] 전역 인터셉터 설정됨 (isLoggedIn:", isLoggedIn, ")");
    const interceptor = apiClient.interceptors.response.use(
      response => response,
      error => {
        const status = error.response?.status;
        console.log(`[AuthContext] API 응답 오류 감지: ${status}, 현재 isLoggedIn: ${isLoggedIn}`);
        
        if (status === 401) {
          console.log("[AuthContext] 401 Unauthorized 감지됨!");
          if (isLoggedIn) {
            console.log("[AuthContext] 세션 만료 모달 활성화 시도...");
            setIsModalOpen(true);
          } else {
            console.log("[AuthContext] 로그인되지 않은 상태에서의 401이므로 모달을 띄우지 않음.");
          }
          setIsLoggedIn(false);
          setUser(null);
        }
        return Promise.reject(error);
      }
    );

    return () => {
      console.log("[AuthContext] 전역 인터셉터 해제됨");
      apiClient.interceptors.response.eject(interceptor);
    };
  }, [isLoggedIn]);

  useEffect(() => {
    fetchUserInfo();

    const handleVisibilityChange = () => {
      console.log("[AuthContext] Visibility 변경 감지:", document.visibilityState);
      if (document.visibilityState === 'visible' && isLoggedIn) {
        console.log("[AuthContext] 화면 복귀에 따른 세션 체크 시작...");
        fetchUserInfo();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [fetchUserInfo, isLoggedIn]);

  const checkSession = async () => {
    await fetchUserInfo();
  };

  const login = () => {
    window.location.href = '/oauth2/authorization/google';
  };

  const logout = async () => {
    try {
      await logoutApi();
    } catch (err) {
      console.error("Logout error", err);
    } finally {
      setUser(null);
      setIsLoggedIn(false);
      window.location.href = '/';
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setIsLoggedIn(false);
    setUser(null);
    window.location.href = '/';
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, isLoading, login, logout, checkSession }}>
      {children}
      <AlertModal 
        isOpen={isModalOpen}
        title="세션 만료"
        message="오랫동안 활동이 없어 세션이 만료되었습니다. 다시 로그인해주세요."
        onClose={handleModalClose}
      />
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
