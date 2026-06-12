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

                        <div className="video-modal fixed inset-0 w-full h-full bg-black bg-opacity-80 flex justify-center items-center z-[1000]" onClick={handleBackgroundClick}>

                            <div className="relative w-full max-w-6xl p-8"> {/* Wrapper with padding */}

                                <button

                                    onClick={onClose}

                                    className="absolute top-0 right-2 text-white text-4xl font-bold z-10 leading-none hover:text-gray-300 transition-colors"

                                    aria-label="Close"

                                >

                                    &times;

                                </button>

                                <div className="aspect-video bg-black rounded-lg overflow-hidden">

                                    <iframe

                                        className="w-full h-full"

                                        src={`https://www.youtube.com/embed/${videoId}`}

                                        title="YouTube video player"

                                        frameBorder="0"

                                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"

                                        allowFullScreen

                                    ></iframe>

                                </div>

                            </div>

                        </div>

                    );
};

export default VideoModal;
