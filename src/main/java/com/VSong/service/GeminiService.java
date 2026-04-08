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
    private final com.VSong.repository.AiRecommendationLogRepository aiLogRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiService(com.VSong.repository.AiRecommendationLogRepository aiLogRepository) {
        this.aiLogRepository = aiLogRepository;
    }

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

    public boolean isVtuberByAI(String title, String description) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            logger.warn("Gemini API Key가 설정되지 않았습니다. 기본 검증 로직으로 대체합니다.");
            return true;
        }

        try {
            String prompt = String.format(
                "다음 유튜브 채널 정보를 보고, 이 채널이 실제 '버츄얼 유튜버(VTuber, V-Tuber)'의 채널인지 판별해줘. " +
                "실제 사람이 얼굴을 드러내고 방송하는 채널(Face-cam), 단순 애니메이션 채널, 게임 하이라이트/클립 채널, 일반 유튜버는 반드시 'false'여야 해. " +
                "2D 또는 3D 아바타를 사용하여 소통하고 콘텐츠를 제작하는 제작자인지 확인해줘. " +
                "결과는 반드시 'true' 또는 'false' 한 단어로만 응답해줘.\n" +
                "채널명: %s\n" +
                "채널 설명: %s", title, description);

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

            logger.info("Gemini AI 버튜버 판별 결과 ({}): {}", title, aiAnswer);
            return aiAnswer.contains("true");

        } catch (Exception e) {
            logger.error("Gemini API 버튜버 판별 중 오류 발생: {}", e.getMessage());
            return true;
        }
    }

    public List<String> analyzeUserTaste(List<String> titles) {
        return analyzeUserTaste(titles, "Unknown");
    }

    public List<String> analyzeUserTaste(List<String> titles, String userEmail) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            return List.of("J-POP", "애니메이션", "버튜버", "K-POP", "발라드");
        }

        long startTime = System.currentTimeMillis();
        com.VSong.entity.AiRecommendationLog aiLog = new com.VSong.entity.AiRecommendationLog();
        aiLog.setUserEmail(userEmail);
        aiLog.setRequestedAt(java.time.LocalDateTime.now());

        try {
            String combinedTitles = String.join(", ", titles);
            if (combinedTitles.length() > 2000) {
                combinedTitles = combinedTitles.substring(0, 2000);
            }

            String prompt = String.format(
                "다음은 유저가 즐겨듣는 유튜브 재생목록과 영상 제목들이야. 이 제목들을 분석해서 이 유저가 좋아할 만한 '음악적 취향 키워드' 5개를 뽑아줘. " +
                "결과는 반드시 '키워드1, 키워드2, 키워드3, 키워드4, 키워드5' 형식으로 쉼표로 구분해서 응답해줘.\n" +
                "영상 제목들: %s", combinedTitles);

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
                                  .path("text").asText().trim();

            logger.info("Gemini AI 취향 분석 결과: {}", aiAnswer);

            aiLog.setResultKeywords(aiAnswer);
            aiLog.setResponseTimeMs(System.currentTimeMillis() - startTime);
            aiLog.setSuccess(true);
            aiLogRepository.save(aiLog);

            return List.of(aiAnswer.split(",\\s*"));

        } catch (Exception e) {
            logger.error("Gemini AI 취향 분석 중 오류 발생: {}", e.getMessage());

            aiLog.setResponseTimeMs(System.currentTimeMillis() - startTime);
            aiLog.setSuccess(false);
            aiLog.setErrorMessage(e.getMessage());
            aiLogRepository.save(aiLog);

            return List.of("J-POP", "애니메이션", "버튜버", "커버곡", "오리지널");
        }
    }
}
