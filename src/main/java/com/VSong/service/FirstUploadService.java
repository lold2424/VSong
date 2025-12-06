package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final VtuberValidationService validationService;
    private final YouTubeApiService youTubeApiService;

    private static final Logger logger = LoggerFactory.getLogger(FirstUploadService.class);

    @Value("${first-upload.enabled:false}")
    private boolean firstUploadEnabled;
    @Value("${first-upload.daily-limit:8000}")
    private int DAILY_QUOTA_LIMIT;
    @Value("${first-upload.batch-size:50}")
    private int VTUBERS_PER_DAY;

    private final AtomicInteger dailyApiUsage = new AtomicInteger(0);

    public FirstUploadService(
            YouTube youTube,
            VtuberRepository vtuberRepository,
            VtuberSongsRepository vtuberSongsRepository,
            VtuberValidationService validationService,
            YouTubeApiService youTubeApiService) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.validationService = validationService;
        this.youTubeApiService = youTubeApiService;
    }

    public void dailyFirstUpload() {
        if (!firstUploadEnabled) {
            if (logger.isDebugEnabled()) {
                logger.debug("첫 업로드 기능이 비활성화되어 있습니다.");
            }
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
        if (logger.isInfoEnabled()) {
            logger.info("오늘 처리할 VTuber 수: {}", todayVtubers.size());
        }

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
                    if (logger.isInfoEnabled()) {
                        logger.info("VTuber 처리 완료: {} ({}/{})", vtuber.getName(), processedCount, todayVtubers.size());
                    }
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("VTuber 처리 중 오류 발생: {} - {}", vtuber.getName(), e.getMessage());
                }
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("=== 첫 업로드 작업 완료 - 처리된 VTuber 수: {}, 사용된 API 할당량: {} ===", processedCount, dailyApiUsage.get());
        }
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
                if (logger.isErrorEnabled()) {
                    logger.error("업로드 플레이리스트를 찾을 수 없습니다: {}", vtuber.getChannelId());
                }
                return false;
            }
            return fetchSongsFromPlaylist(vtuber, uploadsPlaylistId);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("VTuber 노래 처리 중 오류: {} - {}", vtuber.getName(), e.getMessage());
            }
            return false;
        }
    }

    private String getUploadsPlaylistId(String channelId) throws IOException {
        dailyApiUsage.incrementAndGet();
        YouTube.Channels.List channelRequest = youTube.channels().list(List.of("contentDetails"));
        channelRequest.setId(List.of(channelId));
        ChannelListResponse channelResult = youTubeApiService.executeRequest(channelRequest);
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

                dailyApiUsage.incrementAndGet();
                YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems().list(List.of("contentDetails", "snippet"));
                playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
                playlistItemsRequest.setMaxResults(50L);
                playlistItemsRequest.setPageToken(pageToken);

                PlaylistItemListResponse playlistItemResult = youTubeApiService.executeRequest(playlistItemsRequest);
                List<PlaylistItem> playlistItems = playlistItemResult.getItems();
                if (playlistItems == null || playlistItems.isEmpty()) break;

                List<String> videoIds = playlistItems.stream().map(item -> item.getContentDetails().getVideoId()).collect(Collectors.toList());
                totalSongs += fetchAndProcessVideos(videoIds, vtuber);

                pageToken = playlistItemResult.getNextPageToken();
            } while (pageToken != null);
            return totalSongs > 0;
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error fetching songs from playlist for {}: {}", vtuber.getName(), e.getMessage());
            }
            return false;
        }
    }

    private int fetchAndProcessVideos(List<String> videoIds, VtuberEntity vtuber) throws IOException {
        int songsFound = 0;
        try {
            dailyApiUsage.incrementAndGet();
            YouTube.Videos.List videosRequest = youTube.videos().list(List.of("id", "snippet", "contentDetails", "statistics"));
            videosRequest.setId(videoIds);
            VideoListResponse videoResponse = youTubeApiService.executeRequest(videosRequest);
            for (Video video : videoResponse.getItems()) {
                if (processVideo(video, vtuber)) {
                    songsFound++;
                }
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("비디오 정보 조회 실패: {} - {}", vtuber.getName(), e.getMessage());
            }
            throw e;
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

    private void resetDailyUsage() {
        dailyApiUsage.set(0);
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
