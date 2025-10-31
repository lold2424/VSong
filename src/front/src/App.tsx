import React, { useState, useEffect } from 'react';
import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './page/components/Header';
import MainPage from './page/MainPage';
import SearchResultsPage from './page/SearchResultsPage';
import WeeklyChart from './page/components/WeeklyChart';
import axios from 'axios';
import { GenderProvider } from './page/components/GenderContext';
import VtuberDetailPage from "./page/VtuberDetailPage";
import PrivacyPolicyPage from './page/PrivacyPolicyPage';

const App: React.FC = () => {
    const [top10WeeklySongs, setTop10WeeklySongs] = useState<any[]>([]);
    const [top10DailySongs, setTop10DailySongs] = useState<any[]>([]);
    const [top10WeeklyShorts, setTop10WeeklyShorts] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [chartType, setChartType] = useState<'weekly' | 'daily' | 'shorts'>('weekly');

    interface MainApiResponse {
        top10WeeklySongs: any[];
        top10DailySongs: any[];
        top10WeeklyShorts: any[];
    }

    useEffect(() => {
        axios.get<MainApiResponse>('/api/main')
            .then((response) => {
                setTop10WeeklySongs(response.data.top10WeeklySongs || []);
                setTop10DailySongs(response.data.top10DailySongs || []);
                setTop10WeeklyShorts(response.data.top10WeeklyShorts || []);
                setIsLoading(false);
            })
            .catch((error) => {
                console.error('차트 데이터를 가져오는 중 오류 발생:', error);
                setError('차트 데이터를 불러오는 중 오류가 발생했습니다.');
                setIsLoading(false);
            });
    }, []);

    return (
        <Router>
            <GenderProvider>
                <div className="app-container">
                    <Header />
                    <div className="main-layout">
                        <div className="content">
                            <Routes>
                                <Route path="/" element={<MainPage />} />
                                <Route path="/search" element={<SearchResultsPage />} />
                                <Route path="/vtuber/:channelId" element={<VtuberDetailPage />} />
                                <Route path="/privacy" element={<PrivacyPolicyPage />} />
                                <Route path="/login/success" element={<h1>로그인 성공</h1>} />
                                <Route path="/login/failure" element={<h1>로그인 실패</h1>} />
                                <Route path="*" element={<h1>페이지를 찾을 수 없습니다</h1>} />
                            </Routes>
                        </div>
                        <div className="sidebar">
                            <div className="button-group">
                                <button
                                    onClick={() => setChartType('weekly')}
                                    className={chartType === 'weekly' ? 'active' : ''}
                                >
                                    주간
                                </button>
                                <button
                                    onClick={() => setChartType('daily')}
                                    className={chartType === 'daily' ? 'active' : ''}
                                >
                                    일간
                                </button>
                                <button
                                    onClick={() => setChartType('shorts')}
                                    className={chartType === 'shorts' ? 'active' : ''}
                                >
                                    쇼츠
                                </button>
                            </div>

                            {isLoading ? (
                                <p>차트를 불러오는 중입니다...</p>
                            ) : error ? (
                                <p>{error}</p>
                            ) : (
                                <>
                                    {chartType === 'weekly' && (
                                        <WeeklyChart top10WeeklySongs={top10WeeklySongs} title="주간 인기 노래" />
                                    )}
                                    {chartType === 'daily' && (
                                        <WeeklyChart top10WeeklySongs={top10DailySongs} title="일간 인기 노래" />
                                    )}
                                    {chartType === 'shorts' && (
                                        <WeeklyChart top10WeeklySongs={top10WeeklyShorts} title="주간 인기 쇼츠" />
                                    )}
                                </>
                            )}
                        </div>
                    </div>
                </div>
            </GenderProvider>
        </Router>
    );
};

export default App;
