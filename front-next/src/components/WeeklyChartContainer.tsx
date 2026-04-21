"use client";

import React, { useEffect, useState } from "react";
import axios from "axios";
import WeeklyChart from "./WeeklyChart";

interface MainApiResponse {
  top10WeeklySongs: any[];
  top10DailySongs: any[];
}

const WeeklyChartContainer: React.FC = () => {
  const [chartData, setChartData] = useState<MainApiResponse>({
    top10WeeklySongs: [],
    top10DailySongs: [],
  });

  useEffect(() => {
    axios
      .get<MainApiResponse>(`/api/home?t=${new Date().getTime()}`)
      .then((response) => {
        setChartData({
          top10WeeklySongs: response.data.top10WeeklySongs || [],
          top10DailySongs: response.data.top10DailySongs || [],
        });
      })
      .catch((error) => {
        console.error("차트 데이터를 가져오는 중 오류 발생:", error);
      });
  }, []);

  return (
    <WeeklyChart
      top10WeeklySongs={chartData.top10WeeklySongs}
      top10DailySongs={chartData.top10DailySongs}
    />
  );
};

export default WeeklyChartContainer;
