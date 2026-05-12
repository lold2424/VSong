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