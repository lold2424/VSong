import React from 'react';

const PrivacyPolicyPage: React.FC = () => {
    return (
        <div className="max-w-4xl mx-auto p-8 bg-[#272822] text-[#F8F8F2] min-h-screen leading-relaxed">
            <h1 className="text-3xl font-bold mb-6 text-[#A6E22E]">개인정보처리방침</h1>
            <p className="mb-8 text-gray-400"><strong>최종 수정일: 2026년 4월 8일</strong></p>

            <section className="mb-8">
                <h2 className="text-xl font-bold mb-4 text-[#66D9EF]">1. 수집하는 개인정보의 항목 및 수집 방법</h2>
                <p className="mb-2">VSong 서비스(이하 '서비스')는 서비스 제공을 위해 사용자의 동의를 바탕으로 다음과 같은 정보를 수집합니다.</p>
                <h3 className="text-lg font-semibold mb-2 text-[#FD971F]">가. Google OAuth 2.0 및 YouTube API를 통해 제공받는 정보</h3>
                <ul className="list-disc ml-6 mb-4">
                    <li>필수: 이름, 이메일 주소, 프로필 사진 URL</li>
                    <li>선택(취향 분석 및 재생목록 관리 시): YouTube 재생목록 제목, 재생목록 내 영상 제목, 재생목록 ID</li>
                </ul>
                <h3 className="text-lg font-semibold mb-2 text-[#FD971F]">나. 서비스 이용 과정에서 자동으로 생성되는 정보</h3>
                <ul className="list-disc ml-6">
                    <li>방문 기록, 접속 로그, AI 취향 분석 이력(분석 결과, 소요 시간, 성공 여부)</li>
                </ul>
            </section>

            <section className="mb-8">
                <h2 className="text-xl font-bold mb-4 text-[#66D9EF]">2. 개인정보의 수집 및 이용 목적</h2>
                <p className="mb-2">서비스는 수집한 정보를 다음의 목적으로만 이용합니다.</p>
                <ul className="list-disc ml-6">
                    <li><strong>사용자 식별 및 프로필 관리</strong>: 로그인한 사용자의 식별 및 이름/사진 표시</li>
                    <li><strong>AI 기반 맞춤형 추천</strong>: 사용자의 YouTube 활동 데이터를 바탕으로 Google Gemini AI를 통한 음악적 취향 분석 및 버튜버 곡 추천</li>
                    <li><strong>YouTube 기능 제공</strong>: 사용자의 명시적 요청에 따른 YouTube 재생목록 생성 및 노래 추가 기능 대행</li>
                    <li><strong>서비스 개선 및 모니터링</strong>: 추천 시스템의 성능 측정, 오류 진단 및 서비스 최적화</li>
                </ul>
            </section>

            <section className="mb-8">
                <h2 className="text-xl font-bold mb-4 text-[#66D9EF]">3. 데이터의 제3자 제공 및 위탁</h2>
                <p className="mb-2">서비스는 원활한 AI 분석 기능을 제공하기 위해 아래와 같이 외부 서비스를 활용합니다.</p>
                <div className="bg-[#3E3D32] p-4 rounded-lg border border-[#A6E22E] border-opacity-20">
                    <p className="font-bold text-[#A6E22E] mb-2">Google Gemini API (Google Cloud Platform)</p>
                    <ul className="list-disc ml-6 text-sm">
                        <li>제공 항목: YouTube 재생목록 및 영상 제목 (이메일 등 개인 식별 정보 제외)</li>
                        <li>이용 목적: 사용자 취향 키워드 추출 및 분석</li>
                        <li>보유 기간: API 호출 후 분석 완료 시까지</li>
                    </ul>
                </div>
            </section>

            <section className="mb-8">
                <h2 className="text-xl font-bold mb-4 text-[#66D9EF]">4. 개인정보의 보유 및 이용기간</h2>
                <p>
                    서비스는 사용자가 회원 탈퇴를 요청하거나 Google 계정에서 서비스 액세스 권한을 철회할 때까지 정보를 보유합니다. 
                    단, 서비스 모니터링을 위한 로그 데이터는 통계 목적으로 비식별화하여 보관될 수 있습니다.
                </p>
            </section>

            <section className="mb-8">
                <h2 className="text-xl font-bold mb-4 text-[#66D9EF]">5. 정보주체의 권리</h2>
                <p>
                    사용자는 언제든지 자신의 개인정보를 조회하거나 수정할 수 있으며, Google 계정 설정을 통해 서비스에 부여된 YouTube 관리 권한을 철회할 수 있습니다. 
                    권한 철회 시 취향 맞춤 추천 및 재생목록 관리 기능의 이용이 제한될 수 있습니다.
                </p>
            </section>

            <section>
                <h2 className="text-xl font-bold mb-4 text-[#66D9EF]">6. 문의처</h2>
                <p>
                    본 방침과 관련하여 문의사항이 있으신 경우 서비스 내 관리자 또는 프로젝트 저장소(GitHub)를 통해 연락 주시기 바랍니다.
                </p>
            </section>
        </div>
    );
};

export default PrivacyPolicyPage;
