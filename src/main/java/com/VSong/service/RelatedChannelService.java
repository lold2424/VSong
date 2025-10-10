package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.ExceptVtuberRepository;
import com.VSong.repository.VtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatedChannelService {

    private static final Logger logger = LoggerFactory.getLogger(RelatedChannelService.class);
    private static final int MIN_SUBSCRIBERS = 3000;

    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final YouTube youTube;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;

    // UploadVtuberService에서 필터링 로직을 가져옴
    private final List<String> excludeKeywordsInTitle = Arrays.asList(
            "응원", "통계", "번역", "다시보기", "게임", "저장", "일상", "브이로그", "보관", "잼민",
            "TV", "코인", "주식", "Tj", "tv", "팬계정", "창고", "박스", "팬", "클립", "키리누키",
            "vlog", "유튜버", "youtube", "YOUTUBE", "유튜브", "코딩", "코드", "로블록스", "덕질",
            "음식", "기도", "교회", "여행", "VOD", "풀영상"
    );
    private final List<String> excludeKeywordsInDescription = Arrays.asList(
            "팬클립", "팬영상", "팬채널", "저장소", "브이로그", "학년", "초등학", "중학", "고등학",
            "코딩", "로블록스", "덕질", "음식", "지식", "협찬", "비지니스", "광고", "병맛",
            "종합게임", "종겜", "기도", "교회", "목사", "여행", "애니메이션", "개그", "코미디",
            "뷰티", "fashion", "패션", "vlog", "게임채널", "연주", "강의", "더빙", "일상",
            "다시보기", "풀영상", "쇼츠", "서브채널", "그림", "팬페이지", "개그맨", "다이어트",
            "4K", "커뮤니티", "악세사리", "쇼핑몰", "경제", "키리누키", "채널 옮겼습니다",
            "클립", "넷플릭스", "게이머", "팬 채널", "게임방송", "리뷰", "예술", "Fashion",
            "자기관리", "예능", "희극인", "장애인", "실물"
    );
    private final List<String> priorityKeywords = Arrays.asList(
            "버츄얼 유튜버", "버튜버", "V-Youtuber", "버츄얼 유튜버"
    );

    // 새로운 감지 키워드들
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
            "이세계아이돌",
            "V-LUP",
            "RE:REVOLUTION",
            "VRECORD",
            "V&U",
            "일루전 라이브",
            "버츄얼 헤르츠",
            "V-llage",
            "레븐",
            "스타게이저",
            "싸이코드",
            "PLAVE",
            "러브다이아",
            "미츄",
            "스텔라이브",
            "뻐스시간",
            "스타데이즈",
            "블루점프",
            "팔레트",
            "AkaiV Studio",
            "리스텔라",
            "에스더",
            "포더모어",
            "스코시즘",
            "큐버스",
            "라이브루리",
            "플레이 바이 스쿨",
            "VLYZ.",
            "Artisons.",
            "HONEYZ",
            "PROJECT Serenity",
            "위싱 라이브",
            "브이아이",
            "몽상컴퍼니",
            "스테이터스",
            "아쿠아벨",
            "Plan.B Music",
            "멜로데이즈",
            "Re:AcT KR",
            "GRIM PRODUCTION.",
            "PJX.",
            "D-ESTER",
            "방과후버튜버",
            "베리타",
            "스타드림컴퍼니",
            "HANAVI",
            "UR:L",
            "Priz",
            "브이퍼리",
            "크로아",
            "하데스",
            "ACAXIA."
    );

    public RelatedChannelService(VtuberRepository vtuberRepository,
                                 ExceptVtuberRepository exceptVtuberRepository,
                                 YouTube youTube,
                                 @Value("${youtube.api.keys}") String apiKeys) {
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.youTube = youTube;
        this.apiKeys = Arrays.asList(apiKeys.split(","));
    }

    public void discoverAndSaveFromRelatedChannels() {
        logger.info("=== 관련 채널 기반 버튜버 탐색 시작 ===");

        List<String> existingVtuberIds = vtuberRepository.findAllChannelIds();
        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        Set<String> allKnownIds = new HashSet<>(existingVtuberIds);
        allKnownIds.addAll(exceptChannelIds);

        int batchSize = 10;
        Set<String> allDiscoveredChannelIds = new HashSet<>();

        for (int i = 0; i < existingVtuberIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, existingVtuberIds.size());
            List<String> batch = existingVtuberIds.subList(i, end);

            logger.info("배치 {}/{} 처리 중...", (i/batchSize) + 1, (existingVtuberIds.size() + batchSize - 1) / batchSize);

            Set<String> batchResults = processBatchSequentially(batch, allKnownIds);
            allDiscoveredChannelIds.addAll(batchResults);

            try {
                Thread.sleep(1000);
                System.gc();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("총 {}개의 새로운 관련 채널 ID를 발견했습니다.", allDiscoveredChannelIds.size());

        if (!allDiscoveredChannelIds.isEmpty()) {
            processDiscoveredChannels(allDiscoveredChannelIds);
        }

        logger.info("=== 관련 채널 기반 버튜버 탐색 종료 ===");
    }

    private Set<String> processBatchSequentially(List<String> batch, Set<String> allKnownIds) {
        Set<String> discoveredIds = new HashSet<>();
        WebDriver driver = createOptimizedChromeDriver();

        try {
            for (String channelId : batch) {
                String url = "https://www.youtube.com/channel/" + channelId + "/channels";
                logger.info("채널 탐색 중: " + channelId);

                try {
                    driver.get(url);

                    // 페이지 로딩 대기 시간 증가
                    Thread.sleep(5000);

                    // 페이지 소스 일부 로깅 (디버깅용)
                    String pageSource = driver.getPageSource();
                    logger.debug("페이지 길이: {} 문자", pageSource.length());

                    // 다양한 셀렉터로 시도
                    List<String> selectors = Arrays.asList(
                            "ytd-channel-renderer a.yt-simple-endpoint",
                            "ytd-channel-renderer a[href*='/channel/']",
                            "a[href*='/channel/']",
                            "ytd-channel-renderer",
                            "#contents ytd-channel-renderer a"
                    );

                    boolean foundAny = false;

                    for (String selector : selectors) {
                        List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                        logger.info("셀렉터 '{}': {}개 요소 발견", selector, elements.size());

                        if (!elements.isEmpty()) {
                            foundAny = true;

                            for (WebElement element : elements) {
                                try {
                                    String href = element.getAttribute("href");
                                    String text = element.getText();
                                    logger.debug("발견된 요소 - href: {}, text: {}", href, text);

                                    if (href != null && href.contains("/channel/")) {
                                        String discoveredId = extractChannelId(href);
                                        if (discoveredId != null && !allKnownIds.contains(discoveredId)) {

                                            // YouTube 공식 채널 체크 추가
                                            if (isYouTubeOfficialChannel(discoveredId, text)) {
                                                logger.debug("YouTube 공식 채널로 필터링: {} ({})", text, discoveredId);
                                                continue;
                                            }

                                            discoveredIds.add(discoveredId);
                                            logger.info("새로운 관련 채널 발견: {}", discoveredId);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.warn("요소 처리 중 오류", e);
                                }
                            }
                            break; // 첫 번째로 성공한 셀렉터만 사용
                        }
                    }

                    if (!foundAny) {
                        logger.warn("채널 {} - 어떤 관련 채널도 찾을 수 없음", channelId);

                        // 페이지에 "채널" 탭이 있는지 확인
                        List<WebElement> tabs = driver.findElements(By.cssSelector("paper-tab, yt-tab-shape"));
                        logger.info("발견된 탭 수: {}", tabs.size());
                        for (WebElement tab : tabs) {
                            logger.debug("탭 텍스트: {}", tab.getText());
                        }
                    }

                } catch (Exception e) {
                    logger.error("채널 페이지 스크래핑 중 오류 발생: " + url, e);
                }
            }
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    logger.warn("WebDriver 종료 중 오류", e);
                }
            }
        }

        return discoveredIds;
    }

    private String extractChannelId(String href) {
        try {
            if (href.contains("/channel/")) {
                int startIndex = href.indexOf("/channel/") + 9;
                int endIndex = href.indexOf("?", startIndex);
                if (endIndex == -1) endIndex = href.indexOf("#", startIndex);
                if (endIndex == -1) endIndex = href.length();

                String channelId = href.substring(startIndex, endIndex);
                return channelId.matches("UC[a-zA-Z0-9_-]{22}") ? channelId : null;
            }
            return null;
        } catch (Exception e) {
            logger.warn("채널 ID 추출 중 오류: {}", href, e);
            return null;
        }
    }

    private WebDriver createOptimizedChromeDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-web-security");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--disable-dev-tools");
        options.addArguments("--disable-logging");
        options.addArguments("--log-level=3");
        options.addArguments("--silent");
        options.addArguments("--window-size=1366,768");

        options.addArguments("--memory-pressure-off");
        options.addArguments("--max_old_space_size=256");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-backgrounding-occluded-windows");

        options.addArguments("--disable-images");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-java");
        options.addArguments("--disable-audio");
        options.addArguments("--mute-audio");
        options.addArguments("--disable-audio-output");
        options.addArguments("--disable-background-media-suspend");
        options.addArguments("--autoplay-policy=user-gesture-required");
        options.addArguments("--disable-features=MediaSessionService");
        options.addArguments("--disable-media-stream");
        options.addArguments("--disable-rtc-smoothness-algorithm");

        options.addArguments("--aggressive-cache-discard");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-translate");
        options.addArguments("--hide-scrollbars");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--no-first-run");
        options.addArguments("--safebrowsing-disable-auto-update");
        options.addArguments("--disable-permissions-api");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        prefs.put("profile.managed_default_content_settings.plugins", 2);
        prefs.put("profile.managed_default_content_settings.media_stream", 2);
        prefs.put("profile.default_content_setting_values.media_stream_mic", 2);
        prefs.put("profile.default_content_setting_values.media_stream_camera", 2);
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);

        options.setExperimentalOption("prefs", prefs);

        try {
            ChromeDriverService service = ChromeDriverService.createDefaultService();
            service.start();
            return new RemoteWebDriver(service.getUrl(), options);
        } catch (Exception e) {
            logger.error("ChromeDriver 생성 중 오류 발생", e);
            return new ChromeDriver(options);
        }
    }

    private void processDiscoveredChannels(Set<String> channelIds) {
        logger.info("새로 발견된 채널 정보 처리 시작. 대상: {}개", channelIds.size());
        logger.info("발견된 채널 ID 목록: {}", String.join(", ", channelIds));

        try {
            YouTube.Channels.List channelRequest = youTube.channels().list(List.of("snippet","statistics"));
            channelRequest.setId(new java.util.ArrayList<>(channelIds));
            channelRequest.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");
            channelRequest.setKey(getCurrentApiKey());

            ChannelListResponse channelResponse = channelRequest.execute();
            List<Channel> channels = channelResponse.getItems();

            if (channels == null || channels.isEmpty()) {
                logger.warn("API로부터 채널 정보를 가져오지 못했습니다.");
                return;
            }

            logger.info("API로부터 {}개 채널 정보 조회 완료", channels.size());

            int savedCount = 0;
            int filteredOutCount = 0;

            for (Channel channel : channels) {
                String channelId = channel.getId();
                String channelTitle = channel.getSnippet().getTitle();
                BigInteger subscriberCount = channel.getStatistics().getSubscriberCount();

                logger.info("채널 분석 중 - ID: {}, 제목: {}, 구독자: {}",
                        channelId, channelTitle, subscriberCount);

                // 구독자 수 검사
                if (subscriberCount.compareTo(BigInteger.valueOf(MIN_SUBSCRIBERS)) < 0) {
                    logger.info("구독자 수 부족으로 필터링: {} ({}명)", channelTitle, subscriberCount);
                    filteredOutCount++;
                    continue;
                }

                // 한국 버튜버 검사
                if (!isKoreanVtuber(channel)) {
                    filteredOutCount++;
                    continue;
                }

                // DB 중복 검사
                if (vtuberRepository.findByChannelId(channelId).isPresent()) {
                    logger.info("이미 DB에 존재하는 채널: {}", channelTitle);
                    filteredOutCount++;
                    continue;
                }

                // 저장 조건 모두 만족
                logger.info("저장 조건 만족 - 새 버튜버 저장: {}", channelTitle);
                saveNewVtuber(channel);
                savedCount++;
            }

            logger.info("채널 처리 완료 - 저장: {}개, 필터링: {}개, 총 처리: {}개",
                    savedCount, filteredOutCount, channels.size());

        } catch (IOException e) {
            logger.error("YouTube API 호출 중 오류 발생", e);
            rotateApiKey();
        }
    }

    private void saveNewVtuber(Channel channel) {
        VtuberEntity vtuber = new VtuberEntity();
        vtuber.setChannelId(channel.getId());
        vtuber.setName(channel.getSnippet().getTitle());

        String description = channel.getSnippet().getDescription();
        if (description != null && description.length() > 255) {
            description = description.substring(0, 255);
        }
        vtuber.setDescription(description);
        vtuber.setSubscribers(channel.getStatistics().getSubscriberCount());
        vtuber.setAddedTime(LocalDateTime.now());

        if (channel.getSnippet().getThumbnails() != null && channel.getSnippet().getThumbnails().getDefault() != null) {
            vtuber.setChannelImg(channel.getSnippet().getThumbnails().getDefault().getUrl());
        }
        vtuber.setStatus("new");

        vtuberRepository.save(vtuber);
        logger.info("새로운 버튜버 저장 완료: {}", vtuber.getName());
    }

    // 개선된 isKoreanVtuber 메서드 (추천 조합)
    private boolean isKoreanVtuber(Channel channel) {
        String channelId = channel.getId();
        String title = channel.getSnippet().getTitle();
        String description = channel.getSnippet().getDescription();

        boolean containsKoreanInTitle = title.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsKoreanInDescription = description != null && description.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");

        // 한국어 포함 여부 체크
        if (!containsKoreanInTitle && !containsKoreanInDescription) {
            logger.info("한국어 없음으로 필터링: {}", title);
            return false;
        }

        // 제외 키워드 체크 먼저 (우선순위 키워드 제외)
        boolean containsPriorityKeyword = priorityKeywords.stream().anyMatch(keyword ->
                title.contains(keyword) || (description != null && description.contains(keyword))
        );

        boolean containsExcludeKeywordsInTitle = excludeKeywordsInTitle.stream().anyMatch(title::contains);
        boolean containsExcludeKeywordsInDescription = description != null &&
                excludeKeywordsInDescription.stream().anyMatch(description::contains);

        if ((containsExcludeKeywordsInTitle || containsExcludeKeywordsInDescription) && !containsPriorityKeyword) {
            if (containsExcludeKeywordsInTitle) {
                String matchedKeyword = excludeKeywordsInTitle.stream()
                        .filter(title::contains)
                        .findFirst()
                        .orElse("알 수 없음");
                logger.info("제외 키워드(제목)로 인한 필터링: {} - 키워드: '{}'", title, matchedKeyword);
            }
            if (containsExcludeKeywordsInDescription) {
                String matchedKeyword = excludeKeywordsInDescription.stream()
                        .filter(desc -> description.contains(desc))
                        .findFirst()
                        .orElse("알 수 없음");
                logger.info("제외 키워드(설명)로 인한 필터링: {} - 키워드: '{}'", title, matchedKeyword);
            }
            return false;
        }

        // 다양한 감지 방법들
        boolean hasVtuberKeyword = hasDirectVtuberKeyword(title, description);
        boolean hasVisualCharacteristics = hasVtuberVisualCharacteristics(title, description);
        boolean belongsToCompany = belongsToVtuberCompany(description);
        boolean hasContentPattern = hasVtuberContentPattern(channelId);

        boolean result = hasVtuberKeyword || hasVisualCharacteristics || belongsToCompany || hasContentPattern;

        // 상세 로깅
        if (result) {
            StringBuilder reasons = new StringBuilder("버튜버 감지 성공: ");
            if (hasVtuberKeyword) reasons.append("[직접키워드] ");
            if (hasVisualCharacteristics) reasons.append("[시각특징] ");
            if (belongsToCompany) reasons.append("[소속사] ");
            if (hasContentPattern) reasons.append("[콘텐츠패턴] ");
            if (containsPriorityKeyword) reasons.append("[우선순위] ");

            logger.info("{} - {}", reasons.toString().trim(), title);
        } else {
            logger.info("버튜버 감지 실패: [키워드X] [시각특징X] [소속사X] [콘텐츠패턴X] - {}", title);
        }

        return result;
    }

    private boolean hasDirectVtuberKeyword(String title, String description) {
        boolean containsVtuberInTitle = title.contains("버튜버") || title.contains("Vtuber") || title.contains("VTuber");
        boolean containsVtuberInDescription = description != null &&
                (description.contains("버튜버") || description.contains("Vtuber") || description.contains("VTuber"));

        return containsVtuberInTitle || containsVtuberInDescription;
    }

    private boolean hasVtuberVisualCharacteristics(String title, String description) {
        // 아바타/캐릭터 관련 키워드 체크
        boolean hasAvatarKeyword = avatarKeywords.stream().anyMatch(keyword ->
                title.toLowerCase().contains(keyword.toLowerCase()) ||
                        (description != null && description.toLowerCase().contains(keyword.toLowerCase()))
        );

        // 버튜버 특유의 활동 키워드 체크
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

            // 버튜버 특유의 방송 제목 패턴
            List<String> vtuberPatterns = Arrays.asList(
                    "잡담", "방송", "게임", "노래", "singing", "stream",
                    "첫방", "데뷔방", "콜라보", "collaboration", "잡담방",
                    "방송시작", "live", "라이브", "스트림"
            );

            long patternMatches = videoTitles.stream()
                    .mapToLong(videoTitle -> vtuberPatterns.stream()
                            .mapToLong(pattern -> videoTitle.toLowerCase().contains(pattern.toLowerCase()) ? 1 : 0)
                            .sum())
                    .sum();

            logger.debug("채널 {} 콘텐츠 패턴 분석: {}개 매칭 (총 {}개 영상)",
                    channelId, patternMatches, videoTitles.size());

            return patternMatches >= 3; // 10개 영상 중 3개 이상 매칭

        } catch (Exception e) {
            logger.warn("채널 {} 콘텐츠 패턴 분석 실패: {}", channelId, e.getMessage());
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

    private final List<String> youtubeOfficialChannels = Arrays.asList(
            "UCkYQyvc_i9hXEo4xic9Hh2g", // Shopping
            "UC-9-kyTW8ZkZNDHQJ6FgpwQ", // Music
            "UC4R8DWoMoI7CAwX8_LjQHig", // Live
            "UCEgdi0XIXXZ-qJOFPf4JSKw", // Sports
            "UCOpNcN46UbXVtpKMrmU4Abg", // Gaming
            "UCYfdidRxbB8Qhf0Nx7ioOYw"  // News
    );

    private boolean isYouTubeOfficialChannel(String channelId, String title) {
        // 공식 채널 ID 리스트 체크
        if (youtubeOfficialChannels.contains(channelId)) {
            return true;
        }

        // YouTube 공식 채널 패턴 체크
        List<String> officialPatterns = Arrays.asList(
                "Music", "Sports", "Gaming", "News", "Live", "Shopping",
                "YouTube", "Google", "Trending"
        );

        return officialPatterns.stream().anyMatch(pattern ->
                title.toLowerCase().equals(pattern.toLowerCase())
        );
    }
}