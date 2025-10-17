package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.VtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class UploadVtuberService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberValidationService validationService;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;
    private final List<String> queries = Arrays.asList(
            "버튜버", "Vtuber", "버츄얼 유튜버", "버츄버",
            "이세계아이돌", "V-LUP", "RE:REVOLUTION", "VRECORD", "V&U", "일루전 라이브",
            "버츄얼 헤르츠", "V-llage", "레븐", "스타게이저", "싸이코드", "PLAVE", "러브다이아",
            "미츄", "스텔라이브", "뻐스시간", "스타데이즈", "블루점프", "팔레트", "AkaiV Studio",
            "리스텔라", "에스더", "포더모어", "스코시즘", "큐버스", "라이브루리", "플레이 바이 스쿨",
            "VLYZ.", "Artisons.", "HONEYZ", "PROJECT Serenity", "위싱 라이브", "브이아이",
            "몽상컴퍼니", "스테이터스", "아쿠아벨", "Plan.B Music", "멜로데이즈", "Re:AcT KR",
            "GRIM PRODUCTION.", "PJX.", "D-ESTER", "방과후버튜버", "베리타", "스타드림컴퍼니",
            "HANAVI", "UR:L", "Priz", "브이퍼리", "크로아", "하데스", "ACAXIA."
    );
    private static final Logger logger = LoggerFactory.getLogger(UploadVtuberService.class);
    private static final int MAX_PAGES_PER_QUERY = 3;
    private static final int MAX_QUOTA_PER_KEY = 10000;

    private final Cache<String, Boolean> processedCache = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    private final List<AtomicInteger> apiKeyUsage;
    private final RateLimiter rateLimiter = RateLimiter.create(5.0);
    private final Counter searchApiCounter;
    private final Counter channelsApiCounter;

    public UploadVtuberService(YouTube youTube,
                               VtuberRepository vtuberRepository,
                               VtuberValidationService validationService,
                               @Value("${youtube.api.keys}") String apiKeys,
                               MeterRegistry meterRegistry) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.validationService = validationService;
        this.apiKeys = Arrays.stream(apiKeys.split(",")).map(String::trim).collect(Collectors.toList());
        this.apiKeyUsage = this.apiKeys.stream().map(key -> new AtomicInteger(0)).collect(Collectors.toList());
        logger.info("API 키 {}개 로드 완료", this.apiKeys.size());
        this.searchApiCounter = meterRegistry.counter("youtube.api.search");
        this.channelsApiCounter = meterRegistry.counter("youtube.api.channels");
    }

    public void fetchAndSaveVtuberChannels() {
        logger.info("=== fetchAndSaveVtuberChannels 시작 ===");
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        for (String query : queries) {
            logger.info("쿼리 실행: {}", query);
            String pageToken = null;
            int pagesFetched = 0;
            do {
                if (pagesFetched >= MAX_PAGES_PER_QUERY) {
                    logger.info("쿼리 '{}'에 대해 최대 페이지 수를 초과했습니다.", query);
                    break;
                }
                pagesFetched++;
                try {
                    rateLimiter.acquire();
                    YouTube.Search.List search = youTube.search().list(List.of("id"));
                    search.setQ(query);
                    search.setType(List.of("channel"));
                    search.setFields("nextPageToken,items(id/channelId)");
                    search.setMaxResults(50L);
                    search.setKey(getCurrentApiKey());
                    search.setPageToken(pageToken);
                    incrementApiKeyUsage();
                    searchApiCounter.increment();
                    SearchListResponse searchResponse = search.execute();
                    List<String> channelIds = searchResponse.getItems().stream()
                            .map(result -> result.getId().getChannelId())
                            .filter(channelId -> processedCache.getIfPresent(channelId) == null)
                            .collect(Collectors.toList());
                    if (!channelIds.isEmpty()) {
                        List<List<String>> partitions = partitionList(channelIds, 50);
                        for (List<String> partition : partitions) {
                            executor.submit(() -> {
                                processChannels(partition);
                                partition.forEach(id -> processedCache.put(id, true));
                            });
                        }
                    }
                    pageToken = searchResponse.getNextPageToken();
                } catch (IOException e) {
                    logger.error("API 호출 중 오류 발생: {}", e.getMessage());
                    rotateApiKey();
                }
            } while (pageToken != null);
        }
        updateExistingChannelsMissingImages(executor);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.HOURS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("=== fetchAndSaveVtuberChannels 종료 ===");
    }

    public List<String> fetchAllChannelIdsFromApi() {
        logger.info("=== fetchAllChannelIdsFromApi 시작 ===");
        List<String> allChannelIds = new java.util.ArrayList<>();
        for (String query : queries) {
            logger.info("ID 수집 쿼리 실행: {}", query);
            String pageToken = null;
            int pagesFetched = 0;
            do {
                if (pagesFetched >= MAX_PAGES_PER_QUERY) {
                    logger.info("쿼리 '{}'에 대해 최대 페이지 수를 초과했습니다.", query);
                    break;
                }
                pagesFetched++;
                try {
                    rateLimiter.acquire();
                    YouTube.Search.List search = youTube.search().list(List.of("id"));
                    search.setQ(query);
                    search.setType(List.of("channel"));
                    search.setFields("nextPageToken,items(id/channelId)");
                    search.setMaxResults(50L);
                    search.setKey(getCurrentApiKey());
                    search.setPageToken(pageToken);
                    incrementApiKeyUsage();
                    searchApiCounter.increment();
                    SearchListResponse searchResponse = search.execute();
                    allChannelIds.addAll(searchResponse.getItems().stream()
                            .map(result -> result.getId().getChannelId())
                            .collect(Collectors.toList()));
                    pageToken = searchResponse.getNextPageToken();
                } catch (IOException e) {
                    logger.error("API 호출 중 오류 발생: {}", e.getMessage());
                    rotateApiKey();
                }
            } while (pageToken != null);
        }
        logger.info("=== fetchAllChannelIdsFromApi 종료, 총 {}개 ID 수집 ===", allChannelIds.size());
        return allChannelIds.stream().distinct().collect(Collectors.toList());
    }

    private void processChannels(List<String> channelIds) {
        logger.debug("processChannels 시작 - 채널 ID 개수: {}", channelIds.size());
        try {
            rateLimiter.acquire();
            YouTube.Channels.List channelRequest = youTube.channels().list(List.of("snippet", "statistics"));
            channelRequest.setId(channelIds);
            channelRequest.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");
            channelRequest.setKey(getCurrentApiKey());
            incrementApiKeyUsage();
            channelsApiCounter.increment();
            ChannelListResponse channelResponse = channelRequest.execute();
            if (channelResponse.getItems() == null) return;

            for (Channel channel : channelResponse.getItems()) {
                String channelId = channel.getId();
                String channelTitle = channel.getSnippet().getTitle();

                String processableReason = validationService.getChannelProcessableReason(channelId);
                if (processableReason != null) {
                    logger.info("걸러진 채널: {} (ID: {}) - 이유: {}", channelTitle, channelId, processableReason);
                    continue;
                }

                String koreanVtuberReason = validationService.getKoreanVtuberReason(channel);
                if (koreanVtuberReason != null) {
                    logger.info("걸러진 채널: {} (ID: {}) - 이유: {}", channelTitle, channelId, koreanVtuberReason);
                    continue;
                }

                VtuberEntity vtuber = new VtuberEntity();
                vtuber.setChannelId(channelId);
                vtuber.setName(channelTitle);
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
                logger.info("새로운 VTuber 저장: {} (ID: {})", vtuber.getName(), channelId);
            }
        } catch (IOException e) {
            logger.error("채널 처리 중 API 오류 발생: {}", e.getMessage());
            rotateApiKey();
        }
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private synchronized void rotateApiKey() {
        if (apiKeys.size() == 1) throw new RuntimeException("모든 API 키의 할당량이 소진되었습니다.");
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.warn("API 키를 다음 키로 전환했습니다.");
    }

    private void incrementApiKeyUsage() {
        if (apiKeyUsage.get(currentKeyIndex).incrementAndGet() >= MAX_QUOTA_PER_KEY) {
            rotateApiKey();
        }
    }

    private void updateExistingChannelsMissingImages(ThreadPoolExecutor executor) {
        List<VtuberEntity> vtubersWithMissingImages = vtuberRepository.findByChannelImgIsNull();
        if (vtubersWithMissingImages.isEmpty()) return;
        logger.info("프로필 이미지가 없는 VTuber 수: {}", vtubersWithMissingImages.size());
        for (VtuberEntity vtuber : vtubersWithMissingImages) {
            executor.submit(() -> {
                String imageUrl = fetchChannelProfileImage(vtuber.getChannelId());
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    vtuber.setChannelImg(imageUrl);
                    vtuberRepository.save(vtuber);
                    logger.info("프로필 이미지 업데이트 완료: {}", vtuber.getName());
                }
            });
        }
    }

    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private String fetchChannelProfileImage(String channelId) {
        try {
            rateLimiter.acquire();
            YouTube.Channels.List channelsList = youTube.channels().list(List.of("snippet"));
            channelsList.setId(List.of(channelId));
            channelsList.setKey(getCurrentApiKey());
            channelsList.setFields("items(snippet/thumbnails/default/url)");
            incrementApiKeyUsage();
            channelsApiCounter.increment();
            ChannelListResponse response = channelsList.execute();
            if (!response.getItems().isEmpty() && response.getItems().get(0).getSnippet().getThumbnails() != null) {
                return response.getItems().get(0).getSnippet().getThumbnails().getDefault().getUrl();
            }
        } catch (Exception e) {
            logger.warn("프로필 이미지 가져오기 실패 - 채널 ID: {} - 오류: {}", channelId, e.getMessage());
            rotateApiKey();
        }
        return null;
    }
}