import React, { useEffect } from 'react';


interface VideoModalProps {
    videoId: string;
    onClose: () => void;
}

const VideoModal: React.FC<VideoModalProps> = ({ videoId, onClose }) => {
    // 키보드 이벤트 핸들러
    const handleKeyDown = (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
            onClose();
        }
    };

    // 모달 외부 클릭 핸들러
    const handleBackgroundClick = (event: React.MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLDivElement).classList.contains('video-modal')) {
            onClose();
        }
    };

    // 키보드 이벤트 리스너 등록 및 해제
    useEffect(() => {
        window.addEventListener('keydown', handleKeyDown);
        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, []);

    return (
        <div className="fixed inset-0 w-full h-full bg-black bg-opacity-80 flex justify-center items-center z-[1000]" onClick={handleBackgroundClick}>
            <div className="relative w-4/5 max-w-2xl h-3/5 bg-black rounded-lg overflow-hidden">
                <iframe
                    width="100%"
                    height="100%"
                    src={`https://www.youtube.com/embed/${videoId}`}
                    title="YouTube video player"
                    frameBorder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowFullScreen
                ></iframe>
            </div>
        </div>
    );
};

export default VideoModal;
