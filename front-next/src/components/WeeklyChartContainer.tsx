"use client";

import React, { useEffect, useState } from "react";
import axios from "axios";
import WeeklyChart from "./WeeklyChart";
import { useSearchParams } from "next/navigation";

interface MainApiResponse {
  top10WeeklySongs: any[];
  top10DailySongs: any[];
}

const WeeklyChartContainer: React.FC = () => {
  const searchParams = useSearchParams();
  const gender = searchParams.get("gender") || "all";
  
  const [chartData, setChartData] = useState<MainApiResponse>({
    top10WeeklySongs: [],
    top10DailySongs: [],
  });

  useEffect(() => {
    axios
      .get<MainApiResponse>(`/api/home`, {
        params: { 
          gender,
          t: new Date().getTime() 
        }
      })
      .then((response) => {
        setChartData({
          top10WeeklySongs: response.data.top10WeeklySongs || [],
          top10DailySongs: response.data.top10DailySongs || [],
        });
      })
      .catch((error) => {
        console.error("차트 데이터를 가져오는 중 오류 발생:", error);
      });
  }, [gender]);

  return (
    <WeeklyChart
      top10WeeklySongs={chartData.top10WeeklySongs}
      top10DailySongs={chartData.top10DailySongs}
    />
  );
};

export default WeeklyChartContainer;
