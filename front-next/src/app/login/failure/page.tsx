"use client";

import { useEffect, Suspense } from 'react';
import { useRouter } from 'next/navigation';

const LoginFailureContent = () => {
    const router = useRouter();

    useEffect(() => {
        const timer = setTimeout(() => {
            router.push('/');
        }, 3000);

        return () => clearTimeout(timer);
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