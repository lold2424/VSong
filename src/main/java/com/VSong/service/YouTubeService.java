package com.VSong.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class YouTubeService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String updatePlaylist(String accessToken, String playlistId, Map<String, Object> updateData) {
        String url = "https://www.googleapis.com/youtube/v3/playlists?part=snippet&id=" + playlistId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return "플레이리스트 업데이트 성공";
        }
        throw new RuntimeException("플레이리스트 업데이트 실패");
    }
}
