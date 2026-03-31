package com.VSong.service;

import com.VSong.entity.User;
import com.VSong.repository.UserRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserYouTubeService {

    private static final Logger logger = LoggerFactory.getLogger(UserYouTubeService.class);
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public UserYouTubeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getOrRefreshAccessToken(User user) {
        if (user.getAccessTokenExpiresAt() != null && user.getAccessTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return user.getAccessToken();
        }

        if (user.getRefreshToken() == null) {
            throw new RuntimeException("Refresh Token이 없습니다. 다시 로그인해주세요.");
        }

        logger.info("Access Token 만료됨. Token 갱신 시도 중: {}", user.getEmail());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", user.getRefreshToken());
        map.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://oauth2.googleapis.com/token", request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String newAccessToken = (String) response.getBody().get("access_token");
            Integer expiresIn = (Integer) response.getBody().get("expires_in");

            user.setAccessToken(newAccessToken);
            user.setAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            userRepository.save(user);

            logger.info("Access Token 갱신 성공: {}", user.getEmail());
            return newAccessToken;
        } else {
            throw new RuntimeException("Token 갱신 실패");
        }
    }

    public List<String> getUserPlaylistTitles(User user) throws GeneralSecurityException, IOException {
        String accessToken = getOrRefreshAccessToken(user);
        YouTube youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                null)
                .setApplicationName("VSong")
                .build();

        YouTube.Playlists.List request = youtube.playlists()
                .list(List.of("snippet"))
                .setMine(true)
                .setMaxResults(20L)
                .setOauthToken(accessToken);

        PlaylistListResponse response = request.execute();
        List<String> titles = new ArrayList<>();
        if (response.getItems() != null) {
            for (Playlist playlist : response.getItems()) {
                titles.add(playlist.getSnippet().getTitle());
                titles.addAll(getPlaylistItems(youtube, playlist.getId(), accessToken));
            }
        }
        return titles;
    }

    private List<String> getPlaylistItems(YouTube youtube, String playlistId, String accessToken) throws IOException {
        YouTube.PlaylistItems.List request = youtube.playlistItems()
                .list(List.of("snippet"))
                .setPlaylistId(playlistId)
                .setMaxResults(10L)
                .setOauthToken(accessToken);

        PlaylistItemListResponse response = request.execute();
        if (response.getItems() == null) return List.of();
        return response.getItems().stream()
                .map(item -> item.getSnippet().getTitle())
                .collect(Collectors.toList());
    }

    public void addVideoToPlaylist(User user, String videoId, String playlistId) throws GeneralSecurityException, IOException {
        String accessToken = getOrRefreshAccessToken(user);
        YouTube youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                null)
                .setApplicationName("VSong")
                .build();

        PlaylistItem item = new PlaylistItem();
        PlaylistItemSnippet snippet = new PlaylistItemSnippet();
        snippet.setPlaylistId(playlistId);
        
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);
        snippet.setResourceId(resourceId);
        
        item.setSnippet(snippet);

        youtube.playlistItems()
                .insert(List.of("snippet"), item)
                .setOauthToken(accessToken)
                .execute();
        
        logger.info("재생목록({})에 비디오({}) 추가 완료: {}", playlistId, videoId, user.getEmail());
    }

    public List<Playlist> getUserPlaylists(User user) throws GeneralSecurityException, IOException {
        String accessToken = getOrRefreshAccessToken(user);
        YouTube youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                null)
                .setApplicationName("VSong")
                .build();

        return youtube.playlists()
                .list(List.of("snippet", "contentDetails"))
                .setMine(true)
                .setMaxResults(50L)
                .setOauthToken(accessToken)
                .execute()
                .getItems();
    }
}
