"use client";

import React, { useEffect, useState } from "react";
import axios from "axios";
import WeeklyChart from "./WeeklyChart";

interface MainApiResponse {
  top10WeeklySongs: any[];
  top10DailySongs: any[];
  top10WeeklyShorts: any[];
}

const WeeklyChartContainer: React.FC = () => {
  const [chartData, setChartData] = useState<MainApiResponse>({
    top10WeeklySongs: [],
    top10DailySongs: [],
    top10WeeklyShorts: [],
  });

  useEffect(() => {
    axios
      .get<MainApiResponse>("/api/main")
      .then((response) => {
        setChartData({
          top10WeeklySongs: response.data.top10WeeklySongs || [],
          top10DailySongs: response.data.top10DailySongs || [],
          top10WeeklyShorts: response.data.top10WeeklyShorts || [],
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
      top10WeeklyShorts={chartData.top10WeeklyShorts}
    />
  );
};

export default WeeklyChartContainer;
