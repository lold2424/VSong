"use client";

import { useEffect, Suspense } from 'react';
import { useRouter } from 'next/navigation';

const LoginFailureContent = () => {
    const router = useRouter();

    useEffect(() => {
        // 3초 후에 메인 페이지로 리디렉션
        const timer = setTimeout(() => {
            router.push('/');
        }, 3000);

        return () => clearTimeout(timer); // 컴포넌트 언마운트 시 타이머 제거
    }, [router]);

    return (
        <div style={{ padding: '20px', textAlign: 'center', color: 'red' }}>
            <h1>로그인 실패</h1>
            <p>로그인에 실패했습니다. 잠시 후 메인 페이지로 이동합니다.</p>
        </div>
    );
};

const LoginFailurePage = () => (
    <Suspense fallback={<div>Loading...</div>}>
        <LoginFailureContent />
    </Suspense>
);


export default LoginFailurePage;