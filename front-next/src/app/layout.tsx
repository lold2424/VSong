import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { GenderProvider } from "@/components/GenderContext";
import Header from "@/components/Header";
import WeeklyChart from "@/components/WeeklyChart";
import axios from "axios";
import Link from "next/link";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "VSong",
  description: "버튜버 노래 검색 및 추천 서비스",
};

interface MainApiResponse {
  top10WeeklySongs: any[];
  top10DailySongs: any[];
  top10WeeklyShorts: any[];
}

async function getChartData(): Promise<MainApiResponse> {
  try {
    const response = await axios.get<MainApiResponse>(
      `${process.env.NEXT_PUBLIC_API_URL}/api/main`
    );
    return {
      top10WeeklySongs: response.data.top10WeeklySongs || [],
      top10DailySongs: response.data.top10DailySongs || [],
      top10WeeklyShorts: response.data.top10WeeklyShorts || [],
    };
  } catch (error) {
    console.error("차트 데이터를 가져오는 중 오류 발생:", error);
    return {
      top10WeeklySongs: [],
      top10DailySongs: [],
      top10WeeklyShorts: [],
    };
  }
}

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const chartData = await getChartData();

  return (
    <html lang="ko">
      <body className={inter.className}>
        <GenderProvider>
          <div className="app-container">
            <Header />
            <div className="main-layout">
              <div className="content">{children}</div>
              <div className="sidebar">
                <WeeklyChart
                  top10WeeklySongs={chartData.top10WeeklySongs}
                  top10DailySongs={chartData.top10DailySongs}
                  top10WeeklyShorts={chartData.top10WeeklyShorts}
                />
              </div>
            </div>
            <footer className="footer">
              <div>
                <p>© 2025 VSong. All rights reserved.</p>
                <p>
                  <Link
                    href="https://github.com/lold2424/vsong"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    GitHub
                  </Link>{" "}
                  | <Link href="/privacy">개인정보처리방침</Link>
                </p>
              </div>
            </footer>
          </div>
        </GenderProvider>
      </body>
    </html>
  );
}
