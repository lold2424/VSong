import VSongLogo from '../../images/V-song.png';
import SearchBarIcon from '../../images/SearchBar.png';
import React, { useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Header.css";
import { GenderContext } from "./GenderContext";

const saveUserInfoToLocalStorage = (userInfo: { name: string; picture: string }) => {
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

const Header: React.FC<HeaderProps> = ({ onSearch }) => {
    const [searchTerm, setSearchTerm] = useState("");
    const { genderFilter, setGenderFilter } = useContext(GenderContext);
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [userInfo, setUserInfo] = useState<{ name: string; picture: string } | null>(null);

    const fetchUserInfo = async () => {
        try {
            console.log("Fetching user info...");
            const response = await  fetch('/api/login/userinfo', {
                method: "GET",
                credentials: "include",
            });

            if (response.ok) {
                const text = await response.text();
                const data = text ? JSON.parse(text) : null;
                console.log("User info fetched successfully:", data);

                if (data) {
                    saveUserInfoToLocalStorage(data);
                    setIsLoggedIn(true);
                    setUserInfo(data);
                } else {
                    console.warn("User is not logged in.");
                    removeUserInfoFromLocalStorage();
                    setIsLoggedIn(false);
                    setUserInfo(null);
                }
            } else {
                if (response.status === 401) {
                } else {
                    console.warn(`Failed to fetch user info. Status: ${response.status}`);
                }
                removeUserInfoFromLocalStorage();
                setIsLoggedIn(false);
                setUserInfo(null);
            }
        } catch (error) {
            console.error("Error fetching user info:", error);
            removeUserInfoFromLocalStorage();
            setIsLoggedIn(false);
            setUserInfo(null);
        }
    };

    useEffect(() => {
        const storedUserInfo = getUserInfoFromLocalStorage();
        if (storedUserInfo) {
            console.log("Found user info in localStorage:", storedUserInfo);
            setUserInfo(storedUserInfo);
            setIsLoggedIn(true);
        } else {
            fetchUserInfo();
        }
    }, []);

    const handleLogin = () => {
        console.log("Redirecting to Google OAuth...");
        window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    };

    const handleLogout = async () => {
        console.log("Attempting to log out...");
        try {
            const response = await fetch('/api/logout', {
                method: "GET",
                credentials: "include",
            });
            if (response.ok) {
                console.log("Logout successful.");
                removeUserInfoFromLocalStorage();
                setUserInfo(null);
                setIsLoggedIn(false);
                alert("로그아웃되었습니다.");
                navigate("/");
            } else {
                console.warn("Failed to log out. Response status:", response.status);
            }
        } catch (error) {
            console.error("Error during logout:", error);
        }
    };

    const handleSearch = () => {
        const trimmedSearchTerm = searchTerm.trim();
        if (trimmedSearchTerm.length < 2) {
            alert("검색어는 두 글자 이상이 필요합니다.");
            return;
        }
        console.log("Initiating search with term:", trimmedSearchTerm);
        navigate(`/search?query=${encodeURIComponent(trimmedSearchTerm)}&gender=${genderFilter}`);
    };

    const handleKeyPress = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (event.key === "Enter") {
            handleSearch();
        }
    };

    return (
        <header className="header">
            <div style={{ display: "flex", alignItems: "center" }}>
                                    <img
                                    src={VSongLogo}
                                    alt="V-Song Logo"
                                    className="logo"
                                    onClick={() => navigate("/")}
                                />                {isLoggedIn ? (
                    <div className="user-info">
                        <img src={userInfo?.picture} alt="User" className="user-avatar" />
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
            <div className="search-bar">
                <input
                    type="text"
                    placeholder="검색"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={handleKeyPress}
                />
                <button className="search-btn" onClick={handleSearch}>
                    <img src={SearchBarIcon} alt="Search" />
                </button>
            </div>
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
