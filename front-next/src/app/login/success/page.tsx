"use client";

import { useEffect, Suspense } from 'react';
import { useRouter } from 'next/navigation';

const LoginSuccessContent = () => {
    const router = useRouter();

    useEffect(() => {
        // 2초 후에 메인 페이지로 리디렉션
        const timer = setTimeout(() => {
            router.push('/');
        }, 2000);

        return () => clearTimeout(timer); // 컴포넌트 언마운트 시 타이머 제거
    }, [router]);

    return (
        <div style={{ padding: '20px', textAlign: 'center' }}>
            <h1>로그인 성공!</h1>
            <p>잠시 후 메인 페이지로 이동합니다...</p>
        </div>
    );
};

const LoginSuccessPage = () => (
    <Suspense fallback={<div>Loading...</div>}>
        <LoginSuccessContent />
    </Suspense>
);

export default LoginSuccessPage;