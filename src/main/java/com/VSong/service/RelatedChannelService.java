package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.ExceptVtuberRepository;
import com.VSong.repository.VtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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


    public RelatedChannelService(VtuberRepository vtuberRepository,
                                 ExceptVtuberRepository exceptVtuberRepository,
                                 YouTube youTube,
                                 @Value("${youtube.api.keys}") String apiKeys) {
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.youTube = youTube;
        this.apiKeys = Arrays.asList(apiKeys.split(","));
    }

    @Scheduled(cron = "0 06 11 * * ?")
    public void discoverAndSaveFromRelatedChannels() {
        logger.info("=== 관련 채널 기반 버튜버 탐색 시작 ===");

        // 1. 모든 기존 버튜버 및 제외 채널 ID 로드
        List<String> existingVtuberIds = vtuberRepository.findAllChannelIds();
        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        Set<String> allKnownIds = new HashSet<>(existingVtuberIds);
        allKnownIds.addAll(exceptChannelIds);

        // 2. Selenium WebDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 백그라운드에서 실행
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        WebDriver driver = new ChromeDriver(options);

        Set<String> discoveredChannelIds = new HashSet<>();

        // 3. 각 기존 버튜버의 관련 채널 탐색
        for (String channelId : existingVtuberIds) {
            String url = "https://www.youtube.com/channel/" + channelId + "/channels";
            logger.info("채널 탐색 중: " + url);
            try {
                driver.get(url);
                // 페이지가 동적으로 로드될 때까지 잠시 대기
                Thread.sleep(2000);

                // 관련 채널 링크 요소 찾기
                List<WebElement> channelLinks = driver.findElements(By.cssSelector("ytd-channel-renderer a.yt-simple-endpoint"));
                for (WebElement link : channelLinks) {
                    String href = link.getAttribute("href");
                    if (href != null && href.contains("/channel/")) {
                        String discoveredId = href.substring(href.lastIndexOf("/channel/") + 9);
                        if (!allKnownIds.contains(discoveredId)) {
                            discoveredChannelIds.add(discoveredId);
                            logger.info("새로운 관련 채널 발견: " + discoveredId);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("채널 페이지 스크래핑 중 오류 발생: " + url, e);
            }
        }
        driver.quit();

        logger.info("총 {}개의 새로운 관련 채널 ID를 발견했습니다.", discoveredChannelIds.size());

        // 4. 발견된 신규 채널 ID 처리
        if (!discoveredChannelIds.isEmpty()) {
            processDiscoveredChannels(discoveredChannelIds);
        }

        logger.info("=== 관련 채널 기반 버튜버 탐색 종료 ===");
    }

    private void processDiscoveredChannels(Set<String> channelIds) {
        logger.info("새로 발견된 채널 정보 처리 시작. 대상: {}개", channelIds.size());
        try {
            YouTube.Channels.List channelRequest = youTube.channels().list("snippet,statistics");
            channelRequest.setId(String.join(",", channelIds));
            channelRequest.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");
            channelRequest.setKey(getCurrentApiKey());

            ChannelListResponse channelResponse = channelRequest.execute();
            List<Channel> channels = channelResponse.getItems();

            if (channels == null || channels.isEmpty()) {
                logger.warn("API로부터 채널 정보를 가져오지 못했습니다.");
                return;
            }

            for (Channel channel : channels) {
                // isKoreanVtuber 및 구독자 수 검사
                if (isKoreanVtuber(channel) && channel.getStatistics().getSubscriberCount().compareTo(BigInteger.valueOf(MIN_SUBSCRIBERS)) >= 0) {
                    // 이미 DB에 있는지 한번 더 최종 확인
                    if (vtuberRepository.findByChannelId(channel.getId()).isEmpty()) {
                        saveNewVtuber(channel);
                    }
                } else {
                    logger.info("필터링 조건 미충족으로 저장되지 않은 채널: {}", channel.getSnippet().getTitle());
                }
            }
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

    private boolean isKoreanVtuber(Channel channel) {
        String title = channel.getSnippet().getTitle();
        String description = channel.getSnippet().getDescription();

        boolean containsKoreanInTitle = title.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsKoreanInDescription = description != null && description.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsVtuberInTitle = title.contains("버튜버") || title.contains("Vtuber");
        boolean containsVtuberInDescription = description != null && (description.contains("버튜버") || description.contains("Vtuber"));

        boolean containsPriorityKeyword = priorityKeywords.stream().anyMatch(keyword ->
                title.contains(keyword) || (description != null && description.contains(keyword))
        );

        boolean containsExcludeKeywordsInTitle = excludeKeywordsInTitle.stream().anyMatch(title::contains);
        boolean containsExcludeKeywordsInDescription = description != null && excludeKeywordsInDescription.stream().anyMatch(description::contains);

        if ((containsExcludeKeywordsInTitle || containsExcludeKeywordsInDescription) && !containsPriorityKeyword) {
            return false;
        }

        return (containsKoreanInTitle && containsVtuberInTitle) ||
               (containsKoreanInDescription && containsVtuberInDescription) ||
               containsPriorityKeyword;
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private synchronized void rotateApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.warn("API 키를 다음 키로 전환했습니다.");
    }
}
