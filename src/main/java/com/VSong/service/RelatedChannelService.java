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
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RelatedChannelService {

    private static final Logger logger = LoggerFactory.getLogger(RelatedChannelService.class);

    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberValidationService validationService; // 주입
    private final YouTube youTube;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;

    public RelatedChannelService(VtuberRepository vtuberRepository,
                                 ExceptVtuberRepository exceptVtuberRepository,
                                 VtuberValidationService validationService, // 주입
                                 YouTube youTube,
                                 @Value("${youtube.api.keys}") String apiKeys) {
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.validationService = validationService; // 주입
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
                    Thread.sleep(5000);

                    List<String> selectors = Arrays.asList(
                            "ytd-channel-renderer a.yt-simple-endpoint",
                            "ytd-channel-renderer a[href*='/channel/']",
                            "a[href*='/channel/']"
                    );

                    boolean foundAny = false;
                    for (String selector : selectors) {
                        List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                        if (!elements.isEmpty()) {
                            foundAny = true;
                            for (WebElement element : elements) {
                                String href = element.getAttribute("href");
                                if (href != null && href.contains("/channel/")) {
                                    String discoveredId = extractChannelId(href);
                                    if (discoveredId != null && !allKnownIds.contains(discoveredId)) {
                                        if (!isYouTubeOfficialChannel(discoveredId, element.getText())) {
                                            discoveredIds.add(discoveredId);
                                            logger.info("새로운 관련 채널 발견: {}", discoveredId);
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                    if (!foundAny) {
                        logger.warn("채널 {} - 어떤 관련 채널도 찾을 수 없음", channelId);
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

    private void processDiscoveredChannels(Set<String> channelIds) {
        logger.info("새로 발견된 채널 정보 처리 시작. 대상: {}개", channelIds.size());
        try {
            YouTube.Channels.List channelRequest = youTube.channels().list(List.of("snippet", "statistics"));
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
            for (Channel channel : channels) {
                String channelId = channel.getId();
                String channelTitle = channel.getSnippet().getTitle();

                // Validation using VtuberValidationService
                String notProcessableReason = validationService.getChannelProcessableReason(channelId);
                if (notProcessableReason != null) {
                    logger.info("처리 불가 채널: {} ({}) - 이유: {}", channelTitle, channelId, notProcessableReason);
                    continue;
                }

                String notVtuberReason = validationService.getKoreanVtuberReason(channel);
                if (notVtuberReason != null) {
                    logger.info("버튜버 아님으로 필터링: {} ({}) - 이유: {}", channelTitle, channelId, notVtuberReason);
                    continue;
                }

                logger.info("저장 조건 만족 - 새 버튜버 저장: {}", channelTitle);
                saveNewVtuber(channel);
                savedCount++;
            }
            logger.info("채널 처리 완료 - 저장: {}개, 총 처리: {}개", savedCount, channels.size());

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

    private String extractChannelId(String href) {
        try {
            int startIndex = href.indexOf("/channel/") + 9;
            return href.substring(startIndex, startIndex + 24);
        } catch (Exception e) {
            logger.warn("채널 ID 추출 중 오류: {}", href, e);
            return null;
        }
    }

    private WebDriver createOptimizedChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*", "--mute-audio");
        // ... (other options can be kept for performance)
        return new ChromeDriver(options);
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private synchronized void rotateApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.warn("API 키를 다음 키로 전환했습니다.");
    }

    private final List<String> youtubeOfficialChannels = Arrays.asList(
            "UCkYQyvc_i9hXEo4xic9Hh2g", "UC-9-kyTW8ZkZNDHQJ6FgpwQ", "UC4R8DWoMoI7CAwX8_LjQHig",
            "UCEgdi0XIXXZ-qJOFPf4JSKw", "UCOpNcN46UbXVtpKMrmU4Abg", "UCYfdidRxbB8Qhf0Nx7ioOYw"
    );

    private boolean isYouTubeOfficialChannel(String channelId, String title) {
        if (youtubeOfficialChannels.contains(channelId)) return true;
        List<String> officialPatterns = Arrays.asList("Music", "Sports", "Gaming", "News", "Live", "Shopping", "YouTube", "Google", "Trending");
        return officialPatterns.stream().anyMatch(pattern -> title.equalsIgnoreCase(pattern));
    }
}
