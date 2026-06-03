"use client";

import { useEffect, Suspense } from 'react';
import { useRouter } from 'next/navigation';

const LoginSuccessContent = () => {
    const router = useRouter();

    useEffect(() => {
        const timer = setTimeout(() => {
            router.push('/');
        }, 2000);

        return () => clearTimeout(timer);
    }, [router]);

    return (
        <main className="flex flex-col items-center justify-center min-h-[60vh] p-5 text-center bg-[#272822] text-[#F8F8F2]">
            <div 
                role="status" 
                aria-live="polite" 
                className="bg-[#3E3D32] p-10 rounded-2xl shadow-2xl border border-[#A6E22E]"
            >
                <h1 className="text-3xl font-bold text-[#A6E22E] mb-4">로그인 성공!</h1>
                <p className="text-lg text-gray-300">잠시 후 메인 페이지로 이동합니다...</p>
                <div className="mt-8 flex justify-center">
                    <div className="w-12 h-12 border-4 border-[#A6E22E] border-t-transparent rounded-full animate-spin"></div>
                </div>
            </div>
        </main>
    );
};

const LoginSuccessPage = () => (
    <Suspense fallback={<div>Loading...</div>}>
        <LoginSuccessContent />
    </Suspense>
);

export default LoginSuccessPage;