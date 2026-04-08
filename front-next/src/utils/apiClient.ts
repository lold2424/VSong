import axios from 'axios';

export const apiClient = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

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

export const createPlaylist = async (title: string) => {
    const response = await apiClient.post('/youtube/playlists/create', { title });
    return response.data;
};

export const addSongToPlaylist = async (videoId: string, playlistId: string) => {
    const response = await apiClient.post('/youtube/playlists/add', { videoId, playlistId });
    return response.data;
};

export const fetchUserInfoApi = async () => {
    const response = await apiClient.get('/login/userinfo');
    return response.data;
};

export const logoutApi = async () => {
    const response = await apiClient.post('/logout');
    return response.data;
};
