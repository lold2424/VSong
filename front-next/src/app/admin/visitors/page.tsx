'use client';

import { useState, useEffect } from 'react';
import axios from 'axios';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

interface DailyVisitor {
    id: number;
    visitDate: string;
    count: number;
}

interface MonthlyVisitor {
    year: number;
    month: number;
    totalCount: number;
}

type ViewType = 'weekly' | 'monthly';

const WeeklyStats = () => {
    const [data, setData] = useState<DailyVisitor[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const response = await axios.get('/api/visitors/daily');
                const rawData = response.data;

                const last7Days = Array.from({ length: 7 }, (_, i) => {
                    const d = new Date();
                    const kstDate = new Date(d.getTime() + (9 * 60 * 60 * 1000));
                    kstDate.getUTCDate(); // Side effect to ensure object state
                    
                    const targetDate = new Date(kstDate);
                    targetDate.setUTCDate(targetDate.getUTCDate() - (6 - i));
                    return targetDate.toISOString().split('T')[0]; // YYYY-MM-DD
                });

                const paddedData = last7Days.map((date, index) => {
                    const found = rawData.find((item: any) => item.visitDate === date);
                    return {
                        id: found ? found.id : -index,
                        visitDate: date,
                        count: found ? found.count : 0
                    };
                });

                setData(paddedData);
                setError(null);
            } catch (err) {
                setError('Failed to fetch weekly visitor data.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    if (loading) return <div className="text-center p-10 text-white">로딩 중...</div>;
    if (error) return <div className="text-center p-10 text-red-500">{error}</div>;

    return (
        <div>
            <table className="w-full border-collapse">
                <thead>
                    <tr>
                        <th className="border border-gray-600 p-3 bg-gray-700 text-left">날짜</th>
                        <th className="border border-gray-600 p-3 bg-gray-700 text-left">방문자 수</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map((visitor) => (
                        <tr key={visitor.id} className="hover:bg-gray-700 transition-colors">
                            <td className="border border-gray-600 p-3 text-center">{visitor.visitDate}</td>
                            <td className="border border-gray-600 p-3 text-center">{visitor.count}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div className="mt-8">
                <ResponsiveContainer width="100%" height={400}>
                    <BarChart data={data}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="visitDate" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="count" fill="#8884d8" name="방문자 수" />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

const MonthlyStats = () => {
    const [data, setData] = useState<MonthlyVisitor[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const response = await axios.get('/api/visitors/monthly');
                setData(response.data);
                setError(null);
            } catch (err) {
                setError('Failed to fetch monthly visitor data.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    if (loading) return <div className="text-center p-10 text-white">로딩 중...</div>;
    if (error) return <div className="text-center p-10 text-red-500">{error}</div>;

    const chartData = data.map(item => ({
        ...item,
        monthLabel: `${item.year}년 ${item.month}월`
    }));

    return (
        <div>
            <table className="w-full border-collapse">
                <thead>
                    <tr>
                        <th className="border border-gray-600 p-3 bg-gray-700 text-left">월</th>
                        <th className="border border-gray-600 p-3 bg-gray-700 text-left">총 방문자 수</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map((visitor) => (
                        <tr key={`${visitor.year}-${visitor.month}`} className="hover:bg-gray-700 transition-colors">
                            <td className="border border-gray-600 p-3 text-center">{`${visitor.year}년 ${visitor.month}월`}</td>
                            <td className="border border-gray-600 p-3 text-center">{visitor.totalCount}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div className="mt-8">
                <ResponsiveContainer width="100%" height={400}>
                    <BarChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="monthLabel" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="totalCount" fill="#82ca9d" name="총 방문자 수" />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};


export default function VisitorStatsPage() {
    const [view, setView] = useState<ViewType>('weekly');

    return (
        <div className="max-w-4xl mx-auto p-8 bg-gray-800 text-white rounded-lg shadow-lg">
            <h1 className="text-3xl font-bold mb-6 text-center text-[#A6E22E]">방문자 통계</h1>
            
            <div className="flex justify-center mb-6 border-b border-gray-600">
                <button 
                    onClick={() => setView('weekly')}
                    className={`py-2 px-4 font-semibold ${view === 'weekly' ? 'text-[#A6E22E] border-b-2 border-[#A6E22E]' : 'text-gray-400'}`}
                >
                    최근 7일
                </button>
                <button 
                    onClick={() => setView('monthly')}
                    className={`py-2 px-4 font-semibold ${view === 'monthly' ? 'text-[#A6E22E] border-b-2 border-[#A6E22E]' : 'text-gray-400'}`}
                >
                    월별
                </button>
            </div>

            <div>
                {view === 'weekly' ? <WeeklyStats /> : <MonthlyStats />}
            </div>
        </div>
    );
}
