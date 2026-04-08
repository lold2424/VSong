import axios from 'axios';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

export const getYoutubeRecommendations = async () => {
    const response = await api.get('/youtube/recommend');
    return response.data;
};

export const refreshYoutubeRecommendations = async () => {
    const response = await api.post('/youtube/recommend/refresh');
    return response.data;
};

export const getUserPlaylists = async () => {
    const response = await api.get('/youtube/playlists');
    return response.data;
};

export const addSongToPlaylist = async (videoId: string, playlistId: string) => {
    const response = await api.post('/youtube/playlists/add', { videoId, playlistId });
    return response.data;
};
