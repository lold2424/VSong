import type { Metadata } from "next";
import { Suspense } from "react";
import "./globals.css";
import Header from "@/components/Header";
import WeeklyChartContainer from "@/components/WeeklyChartContainer";
import Link from "next/link";

import { AuthProvider } from "@/context/AuthContext";
import { WebVitalsReporter } from "@/components/WebVitalsReporter";
import SuggestionModalTrigger from "@/components/SuggestionModalTrigger";

export const metadata: Metadata = {
  metadataBase: new URL("https://www.vsong.site"),
  title: {
    default: "VSong | 버튜버 음악 아카이브 & 트렌드",
    template: "%s | VSong",
  },
  description: "버튜버 음악의 모든 순간을 기록합니다. 실시간 수집되는 버추얼 유튜버들의 최신곡과 주간 인기 트렌드를 VSong에서 만나보세요.",
  icons: {
    icon: [
      { url: '/favicon.svg', type: 'image/svg+xml' }
    ],
  },
  openGraph: {
    title: "VSong | 버튜버 음악 아카이브 & 트렌드",
    description: "버튜버 음악의 모든 순간을 기록합니다. 최신곡과 실시간 트렌드 정보를 제공하는 정교한 아카이브.",
    url: "https://www.vsong.site",
    siteName: "VSong",
    locale: "ko_KR",
    type: "website",
  },
  alternates: {
    canonical: "/",
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
        <AuthProvider>
          <WebVitalsReporter /> {/* Add the WebVitalsReporter here */}
          <a href="#main-content" className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-[2000] focus:px-4 focus:py-2 focus:bg-[#A6E22E] focus:text-black focus:font-bold focus:rounded-lg focus:shadow-2xl">
            본문 바로가기
          </a>
          <div className="flex flex-col min-h-screen overflow-x-hidden bg-[#272822]">
            <Suspense fallback={<div>Loading...</div>}>
              <Header />
            </Suspense>
            <main id="main-content" className="flex flex-grow overflow-x-hidden w-full box-border min-h-[calc(100vh-140px)] pt-5 outline-none" tabIndex={-1}>
              <div className="flex-[8] p-5 bg-[#272822] overflow-y-auto box-border">
                {children}
              </div>
              <div className="flex-[2] bg-[#272822] border-l border-[#66D9EF] p-5 shadow-md overflow-y-auto overflow-x-hidden box-border">
                <Suspense fallback={<div className="text-white text-xs">Loading Charts...</div>}>
                  <WeeklyChartContainer />
                </Suspense>
              </div>
            </main>
            <footer className="bg-[#1e1f1c] text-[#F8F8F2] pt-16 pb-10 px-8 mt-20 border-t border-[#3E3D32]">
              <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-12">
                {/* Brand Column */}
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <h3 className="text-2xl font-black tracking-tighter text-[#A6E22E]">V-SONG</h3>
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-[#3E3D32] text-[#66D9EF] rounded uppercase tracking-widest border border-[#49483E]">
                      Archive
                    </span>
                  </div>
                  <p className="text-sm font-bold text-gray-300 leading-snug">
                    버튜버 음악의 모든 순간을 기록합니다.
                  </p>
                  <div className="flex items-center gap-2 pt-2">
                    <span className="relative flex h-2 w-2">
                      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#A6E22E] opacity-75"></span>
                      <span className="relative inline-flex rounded-full h-2 w-2 bg-[#A6E22E]"></span>
                    </span>
                    <span className="text-[10px] font-mono text-[#A6E22E] uppercase tracking-tighter">Real-time Data Tracking Active</span>
                  </div>
                </div>

                {/* Description Column */}
                <div className="space-y-4">
                  <h4 className="text-xs font-black text-gray-400 uppercase tracking-widest">About Project</h4>
                  <p className="text-xs leading-relaxed text-gray-400">
                    V-Song은 버추얼 유튜버들의 음악 데이터를 수집하고 분석하는 아카이브 플랫폼입니다. <br /><br />
                    신곡 탐색부터 주간 조회수 트렌드까지, 데이터 기반의 신뢰할 수 있는 정보를 제공하여 버튜버 음악 생태계의 성장을 기록합니다.
                  </p>
                </div>

                {/* Meta & Links Column */}
                <div className="space-y-4">
                  <h4 className="text-xs font-black text-gray-400 uppercase tracking-widest">Resources</h4>
                  <div className="flex flex-col gap-2 text-xs">
                    <SuggestionModalTrigger />
                    <Link href="/privacy" className="hover:text-[#A6E22E] transition-colors">개인정보처리방침 (Privacy Policy)</Link>
                  </div>
                  <div className="pt-4 text-[10px] text-gray-600">
                    <p>© 2026 V-SONG Archive. All rights reserved.</p>
                    <p className="mt-1 italic">Powered by YouTube Data API v3</p>
                  </div>
                </div>
              </div>
            </footer>
          </div>
        </AuthProvider>
      </body>
    </html>
  );
}
