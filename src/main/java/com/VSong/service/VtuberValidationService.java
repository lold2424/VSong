package com.VSong.service;

import com.VSong.repository.ExceptVtuberRepository;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.Video;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

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

    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;

    public VtuberValidationService(VtuberRepository vtuberRepository, ExceptVtuberRepository exceptVtuberRepository, VtuberSongsRepository vtuberSongsRepository) {
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
    }

    /**
     * 채널이 처리 가능한 상태인지 검증하고, 실패 시 이유를 반환합니다.
     * @return null if processable, otherwise returns a reason string.
     */
    public String getChannelProcessableReason(String channelId) {
        if (exceptVtuberRepository.existsByChannelId(channelId)) {
            return "제외 목록에 포함된 채널";
        }
        if (vtuberRepository.existsByChannelId(channelId)) {
            return "이미 DB에 존재하는 채널";
        }
        return null; // 검증 통과
    }

    /**
     * 채널이 한국 버튜버인지 검증하고, 실패 시 이유를 반환합니다.
     * @return null if valid, otherwise returns a reason string.
     */
    public String getKoreanVtuberReason(Channel channel) {
        String title = channel.getSnippet().getTitle();
        String description = channel.getSnippet().getDescription();

        if (channel.getStatistics().getSubscriberCount().compareTo(BigInteger.valueOf(MIN_SUBSCRIBERS)) < 0) {
            return "구독자 수 미달 (" + channel.getStatistics().getSubscriberCount() + ")";
        }

        boolean containsPriorityKeyword = PRIORITY_KEYWORDS.stream().anyMatch(keyword ->
                title.contains(keyword) || (description != null && description.contains(keyword))
        );
        if (containsPriorityKeyword) {
            return null; // 우선순위 키워드가 있으면 다른 조건 안 보고 통과
        }

        boolean containsExcludeKeywordsInTitle = EXCLUDE_TITLE_KEYWORDS.stream().anyMatch(title::contains);
        if (containsExcludeKeywordsInTitle) {
            return "제목에 제외 키워드 포함";
        }

        boolean containsExcludeKeywordsInDescription = description != null && EXCLUDE_DESCRIPTION_KEYWORDS.stream().anyMatch(description::contains);
        if (containsExcludeKeywordsInDescription) {
            return "설명에 제외 키워드 포함";
        }

        boolean containsKoreanInTitle = title.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsKoreanInDescription = description != null && description.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");

        if (containsKoreanInTitle || containsKoreanInDescription) {
            return null; // 한글이 포함되어 있으면 통과
        }

        return "한국 버튜버 조건 미충족"; // 모든 조건을 통과하지 못한 경우
    }

    public boolean isSongRelated(Video video) {
        String lowerTitle = video.getSnippet().getTitle().toLowerCase();
        boolean keywordMatch = SONG_KEYWORDS.stream().anyMatch(lowerTitle::contains);
        boolean categoryMatch = "10".equals(video.getSnippet().getCategoryId());
        String classification = classifyVideo(video);
        if ("shorts".equals(classification) && !categoryMatch) {
            return false;
        }
        return keywordMatch || categoryMatch;
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