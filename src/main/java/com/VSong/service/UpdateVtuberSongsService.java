package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.entity.SongViewHistory;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.VSong.repository.SongViewHistoryRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.VSong.repository.SongUpdateLogRepository;
import com.VSong.entity.SongUpdateLog;

@Service
public class UpdateVtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberValidationService validationService;
    private final YouTubeApiService youTubeApiService;
    private final MainPageService mainPageService;
    private final CacheManager cacheManager;
    private final SongUpdateLogRepository songUpdateLogRepository;
    private final SongViewHistoryRepository songViewHistoryRepository;
    private final com.VSong.repository.ApiQuotaLogRepository apiQuotaLogRepository;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(UpdateVtuberSongsService.class);
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    public UpdateVtuberSongsService(
            YouTube youTube,
            VtuberRepository vtuberRepository,
            VtuberSongsRepository vtuberSongsRepository,
            VtuberValidationService validationService,
            YouTubeApiService youTubeApiService,
            MainPageService mainPageService,
            CacheManager cacheManager,
            SongUpdateLogRepository songUpdateLogRepository,
            SongViewHistoryRepository songViewHistoryRepository,
            com.VSong.repository.ApiQuotaLogRepository apiQuotaLogRepository,
            ObjectMapper objectMapper) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.validationService = validationService;
        this.youTubeApiService = youTubeApiService;
        this.mainPageService = mainPageService;
        this.cacheManager = cacheManager; 
        this.songUpdateLogRepository = songUpdateLogRepository;
        this.songViewHistoryRepository = songViewHistoryRepository;
        this.apiQuotaLogRepository = apiQuotaLogRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getLastOperationStats() {
        Map<String, Object> stats = new HashMap<>();
        SongUpdateLog latestLog = songUpdateLogRepository.findLatestLog();
        
        if (latestLog != null) {
            stats.put("lastRunTime", latestLog.getRunTime());
            stats.put("durationSeconds", latestLog.getDurationSeconds());
            stats.put("newSongsCount", latestLog.getNewSongsCount());
            stats.put("excludedSongsCount", latestLog.getExcludedSongsCount());
            stats.put("failedSongsCount", latestLog.getFailedSongsCount());
            stats.put("usedQuota", latestLog.getUsedQuota());
            try {
                if (latestLog.getErrorSummaryJson() != null) {
                    stats.put("errorSummary", objectMapper.readValue(latestLog.getErrorSummaryJson(), Map.class));
                }
                if (latestLog.getSlowestChannelsJson() != null) {
                    stats.put("slowestChannels", objectMapper.readValue(latestLog.getSlowestChannelsJson(), List.class));
                }
                if (latestLog.getNewSongsJson() != null) {
                    stats.put("newSongs", objectMapper.readValue(latestLog.getNewSongsJson(), List.class));
                }
                if (latestLog.getExcludedSongsJson() != null) {
                    stats.put("excludedSongs", objectMapper.readValue(latestLog.getExcludedSongsJson(), List.class));
                }
                if (latestLog.getFailedSongsJson() != null) {
                    stats.put("failedSongs", objectMapper.readValue(latestLog.getFailedSongsJson(), List.class));
                }
            } catch (Exception e) {
                logger.error("Error reading saved stats from DB", e);
            }
        }
        return stats;
    }

    public List<SongUpdateLog> getRecentLogs() {
        return songUpdateLogRepository.findRecentLogs();
    }

    public void fetchVtuberSongs() {
        logger.info("=== fetchVtuberSongs 실행 시작 ===");
        LocalDateTime startTime = LocalDateTime.now(SEOUL_ZONE);
        List<String> allNewSongTitles = new ArrayList<>();
        List<String> allExcludedSongInfo = new ArrayList<>();
        List<String> allFailedSongInfo = new ArrayList<>();
        Map<String, Integer> errorSummary = new HashMap<>();
        List<Map<String, Object>> performanceLogs = new ArrayList<>();

        List<VtuberEntity> newVtubers = vtuberRepository.findByStatus("new");
        List<VtuberEntity> existingVtubers = vtuberRepository.findByStatus("existing");

        for (VtuberEntity vtuber : newVtubers) {
            long channelStartTime = System.currentTimeMillis();
            fetchAllSongsFromPlaylist(vtuber.getChannelId(), vtuber.getName(), allNewSongTitles, allExcludedSongInfo, allFailedSongInfo, errorSummary);
            long duration = System.currentTimeMillis() - channelStartTime;
            
            Map<String, Object> log = new HashMap<>();
            log.put("name", vtuber.getName());
            log.put("durationMs", duration);
            log.put("type", "NEW");
            performanceLogs.add(log);

            vtuber.setStatus("existing");
            vtuberRepository.save(vtuber);
        }

        for (VtuberEntity vtuber : existingVtubers) {
            long channelStartTime = System.currentTimeMillis();
            fetchRecentSongsFromSearch(vtuber.getChannelId(), vtuber.getName(), allNewSongTitles, allExcludedSongInfo, allFailedSongInfo, errorSummary);
            long duration = System.currentTimeMillis() - channelStartTime;

            Map<String, Object> log = new HashMap<>();
            log.put("name", vtuber.getName());
            log.put("durationMs", duration);
            log.put("type", "EXISTING");
            performanceLogs.add(log);
        }

        mainPageService.refreshMainPageCache();

        performanceLogs.sort((a, b) -> Long.compare((long) b.get("durationMs"), (long) a.get("durationMs")));
        int slowestCount = (int) Math.ceil(performanceLogs.size() * 0.2);
        List<Map<String, Object>> slowestChannels = performanceLogs.stream()
                .limit(slowestCount)
                .collect(Collectors.toList());

        logger.info("=== fetchVtuberSongs 실행 종료 ===");

        LocalDateTime endTime = LocalDateTime.now(SEOUL_ZONE);
        long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        Integer usedQuota = apiQuotaLogRepository.sumCostByRequestTimeBetween(startTime, endTime);
        if (usedQuota == null) usedQuota = 0;

        try {
            SongUpdateLog log = new SongUpdateLog();
            log.setRunTime(startTime);
            log.setDurationSeconds(durationSeconds);
            log.setNewSongsCount(allNewSongTitles.size());
            log.setExcludedSongsCount(allExcludedSongInfo.size());
            log.setFailedSongsCount(allFailedSongInfo.size());
            log.setUsedQuota(usedQuota);
            log.setErrorSummaryJson(objectMapper.writeValueAsString(errorSummary));
            log.setSlowestChannelsJson(objectMapper.writeValueAsString(slowestChannels));
            log.setNewSongsJson(objectMapper.writeValueAsString(allNewSongTitles));
            log.setExcludedSongsJson(objectMapper.writeValueAsString(allExcludedSongInfo));
            log.setFailedSongsJson(objectMapper.writeValueAsString(allFailedSongInfo));
            songUpdateLogRepository.save(log);
            logger.info("Song update log saved to DB successfully.");
        } catch (Exception e) {
            logger.error("Failed to save song update log to DB: {}", e.getMessage());
        }
    }

    public void updateSongStatusToExisting() {
        List<VtuberSongsEntity> newSongs = vtuberSongsRepository.findByStatus("new");
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        int updatedCount = 0;

        for (VtuberSongsEntity song : newSongs) {
            long pubToAddedHours = java.time.Duration.between(song.getPublishedAt(), song.getAddedTime()).toHours();
            long addedToNowHours = java.time.Duration.between(song.getAddedTime(), now).toHours();

            boolean shouldBecomeExisting = false;
            if (pubToAddedHours <= 24) {
                if (addedToNowHours >= 24) {
                    shouldBecomeExisting = true;
                }
            } else {
                if (addedToNowHours >= 168) {
                    shouldBecomeExisting = true;
                }
            }

            if (shouldBecomeExisting) {
                song.setStatus("existing");
                song.setUpdateDayTime(now);
                vtuberSongsRepository.save(song);
                updatedCount++;
            }
        }
    }

    @SuppressWarnings("PMD.LooseCoupling")
    @Transactional
    public void updateViewCounts() {
        List<VtuberSongsEntity> songs = vtuberSongsRepository.findAll();
        if (logger.isInfoEnabled()) {
            logger.info("조회수 업데이트를 위해 {}개의 노래를 찾았습니다.", songs.size());
        }
        if (songs.isEmpty()) {
            return;
        }

        Map<String, VtuberSongsEntity> songMap = songs.stream()
                .collect(Collectors.toMap(VtuberSongsEntity::getVideoId, song -> song, (existing, replacement) -> existing));
        List<String> videoIds = new ArrayList<>(songMap.keySet());

        int updatedCount = 0;
        int deletedCount = 0;
        int failedCount = 0;

        LocalDateTime nowSeoul = LocalDateTime.now(SEOUL_ZONE);
        LocalDate today = nowSeoul.toLocalDate();
        LocalDate sevenDaysAgo = today.minusDays(7);

        int batchSize = 50;
        for (int i = 0; i < videoIds.size(); i += batchSize) {
            List<String> batch = videoIds.subList(i, Math.min(i + batchSize, videoIds.size()));
            try {
                YouTube.Videos.List request = youTube.videos().list(List.of("id", "statistics"));
                request.setId(batch);
                VideoListResponse response = youTubeApiService.executeRequest(request);

                List<String> foundVideoIds = new ArrayList<>();
                if (response != null) {
                    for (Video video : response.getItems()) {
                        foundVideoIds.add(video.getId());
                        try {
                            VtuberSongsEntity song = songMap.get(video.getId());
                            if (song != null && video.getStatistics() != null && video.getStatistics().getViewCount() != null) {
                                long newViewCount = video.getStatistics().getViewCount().longValue();
                                long oldViewCount = (song.getViewCount() == null) ? newViewCount : song.getViewCount();
                                
                                song.setViewsIncreaseDay(newViewCount - oldViewCount);
                                song.setViewCount(newViewCount);
                                song.setUpdateDayTime(nowSeoul);

                                SongViewHistory todaySnapshot = new SongViewHistory();
                                todaySnapshot.setVideoId(song.getVideoId());
                                todaySnapshot.setRecordDate(today);
                                todaySnapshot.setViewCount(newViewCount);
                                songViewHistoryRepository.save(todaySnapshot);

                                songViewHistoryRepository.findByVideoIdAndRecordDate(song.getVideoId(), sevenDaysAgo)
                                    .ifPresentOrElse(
                                        history -> {
                                            song.setViewsIncreaseWeek(newViewCount - history.getViewCount());
                                            song.setLastWeekViewCount(history.getViewCount());
                                            song.setUpdateWeekTime(nowSeoul);
                                        },
                                        () -> {
                                            long baseCount = (song.getLastWeekViewCount() == null) ? (newViewCount - song.getViewsIncreaseDay()) : song.getLastWeekViewCount();
                                            song.setViewsIncreaseWeek(newViewCount - baseCount);
                                            song.setUpdateWeekTime(nowSeoul);
                                        }
                                    );

                                vtuberSongsRepository.save(song);
                                updatedCount++;
                            } else {
                                failedCount++;
                            }
                        } catch (Exception e) {
                            failedCount++;
                        }
                    }
                }

                List<String> batchCopy = new ArrayList<>(batch);
                batchCopy.removeAll(foundVideoIds);
                for (String deletedVideoId : batchCopy) {
                    VtuberSongsEntity songToDelete = songMap.get(deletedVideoId);
                    if (songToDelete != null) {
                        vtuberSongsRepository.delete(songToDelete);
                        deletedCount++;
                    }
                }
            } catch (IOException e) {
                failedCount += batch.size();
            }
        }

        try {
            songViewHistoryRepository.deleteByRecordDateBefore(today.minusDays(8));
        } catch (Exception e) {
            logger.error("스냅샷 데이터 정리 중 오류 발생", e);
        }

        if (logger.isInfoEnabled()) {
            logger.info("조회수 업데이트 완료 (Rolling 7-Day 적용). 업데이트: {}개, 삭제: {}개, 실패: {}개", updatedCount, deletedCount, failedCount);
        }
        mainPageService.refreshMainPageCache();
    }

    private void summarizeError(String message, Map<String, Integer> errorSummary) {
        String category = "OTHER_ERROR";
        if (message.contains("403 Forbidden") || message.contains("accessNotConfigured") || message.contains("SERVICE_DISABLED")) {
            category = "API_FORBIDDEN";
        } else if (message.contains("quotaExceeded")) {
            category = "API_QUOTA_EXCEEDED";
        } else if (message.contains("업로드 재생목록 ID 조회 실패")) {
            category = "CHANNEL_NOT_FOUND";
        } else if (message.contains("IOException") || message.contains("시스템 오류")) {
            category = "NETWORK_OR_SYSTEM_ERROR";
        } else if (message.contains("DB 저장 오류")) {
            category = "DATABASE_ERROR";
        } else if (message.contains("재생목록 항목 조회 실패") || message.contains("응답 없음")) {
            category = "API_RESPONSE_ERROR";
        }
        errorSummary.put(category, errorSummary.getOrDefault(category, 0) + 1);
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private List<String> fetchAndProcessVideos(List<String> videoIds, String channelName, List<String> excludedSongInfo, List<String> allFailedSongInfo, Map<String, Integer> errorSummary) {
        List<String> newSongTitles = new ArrayList<>();
        if (videoIds == null || videoIds.isEmpty()) return newSongTitles;

        try {
            YouTube.Videos.List videoRequest = youTube.videos().list(List.of("id", "snippet", "contentDetails", "statistics"));
            videoRequest.setId(videoIds);
            VideoListResponse videoResponse = youTubeApiService.executeRequest(videoRequest, "신규 영상 상세 조회 batch (size: " + videoIds.size() + ") - " + channelName);
            if (videoResponse == null || videoResponse.getItems().isEmpty()) return newSongTitles;

            for (Video video : videoResponse.getItems()) {
                String videoTitle = video.getSnippet().getTitle();
                String videoId = video.getId();

                if (validationService.isSongAlreadyExists(videoId)) continue;

                String classification = validationService.classifyVideo(video);
                if ("ignore".equals(classification)) continue;

                if (!validationService.isSongRelated(video)) continue;

                if (video.getStatistics() == null || video.getStatistics().getViewCount() == null) continue;

                try {
                    saveNewSong(video, video.getStatistics(), channelName, classification);
                    newSongTitles.add(String.format("%s (분류: %s)", videoTitle, classification));
                } catch (Exception e) {
                    allFailedSongInfo.add(String.format("[%s] %s - 실패 사유: DB 저장 오류", channelName, videoTitle));
                }
            }
        } catch (IOException e) {
            logger.error("비디오 정보를 가져오는 동안 IOException 발생", e);
        }
        return newSongTitles;
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private void saveNewSong(Video video, VideoStatistics statistics, String channelName, String classification) {
        VtuberSongsEntity song = new VtuberSongsEntity();
        song.setChannelId(video.getSnippet().getChannelId());
        song.setVideoId(video.getId());
        song.setTitle(video.getSnippet().getTitle());
        song.setPublishedAt(Instant.ofEpochMilli(video.getSnippet().getPublishedAt().getValue()).atZone(ZoneId.systemDefault()).toLocalDateTime());
        song.setAddedTime(LocalDateTime.now(SEOUL_ZONE));
        song.setUpdateDayTime(LocalDateTime.now(SEOUL_ZONE));
        song.setUpdateWeekTime(LocalDateTime.now(SEOUL_ZONE));
        song.setVtuberName(channelName);
        song.setViewCount(statistics.getViewCount().longValue());
        song.setViewsIncreaseDay(0L);
        song.setViewsIncreaseWeek(0L);
        song.setLastWeekViewCount(statistics.getViewCount().longValue());
        song.setStatus("new");
        song.setClassification(classification);
        vtuberSongsRepository.save(song);
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private void fetchAllSongsFromPlaylist(String channelId, String channelName, List<String> allNewSongTitles, List<String> excludedSongInfo, List<String> allFailedSongInfo, Map<String, Integer> errorSummary) {
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(channelId);
            if (uploadsPlaylistId == null) return;
            String pageToken = null;
            do {
                YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems().list(List.of("contentDetails", "snippet"));
                playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
                playlistItemsRequest.setMaxResults(50L);
                playlistItemsRequest.setPageToken(pageToken);

                PlaylistItemListResponse playlistItemResult = youTubeApiService.executeRequest(playlistItemsRequest, "채널 전체 재생목록 항목 조회: " + channelName);
                if (playlistItemResult == null) break;
                
                List<String> videoIds = playlistItemResult.getItems().stream()
                        .map(item -> item.getContentDetails().getVideoId())
                        .collect(Collectors.toList());
                allNewSongTitles.addAll(fetchAndProcessVideos(videoIds, channelName, excludedSongInfo, allFailedSongInfo, errorSummary));
                pageToken = playlistItemResult.getNextPageToken();
            } while (pageToken != null);
        } catch (Exception e) {
            logger.error("재생목록 처리 중 오류 발생", e);
        }
    }

    private String getUploadsPlaylistId(String channelId) throws IOException {
        YouTube.Channels.List channelRequest = youTube.channels().list(List.of("contentDetails"));
        channelRequest.setId(List.of(channelId));
        ChannelListResponse channelResult = youTubeApiService.executeRequest(channelRequest, "업로드 재생목록 ID 조회: " + channelId);
        if (channelResult.getItems() != null && !channelResult.getItems().isEmpty()) {
            return channelResult.getItems().get(0).getContentDetails().getRelatedPlaylists().getUploads();
        }
        return null;
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private void fetchRecentSongsFromSearch(String channelId, String channelName, List<String> allNewSongTitles, List<String> excludedSongInfo, List<String> allFailedSongInfo, Map<String, Integer> errorSummary) {
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(channelId);
            if (uploadsPlaylistId == null) return;

            YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems().list(List.of("contentDetails"));
            playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
            playlistItemsRequest.setMaxResults(10L);

            PlaylistItemListResponse playlistItemResult = youTubeApiService.executeRequest(playlistItemsRequest, "채널 최근 노래 검색: " + channelName);
            if (playlistItemResult == null) return;

            List<String> videoIds = playlistItemResult.getItems().stream()
                    .map(item -> item.getContentDetails().getVideoId())
                    .collect(Collectors.toList());
            allNewSongTitles.addAll(fetchAndProcessVideos(videoIds, channelName, excludedSongInfo, allFailedSongInfo, errorSummary));
        } catch (Exception e) {
            logger.error("최근 노래 검색 중 오류 발생", e);
        }
    }
}
