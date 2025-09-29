package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class FirstUploadService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberValidationService validationService; // 검증 서비스 주입
    private final List<String> apiKeys;
    private final List<AtomicInteger> apiKeyUsage;
    private final List<Boolean> keyAvailable;
    private int currentKeyIndex = 0;

    private static final Logger logger = LoggerFactory.getLogger(FirstUploadService.class);

    @Value("${first-upload.enabled:false}")
    private boolean firstUploadEnabled;
    @Value("${first-upload.daily-limit:8000}")
    private int DAILY_QUOTA_LIMIT;
    @Value("${first-upload.batch-size:50}")
    private int VTUBERS_PER_DAY;

    private static final RateLimiter rateLimiter = RateLimiter.create(5.0);
    private final AtomicInteger dailyApiUsage = new AtomicInteger(0);

    public FirstUploadService(
            YouTube youTube,
            VtuberRepository vtuberRepository,
            VtuberSongsRepository vtuberSongsRepository,
            VtuberValidationService validationService, // 생성자에 추가
            @Value("${youtube.api.keys}") List<String> apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.validationService = validationService; // 초기화
        this.apiKeys = new ArrayList<>(apiKeys);
        this.apiKeyUsage = new ArrayList<>();
        this.keyAvailable = new ArrayList<>();
        for (int i = 0; i < apiKeys.size(); i++) {
            this.apiKeyUsage.add(new AtomicInteger(0));
            this.keyAvailable.add(true);
        }
    }

    public void dailyFirstUpload() {
        if (!firstUploadEnabled) {
            logger.debug("첫 업로드 기능이 비활성화되어 있습니다.");
            return;
        }
        logger.info("=== 첫 업로드 작업 시작 ===");
        resetDailyUsage();

        List<VtuberEntity> unprocessedVtubers = getUnprocessedVtubers();
        if (unprocessedVtubers.isEmpty()) {
            logger.info("처리할 VTuber가 없습니다. 첫 업로드 작업 완료.");
            return;
        }

        List<VtuberEntity> todayVtubers = unprocessedVtubers.stream().limit(VTUBERS_PER_DAY).collect(Collectors.toList());
        logger.info("오늘 처리할 VTuber 수: {}", todayVtubers.size());

        int processedCount = 0;
        for (VtuberEntity vtuber : todayVtubers) {
            if (dailyApiUsage.get() >= DAILY_QUOTA_LIMIT) {
                logger.warn("일일 API 할당량 한계에 도달했습니다. 작업을 중단합니다.");
                break;
            }
            try {
                boolean success = processVtuberSongs(vtuber);
                if (success) {
                    vtuber.setStatus("processed");
                    vtuberRepository.save(vtuber);
                    processedCount++;
                    logger.info("VTuber 처리 완료: {} ({}/{})", vtuber.getName(), processedCount, todayVtubers.size());
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error("VTuber 처리 중 오류 발생: {} - {}", vtuber.getName(), e.getMessage());
            }
        }
        logger.info("=== 첫 업로드 작업 완료 - 처리된 VTuber 수: {}, 사용된 API 할당량: {} ===", processedCount, dailyApiUsage.get());
    }

    private boolean processVideo(Video video, VtuberEntity vtuber) {
        // 통합된 검증 로직 사용
        if (!validationService.isSongRelated(video)) {
            return false;
        }
        if (validationService.isSongAlreadyExists(video.getId())) {
            return false;
        }

        String classification = validationService.classifyVideo(video);
        if ("ignore".equals(classification)) {
            return false;
        }

        saveNewSong(video, vtuber, classification);
        return true;
    }

    // --- Other methods remain largely the same ---

    private List<VtuberEntity> getUnprocessedVtubers() {
        return vtuberRepository.findAll().stream()
                .filter(vtuber -> {
                    String status = vtuber.getStatus();
                    if ("processed".equals(status)) return false;
                    if ("processing".equals(status) || "error".equals(status)) return true;
                    boolean hasNoSongs = vtuberSongsRepository.countByChannelId(vtuber.getChannelId()) == 0;
                    return hasNoSongs && ("new".equals(status) || "existing".equals(status));
                })
                .collect(Collectors.toList());
    }

    private boolean processVtuberSongs(VtuberEntity vtuber) {
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(vtuber.getChannelId());
            if (uploadsPlaylistId == null) {
                logger.error("업로드 플레이리스트를 찾을 수 없습니다: {}", vtuber.getChannelId());
                return false;
            }
            return fetchSongsFromPlaylist(vtuber, uploadsPlaylistId);
        } catch (Exception e) {
            logger.error("VTuber 노래 처리 중 오류: {} - {}", vtuber.getName(), e.getMessage());
            return false;
        }
    }

    private String getUploadsPlaylistId(String channelId) throws IOException {
        rateLimiter.acquire();
        incrementApiUsage();
        YouTube.Channels.List channelRequest = youTube.channels().list(List.of("contentDetails"));
        channelRequest.setId(List.of(channelId));
        channelRequest.setKey(getCurrentApiKey());
        ChannelListResponse channelResult = channelRequest.execute();
        List<Channel> channelsList = channelResult.getItems();
        if (channelsList != null && !channelsList.isEmpty()) {
            return channelsList.get(0).getContentDetails().getRelatedPlaylists().getUploads();
        }
        return null;
    }

    private boolean fetchSongsFromPlaylist(VtuberEntity vtuber, String uploadsPlaylistId) {
        String pageToken = null;
        int totalSongs = 0;
        try {
            do {
                if (dailyApiUsage.get() >= DAILY_QUOTA_LIMIT) break;
                rateLimiter.acquire();
                incrementApiUsage();

                YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems().list(List.of("contentDetails", "snippet"));
                playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
                playlistItemsRequest.setMaxResults(50L);
                playlistItemsRequest.setPageToken(pageToken);
                playlistItemsRequest.setKey(getCurrentApiKey());

                PlaylistItemListResponse playlistItemResult = playlistItemsRequest.execute();
                List<PlaylistItem> playlistItems = playlistItemResult.getItems();
                if (playlistItems == null || playlistItems.isEmpty()) break;

                List<String> videoIds = playlistItems.stream().map(item -> item.getContentDetails().getVideoId()).collect(Collectors.toList());
                totalSongs += fetchAndProcessVideos(videoIds, vtuber);

                pageToken = playlistItemResult.getNextPageToken();
            } while (pageToken != null);
            return totalSongs > 0;
        } catch (Exception e) {
            logger.error("Error fetching songs from playlist for {}: {}", vtuber.getName(), e.getMessage());
            return false;
        }
    }

    private int fetchAndProcessVideos(List<String> videoIds, VtuberEntity vtuber) {
        int songsFound = 0;
        try {
            rateLimiter.acquire();
            incrementApiUsage();
            YouTube.Videos.List videosRequest = youTube.videos().list(List.of("id", "snippet", "contentDetails", "statistics"));
            videosRequest.setId(videoIds);
            videosRequest.setKey(getCurrentApiKey());
            VideoListResponse videoResponse = videosRequest.execute();
            for (Video video : videoResponse.getItems()) {
                if (processVideo(video, vtuber)) {
                    songsFound++;
                }
            }
        } catch (IOException e) {
            logger.error("비디오 정보 조회 실패: {} - {}", vtuber.getName(), e.getMessage());
            switchApiKey();
        }
        return songsFound;
    }

    private void saveNewSong(Video video, VtuberEntity vtuber, String classification) {
        VtuberSongsEntity song = new VtuberSongsEntity();
        song.setChannelId(video.getSnippet().getChannelId());
        song.setVideoId(video.getId());
        song.setTitle(video.getSnippet().getTitle());
        song.setPublishedAt(Instant.ofEpochMilli(video.getSnippet().getPublishedAt().getValue()).atZone(ZoneId.systemDefault()).toLocalDateTime());
        song.setAddedTime(LocalDateTime.now());
        song.setUpdateDayTime(LocalDateTime.now());
        song.setUpdateWeekTime(LocalDateTime.now());
        song.setVtuberName(vtuber.getName());
        song.setViewCount(video.getStatistics().getViewCount().longValue());
        song.setViewsIncreaseDay(0L);
        song.setViewsIncreaseWeek(0L);
        song.setLastWeekViewCount(0L);
        song.setStatus("existing");
        song.setClassification(classification);
        vtuberSongsRepository.save(song);
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private void incrementApiUsage() {
        apiKeyUsage.get(currentKeyIndex).incrementAndGet();
        dailyApiUsage.incrementAndGet();
        if (apiKeyUsage.get(currentKeyIndex).get() >= 9000) {
            switchApiKey();
        }
    }

    private void switchApiKey() {
        keyAvailable.set(currentKeyIndex, false);
        int initialKeyIndex = currentKeyIndex;
        do {
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
            if (keyAvailable.get(currentKeyIndex)) {
                logger.info("API 키 전환: {}", getCurrentApiKey());
                return;
            }
        } while (currentKeyIndex != initialKeyIndex);
        throw new RuntimeException("모든 API 키의 할당량이 소진되었습니다.");
    }

    private void resetDailyUsage() {
        dailyApiUsage.set(0);
        for (int i = 0; i < apiKeyUsage.size(); i++) {
            apiKeyUsage.get(i).set(0);
            keyAvailable.set(i, true);
        }
        currentKeyIndex = 0;
        logger.info("일일 API 사용량 리셋 완료");
    }

    public Map<String, Object> getProgress() {
        List<VtuberEntity> allVtubers = vtuberRepository.findAll();
        List<VtuberEntity> unprocessedVtubers = getUnprocessedVtubers();
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalVtubers", allVtubers.size());
        progress.put("processedVtubers", allVtubers.size() - unprocessedVtubers.size());
        progress.put("remainingVtubers", unprocessedVtubers.size());
        progress.put("dailyApiUsage", dailyApiUsage.get());
        progress.put("dailyQuotaLimit", DAILY_QUOTA_LIMIT);
        progress.put("vtubersPerDay", VTUBERS_PER_DAY);
        return progress;
    }
}
