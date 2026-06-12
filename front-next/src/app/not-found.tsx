import Link from 'next/link';

export default function NotFound() {
    return (
        <div style={{ padding: '40px', textAlign: 'center' }}>
            <h1>404 - 페이지를 찾을 수 없습니다</h1>
            <p>요청하신 페이지가 존재하지 않습니다.</p>
            <Link href="/" style={{ color: 'blue', textDecoration: 'underline' }}>
                메인 페이지로 돌아가기
            </Link>
        </div>
    );
}
