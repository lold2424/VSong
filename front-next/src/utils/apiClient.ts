import axios from 'axios';

export const apiClient = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

// 유튜브 추천 관련
export const getYoutubeRecommendations = async () => {
    const response = await apiClient.get('/youtube/recommend');
    return response.data;
};

export const refreshYoutubeRecommendations = async () => {
    const response = await apiClient.post('/youtube/recommend/refresh');
    return response.data;
};

export const getUserPlaylists = async () => {
    const response = await apiClient.get('/youtube/playlists');
    return response.data;
};

export const addSongToPlaylist = async (videoId: string, playlistId: string) => {
    const response = await apiClient.post('/youtube/playlists/add', { videoId, playlistId });
    return response.data;
};

// 유저 정보 조회 (전역용)
export const fetchUserInfoApi = async () => {
    const response = await apiClient.get('/login/userinfo');
    return response.data;
};

// 로그아웃 (전역용)
export const logoutApi = async () => {
    const response = await apiClient.post('/logout');
    return response.data;
};
