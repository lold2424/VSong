package com.VSong.service;

import com.VSong.repository.ExceptVtuberRepository;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.Video;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VtuberValidationService {

    private static final Logger logger = LoggerFactory.getLogger(VtuberValidationService.class);

    // Constants for Validation
    private static final int MIN_SUBSCRIBERS = 3000;
    private static final List<String> SONG_KEYWORDS = List.of("music", "song", "cover", "original", "official", "mv", "뮤직", "노래", "커버");
    private static final List<String> EXCLUDE_TITLE_KEYWORDS = Arrays.asList(
            "응원", "통계", "번역", "다시보기", "게임", "저장", "일상", "브이로그", "보관", "잼민",
            "TV", "코인", "주식", "Tj", "tv", "팬계정", "창고", "박스", "팬", "클립", "키리누키",
            "vlog", "유튜버", "youtube", "YOUTUBE", "유튜브", "코딩", "코드", "로블록스", "덕질",
            "음식", "기도", "교회", "여행", "VOD", "풀영상"
    );
    private static final List<String> EXCLUDE_DESCRIPTION_KEYWORDS = Arrays.asList(
            "팬클립", "팬영상", "팬채널", "저장소", "브이로그", "학년", "초등학", "중학", "고등학",
            "코딩", "로블록스", "덕질", "음식", "지식", "협찬", "비지니스", "광고", "병맛",
            "종합게임", "종겜", "기도", "교회", "목사", "여행", "애니메이션", "개그", "코미디",
            "뷰티", "fashion", "패션", "vlog", "게임채널", "연주", "강의", "더빙", "일상",
            "다시보기", "풀영상", "쇼츠", "서브채널", "그림", "팬페이지", "개그맨", "다이어트",
            "4K", "커뮤니티", "악세사리", "쇼핑몰", "경제", "키리누키", "채널 옮겼습니다",
            "클립", "넷플릭스", "게이머", "팬 채널", "게임방송", "리뷰", "예술", "Fashion",
            "자기관리", "예능", "희극인", "장애인", "실물"
    );
    private static final List<String> PRIORITY_KEYWORDS = Arrays.asList(
            "버츄얼 유튜버", "버튜버", "V-Youtuber", "버츄얼 유튜버"
    );

    // Keywords from RelatedChannelService
    private final List<String> avatarKeywords = Arrays.asList(
            "아바타", "캐릭터", "모델", "Live2D", "3D", "MMD",
            "가상", "버츄얼", "virtual", "avatar", "character",
            "리깅", "rigging", "모션캡처", "motion capture", "VRM"
    );

    private final List<String> activityKeywords = Arrays.asList(
            "데뷔", "debut", "첫방송", "방송시작", "신인",
            "소속", "기획사", "컴퍼니", "엔터", "프로덕션",
            "잡담", "방송", "노래", "singing", "stream",
            "첫방", "데뷔방", "콜라보", "collaboration"
    );

    private final List<String> vtuberCompanies = Arrays.asList(
            "이세계아이돌", "V-LUP", "RE:REVOLUTION", "VRECORD", "V&U", "일루전 라이브",
            "버츄얼 헤르츠", "V-llage", "레븐", "스타게이저", "싸이코드", "PLAVE", "러브다이아",
            "미츄", "스텔라이브", "뻐스시간", "스타데이즈", "블루점프", "팔레트", "AkaiV Studio",
            "리스텔라", "에스더", "포더모어", "스코시즘", "큐버스", "라이브루리", "플레이 바이 스쿨",
            "VLYZ.", "Artisons.", "HONEYZ", "PROJECT Serenity", "위싱 라이브", "브이아이",
            "몽상컴퍼니", "스테이터스", "아쿠아벨", "Plan.B Music", "멜로데이즈", "Re:AcT KR",
            "GRIM PRODUCTION.", "PJX.", "D-ESTER", "방과후버튜버", "베리타", "스타드림컴퍼니",
            "HANAVI", "UR:L", "Priz", "브이퍼리", "크로아", "하데스", "ACAXIA."
    );

    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final YouTube youTube;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;

    public VtuberValidationService(VtuberRepository vtuberRepository,
                                     ExceptVtuberRepository exceptVtuberRepository,
                                     VtuberSongsRepository vtuberSongsRepository,
                                     YouTube youTube,
                                     @Value("${youtube.api.keys}") String apiKeys) {
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.youTube = youTube;
        this.apiKeys = Arrays.asList(apiKeys.split(","));
    }

    public String getChannelProcessableReason(String channelId) {
        if (exceptVtuberRepository.existsByChannelId(channelId)) {
            return "제외 목록에 포함된 채널";
        }
        if (vtuberRepository.existsByChannelId(channelId)) {
            return "이미 DB에 존재하는 채널";
        }
        return null; // 검증 통과
    }

    public String getKoreanVtuberReason(Channel channel) {
        String channelId = channel.getId();
        String title = channel.getSnippet().getTitle();
        String description = channel.getSnippet().getDescription();

        if (channel.getStatistics().getSubscriberCount().compareTo(BigInteger.valueOf(MIN_SUBSCRIBERS)) < 0) {
            return "구독자 수 미달 (" + channel.getStatistics().getSubscriberCount() + ")";
        }

        boolean containsKoreanInTitle = title.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsKoreanInDescription = description != null && description.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");

        if (!containsKoreanInTitle && !containsKoreanInDescription) {
            return "한국어 없음";
        }

        boolean containsPriorityKeyword = PRIORITY_KEYWORDS.stream().anyMatch(keyword ->
                title.contains(keyword) || (description != null && description.contains(keyword))
        );

        boolean containsExcludeKeywordsInTitle = EXCLUDE_TITLE_KEYWORDS.stream().anyMatch(title::contains);
        boolean containsExcludeKeywordsInDescription = description != null &&
                EXCLUDE_DESCRIPTION_KEYWORDS.stream().anyMatch(description::contains);

        if ((containsExcludeKeywordsInTitle || containsExcludeKeywordsInDescription) && !containsPriorityKeyword) {
            String matchedKeyword = containsExcludeKeywordsInTitle ?
                    EXCLUDE_TITLE_KEYWORDS.stream().filter(title::contains).findFirst().orElse("?") :
                    EXCLUDE_DESCRIPTION_KEYWORDS.stream().filter(description::contains).findFirst().orElse("?");
            return "제외 키워드 포함: " + matchedKeyword;
        }

        boolean hasVtuberKeyword = hasDirectVtuberKeyword(title, description);
        boolean hasVisualCharacteristics = hasVtuberVisualCharacteristics(title, description);
        boolean belongsToCompany = belongsToVtuberCompany(description);
        boolean hasContentPattern = hasVtuberContentPattern(channelId);

        boolean isVtuber = hasVtuberKeyword || hasVisualCharacteristics || belongsToCompany || hasContentPattern || containsPriorityKeyword;

        if (isVtuber) {
            return null; // It's a VTuber, validation passed.
        } else {
            return "버튜버 특징 미발견";
        }
    }

    private boolean hasDirectVtuberKeyword(String title, String description) {
        boolean containsVtuberInTitle = title.contains("버튜버") || title.contains("Vtuber") || title.contains("VTuber");
        boolean containsVtuberInDescription = description != null &&
                (description.contains("버튜버") || description.contains("Vtuber") || description.contains("VTuber"));
        return containsVtuberInTitle || containsVtuberInDescription;
    }

    private boolean hasVtuberVisualCharacteristics(String title, String description) {
        boolean hasAvatarKeyword = avatarKeywords.stream().anyMatch(keyword ->
                title.toLowerCase().contains(keyword.toLowerCase()) ||
                        (description != null && description.toLowerCase().contains(keyword.toLowerCase()))
        );
        boolean hasActivityKeyword = activityKeywords.stream().anyMatch(keyword ->
                title.toLowerCase().contains(keyword.toLowerCase()) ||
                        (description != null && description.toLowerCase().contains(keyword.toLowerCase()))
        );
        return hasAvatarKeyword || hasActivityKeyword;
    }

    private boolean belongsToVtuberCompany(String description) {
        if (description == null) return false;
        return vtuberCompanies.stream()
                .anyMatch(company -> description.toLowerCase().contains(company.toLowerCase()));
    }

    private boolean hasVtuberContentPattern(String channelId) {
        try {
            YouTube.Search.List search = youTube.search().list(List.of("snippet"));
            search.setChannelId(channelId);
            search.setOrder("date");
            search.setMaxResults(10L);
            search.setType(List.of("video"));
            search.setKey(getCurrentApiKey());

            SearchListResponse response = search.execute();
            if (response.getItems() == null || response.getItems().isEmpty()) {
                return false;
            }

            List<String> videoTitles = response.getItems().stream()
                    .map(item -> item.getSnippet().getTitle())
                    .collect(Collectors.toList());

            List<String> vtuberPatterns = Arrays.asList(
                    "잡담", "방송", "게임", "노래", "singing", "stream",
                    "첫방", "데뷔방", "콜라보", "collaboration", "잡담방",
                    "방송시작", "live", "라이브", "스트림"
            );

            long patternMatches = videoTitles.stream()
                    .mapToLong(videoTitle -> vtuberPatterns.stream()
                            .filter(pattern -> videoTitle.toLowerCase().contains(pattern.toLowerCase()))
                            .count())
                    .sum();

            return patternMatches >= 3;
        } catch (Exception e) {
            logger.warn("채널 {} 콘텐츠 패턴 분석 실패: {}", channelId, e.getMessage());
            rotateApiKey();
            return false;
        }
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private synchronized void rotateApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.warn("API 키를 다음 키로 전환했습니다.");
    }

    // Methods for song validation (isSongRelated, etc.) remain unchanged
    private static final List<String> DESCRIPTION_SONG_KEYWORDS = Arrays.asList(
            "lyrics", "가사", "prod", "작곡", "편곡", "믹싱", "mastering", "vocal", "inst",
            "spotify", "melon", "apple music"
    );

    public boolean isSongRelated(Video video) {
        String title = video.getSnippet().getTitle();
        String lowerTitle = title.toLowerCase();
        String description = video.getSnippet().getDescription();
        String lowerDescription = description != null ? description.toLowerCase() : "";
        String videoId = video.getId();

        boolean titleHasExcludeKeyword = EXCLUDE_TITLE_KEYWORDS.stream().anyMatch(lowerTitle::contains);
        boolean descriptionHasExcludeKeyword = EXCLUDE_DESCRIPTION_KEYWORDS.stream().anyMatch(lowerDescription::contains);
        if (titleHasExcludeKeyword || descriptionHasExcludeKeyword) {
            return false;
        }

        String titleMatchedKeyword = SONG_KEYWORDS.stream().filter(lowerTitle::contains).findFirst().orElse(null);
        boolean titleHasSongKeyword = titleMatchedKeyword != null;

        String descriptionMatchedKeyword = DESCRIPTION_SONG_KEYWORDS.stream().filter(lowerDescription::contains).findFirst().orElse(null);
        boolean descriptionHasSongKeyword = descriptionMatchedKeyword != null;

        boolean finalDecision = titleHasSongKeyword || descriptionHasSongKeyword;

        if (finalDecision) {
            logger.info("Validation Check for Video ID: {} -> ACCEPTED", videoId);
        }

        return finalDecision;
    }

    public boolean isSongAlreadyExists(String videoId) {
        return vtuberSongsRepository.existsByVideoId(videoId);
    }

    public String classifyVideo(Video video) {
        String title = video.getSnippet().getTitle();
        String durationStr = video.getContentDetails().getDuration();
        if (durationStr == null) return "ignore";
        try {
            Duration videoDuration = Duration.parse(durationStr);
            if (videoDuration.compareTo(Duration.ofMinutes(8)) > 0) return "ignore";
            if (videoDuration.compareTo(Duration.ofMinutes(1)) <= 0 || title.toLowerCase().contains("short")) return "shorts";
            return "videos";
        } catch (Exception e) {
            logger.error("Error parsing duration '{}' for videoId: {}. Ignoring.", durationStr, video.getId(), e);
            return "ignore";
        }
    }
}
