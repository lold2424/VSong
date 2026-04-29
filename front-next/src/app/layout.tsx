import type { Metadata } from "next";
import { Suspense } from "react";
import "./globals.css";
import Header from "@/components/Header";
import WeeklyChartContainer from "@/components/WeeklyChartContainer";
import Link from "next/link";

import { AuthProvider } from "@/context/AuthContext";
import { WebVitalsReporter } from "@/components/WebVitalsReporter";

export const metadata: Metadata = {
  metadataBase: new URL("https://www.vsong.site"),
  title: {
    default: "VSong | 버튜버 음악 아카이브 & 트렌드",
    template: "%s | VSong",
  },
  description: "버튜버 음악의 모든 순간을 기록합니다. 실시간 수집되는 버추얼 유튜버들의 최신곡과 주간 인기 트렌드를 VSong에서 만나보세요.",
  icons: {
    icon: '/favicon.ico',
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
                  <h4 className="text-xs font-black text-gray-500 uppercase tracking-widest">About Project</h4>
                  <p className="text-xs leading-relaxed text-gray-400">
                    V-Song은 버추얼 유튜버들의 음악 데이터를 수집하고 분석하는 아카이브 플랫폼입니다. <br /><br />
                    신곡 탐색부터 주간 조회수 트렌드까지, 데이터 기반의 신뢰할 수 있는 정보를 제공하여 버튜버 음악 생태계의 성장을 기록합니다.
                  </p>
                </div>

                {/* Meta & Links Column */}
                <div className="space-y-4">
                  <h4 className="text-xs font-black text-gray-500 uppercase tracking-widest">Resources</h4>
                  <div className="flex flex-col gap-2 text-xs">
                    <Link href="https://github.com/lold2424/vsong" target="_blank" className="hover:text-[#A6E22E] transition-colors flex items-center gap-2">
                      <span>GitHub Repository</span>
                      <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 24 24"><path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/></svg>
                    </Link>
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
