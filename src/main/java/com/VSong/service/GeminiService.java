package com.VSong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public boolean isSongByAI(String title, String description) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            logger.warn("Gemini API Key가 설정되지 않았습니다. 기본 검증 로직으로 대체합니다.");
            return true;
        }

        try {
            String prompt = String.format(
                "다음 유튜브 영상 정보를 보고, 이 영상이 실제 '노래(커버곡, MV, 오리지널 곡, 리믹스 포함)'인지 판별해줘. " +
                "단순 게임 방송, 잡담, 짧은 웃음 클립, 1분 미만의 쇼츠(Shorts) 영상, 노래 제목이 언급된 BGM 정보는 반드시 제외해야 해. " +
                "결과는 반드시 'true' 또는 'false' 한 단어로만 응답해줘.\n" +
                "영상 제목: %s\n" +
                "영상 설명: %s", title, description);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", prompt)));
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(apiUrl + apiKey, entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            String aiAnswer = root.path("candidates").get(0)
                                  .path("content").path("parts").get(0)
                                  .path("text").asText().trim().toLowerCase();

            logger.info("Gemini AI 판별 결과 ({}): {}", title, aiAnswer);
            return aiAnswer.contains("true");

        } catch (Exception e) {
            logger.error("Gemini API 호출 중 오류 발생: {}", e.getMessage());
            return true;
        }
    }
}
