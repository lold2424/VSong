"use client";

import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import axios from 'axios';

axios.defaults.withCredentials = true;

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      if (window.location.pathname !== '/') {
        alert('세션이 만료되었습니다. 다시 로그인해주세요.');
        window.location.href = '/';
      }
    }
    return Promise.reject(error);
  }
);


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
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

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
          const data = await response.json().catch(() => null);
          if (data && data.name) {
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
        setUser(null);
        setIsLoggedIn(false);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserInfo();
  }, []);

  useEffect(() => {
    axios.post('/api/track-visit').catch(err => {
      console.error("Failed to track visit:", err);
    });
  }, []);

  const login = () => {
    window.location.href = '/oauth2/authorization/google';
  };

  const logout = async () => {
    await fetch('/api/logout', { credentials: 'include' });
    setUser(null);
    setIsLoggedIn(false);
    window.location.href = '/';
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, isLoading, login, logout }}>
      {children}
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
