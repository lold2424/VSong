import React, { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import {getUserPlaylists, addSongToPlaylist, createPlaylist} from '@/utils/apiClient';

interface VideoModalProps {
    videoId: string;
    onClose: () => void;
}

const VideoModal: React.FC<VideoModalProps> = ({ videoId, onClose }) => {
    const { isLoggedIn } = useAuth();
    const [playlists, setPlaylists] = useState<any[]>([]);
    const [showPlaylistSelector, setShowPlaylistSelector] = useState(false);
    const [isLoadingPlaylists, setIsLoadingPlaylists] = useState(false);
    const [isAdding, setIsAdding] = useState(false);
    const [newPlaylistTitle, setNewPlaylistTitle] = useState('');
    const [isCreating, setIsCreating] = useState(false);

    const handleKeyDown = (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
            onClose();
        }
    };

    const handleBackgroundClick = (event: React.MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLDivElement).classList.contains('video-modal')) {
            onClose();
        }
    };

    const handleTogglePlaylistSelector = async () => {
        if (!showPlaylistSelector && playlists.length === 0) {
            setIsLoadingPlaylists(true);
            try {
                const data = await getUserPlaylists();
                setPlaylists(data);
            } catch (err) {
                console.error("재생목록 로딩 실패:", err);
            } finally {
                setIsLoadingPlaylists(false);
            }
        }
        setShowPlaylistSelector(!showPlaylistSelector);
    };

    const handleAddToPlaylist = async (playlistId: string) => {
        setIsAdding(true);
        try {
            await addSongToPlaylist(videoId, playlistId);
            alert("재생목록에 추가되었습니다!");
            setShowPlaylistSelector(false);
        } catch (err) {
            console.error("추가 실패:", err);
            alert("노래를 추가하는 중 오류가 발생했습니다.");
        } finally {
            setIsAdding(false);
        }
    };

    const handleCreateAndAdd = async () => {
        if (!newPlaylistTitle.trim()) {
            alert("재생목록 제목을 입력해주세요.");
            return;
        }

        setIsCreating(true);
        try {
            const newPlaylist = await createPlaylist(newPlaylistTitle);

            await addSongToPlaylist(videoId, newPlaylist.id);

            alert(`'${newPlaylistTitle}' 재생목록이 생성되고 노래가 추가되었습니다!`);

            setNewPlaylistTitle('');
            setShowPlaylistSelector(false);
            setPlaylists([]);
        } catch (err) {
            console.error("재생목록 생성 및 추가 실패:", err);
            alert("재생목록 생성 중 오류가 발생했습니다.");
        } finally {
            setIsCreating(false);
        }
    };
    useEffect(() => {
        window.addEventListener('keydown', handleKeyDown);
        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, []);

    return (
        <div className="video-modal fixed inset-0 w-full h-full bg-black bg-opacity-80 flex justify-center items-center z-[1000]" onClick={handleBackgroundClick}>
            <div className="relative w-full max-w-6xl p-8">
                <button
                    onClick={onClose}
                    className="absolute top-0 right-2 text-white text-4xl font-bold z-10 leading-none hover:text-gray-300 transition-colors"
                    aria-label="Close"
                >
                    &times;
                </button>
                
                <div className="flex flex-col gap-4">
                    <div className="aspect-video bg-black rounded-lg overflow-hidden shadow-2xl">
                        <iframe
                            className="w-full h-full"
                            src={`https://www.youtube.com/embed/${videoId}?autoplay=1`}
                            title="YouTube video player"
                            frameBorder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowFullScreen
                        ></iframe>
                    </div>

                    {isLoggedIn && (
                        <div className="relative self-end">
                            <button 
                                onClick={handleTogglePlaylistSelector}
                                className="flex items-center gap-2 px-4 py-2 bg-[#A6E22E] text-black font-bold rounded-lg hover:bg-[#bfef5a] transition-colors"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" />
                                </svg>
                                내 재생목록에 추가
                            </button>

                            {showPlaylistSelector && (
                                <div className="absolute bottom-full right-0 mb-2 w-72 bg-[#272822] border border-[#3E3D32] rounded-lg shadow-xl overflow-hidden z-[1001]">
                                    <div className="p-3 border-b border-[#3E3D32] text-[#A6E22E] font-bold text-sm">
                                        기존 재생목록 선택
                                    </div>
                                    <div className="max-h-48 overflow-y-auto">
                                        {isLoadingPlaylists ? (
                                            <div className="p-4 text-center text-gray-400 text-sm">로딩 중...</div>
                                        ) : playlists.length === 0 ? (
                                            <div className="p-4 text-center text-gray-400 text-sm">재생목록이 없습니다.</div>
                                        ) : (
                                            playlists.map((playlist: any) => (
                                                <button
                                                    key={playlist.id}
                                                    onClick={() => handleAddToPlaylist(playlist.id)}
                                                    disabled={isAdding || isCreating}
                                                    className="w-full text-left px-4 py-3 text-[#F8F8F2] text-sm hover:bg-[#3E3D32] transition-colors border-b border-[#3E3D32] last:border-0 truncate"
                                                >
                                                    {playlist.snippet.title}
                                                </button>
                                            ))
                                        )}
                                    </div>
                                    
                                    {/* 새 재생목록 만들기 섹션 */}
                                    <div className="p-3 bg-[#1e1f1c] border-t border-[#3E3D32]">
                                        <div className="flex flex-col gap-2">
                                            <input 
                                                type="text"
                                                value={newPlaylistTitle}
                                                onChange={(e) => setNewPlaylistTitle(e.target.value)}
                                                placeholder="새 재생목록 이름"
                                                className="bg-[#272822] border border-[#3E3D32] text-[#F8F8F2] text-xs p-2 rounded outline-none focus:border-[#A6E22E] transition-colors"
                                                disabled={isCreating}
                                            />
                                            <button
                                                onClick={handleCreateAndAdd}
                                                disabled={isCreating || !newPlaylistTitle.trim()}
                                                className="w-full py-2 bg-[#3E3D32] text-[#A6E22E] text-xs font-bold rounded hover:bg-[#4E4D42] transition-colors disabled:opacity-50"
                                            >
                                                {isCreating ? '생성 중...' : '목록 생성 후 노래 추가'}
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default VideoModal;
