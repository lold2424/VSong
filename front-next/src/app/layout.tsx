import type { Metadata } from "next";
import { Suspense } from "react";
import "./globals.css";
import Header from "@/components/Header";
import WeeklyChartContainer from "@/components/WeeklyChartContainer";
import Link from "next/link";

export const metadata: Metadata = {
  title: {
    default: "VSong",
    template: "%s | VSong",
  },
  description: "버튜버 노래 검색 및 추천 서비스",
  icons: {
    icon: '/favicon.ico',
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <div className="flex flex-col min-h-screen overflow-x-hidden bg-[#272822]">
          <Suspense fallback={<div>Loading...</div>}>
            <Header />
          </Suspense>
          <div className="flex flex-grow overflow-x-hidden w-full box-border min-h-[calc(100vh-140px)] pt-5">
            <div className="flex-[8] p-5 bg-[#272822] overflow-y-auto box-border">
              {children}
            </div>
            <div className="flex-[2] bg-[#272822] border-l border-[#66D9EF] p-5 shadow-md overflow-y-auto overflow-x-hidden box-border">
              <WeeklyChartContainer />
            </div>
          </div>
          <footer className="bg-gray-800 text-white p-5 text-center">
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
      </body>
    </html>
  );
}
