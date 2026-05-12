"use client";

import React, { useEffect, useState } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { getSongHistory } from "@/utils/apiClient";

interface SongHistoryData {
  recordDate: string;
  viewCount: number;
}

interface SongViewChartProps {
  videoId: string;
}

const SongViewChart: React.FC<SongViewChartProps> = ({ videoId }) => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchHistory = async () => {
      setLoading(true);
      try {
        const historyData: SongHistoryData[] = await getSongHistory(videoId);

        const chartData = historyData.map((curr, idx, arr) => {
          if (idx === 0) return null;
          const prev = arr[idx - 1];
          const increase = curr.viewCount - prev.viewCount;
          
          return {
            date: curr.recordDate.split('-').slice(1).join('/'),
            increase: increase > 0 ? increase : 0,
            fullCount: curr.viewCount
          };
        }).filter(item => item !== null);

        setData(chartData);
      } catch (err) {
        console.error("히스토리 로딩 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, [videoId]);

  if (loading) return <div className="h-48 flex justify-center items-center text-gray-400">조회수 추이 로딩 중...</div>;
  if (data.length === 0) return <div className="h-48 flex justify-center items-center text-gray-500 text-sm">최근 수집된 데이터가 부족합니다.</div>;

  return (
    <div className="w-full mt-4 p-4 bg-[#1e1f1c] rounded-lg border border-[#3E3D32]">
      <h4 className="text-[#A6E22E] text-sm font-bold mb-4 flex items-center gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
        </svg>
        일간 조회수 상승 추이 (최근 7일)
      </h4>
      <div className="h-48 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" stroke="#3E3D32" vertical={false} />
            <XAxis 
              dataKey="date" 
              stroke="#75715E" 
              fontSize={10}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              stroke="#75715E" 
              fontSize={10} 
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => value.toLocaleString()}
            />
            <Tooltip 
              contentStyle={{ backgroundColor: '#272822', border: '1px solid #3E3D32', borderRadius: '4px' }}
              labelStyle={{ color: '#A6E22E', fontWeight: 'bold', fontSize: '12px' }}
              itemStyle={{ color: '#F8F8F2', fontSize: '12px' }}
              formatter={(value: any) => [Number(value).toLocaleString() + '회', '상승량']}
            />
            <Line 
              type="monotone" 
              dataKey="increase" 
              stroke="#A6E22E" 
              strokeWidth={3} 
              dot={{ fill: '#A6E22E', r: 4 }}
              activeDot={{ r: 6, stroke: '#FFF' }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <p className="mt-3 text-[10px] text-[#75715E] italic text-right">
        * 매일 00:01 (KST) 기준의 유튜브 조회수를 기록한 데이터입니다.
      </p>
    </div>
  );
};

export default SongViewChart;
