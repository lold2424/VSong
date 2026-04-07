package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

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
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(UpdateVtuberSongsService.class);
    private final Map<String, Object> lastOperationStats = new java.util.concurrent.ConcurrentHashMap<>();

    public Map<String, Object> getLastOperationStats() {
        if (lastOperationStats.isEmpty()) {
            SongUpdateLog latestLog = songUpdateLogRepository.findLatestLog();
            if (latestLog != null) {
                lastOperationStats.put("lastRunTime", latestLog.getRunTime());
                lastOperationStats.put("durationSeconds", latestLog.getDurationSeconds());
                lastOperationStats.put("newSongsCount", latestLog.getNewSongsCount());
                lastOperationStats.put("excludedSongsCount", latestLog.getExcludedSongsCount());
                lastOperationStats.put("failedSongsCount", latestLog.getFailedSongsCount());
                try {
                    lastOperationStats.put("errorSummary", objectMapper.readValue(latestLog.getErrorSummaryJson(), Map.class));
                    lastOperationStats.put("slowestChannels", objectMapper.readValue(latestLog.getSlowestChannelsJson(), List.class));
                } catch (Exception e) {
                    logger.error("Error reading saved stats from DB", e);
                }
            }
        }
        return lastOperationStats;
    }

    public List<SongUpdateLog> getRecentLogs() {
        return songUpdateLogRepository.findRecentLogs();
    }

    public UpdateVtuberSongsService(
            YouTube youTube,
            VtuberRepository vtuberRepository,
            VtuberSongsRepository vtuberSongsRepository,
            VtuberValidationService validationService,
            YouTubeApiService youTubeApiService,
            MainPageService mainPageService,
            CacheManager cacheManager,
            SongUpdateLogRepository songUpdateLogRepository,
            ObjectMapper objectMapper) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.validationService = validationService;
        this.youTubeApiService = youTubeApiService;
        this.mainPageService = mainPageService;
        this.cacheManager = cacheManager;
        this.songUpdateLogRepository = songUpdateLogRepository;
        this.objectMapper = objectMapper;
    }

    public void fetchVtuberSongs() {
        logger.info("=== fetchVtuberSongs 실행 시작 ===");
        LocalDateTime startTime = LocalDateTime.now();
        List<String> allNewSongTitles = new ArrayList<>();
        List<String> allExcludedSongInfo = new ArrayList<>();
        List<String> allFailedSongInfo = new ArrayList<>();
        Map<String, Integer> errorSummary = new HashMap<>();
        List<Map<String, Object>> performanceLogs = new ArrayList<>();

        List<VtuberEntity> newVtubers = vtuberRepository.findByStatus("new");
        List<VtuberEntity> existingVtubers = vtuberRepository.findByStatus("existing");

        if (logger.isInfoEnabled()) {
            logger.info("조회된 new Vtubers 수: {}", newVtubers.size());
            logger.info("조회된 existing Vtubers 수: {}", existingVtubers.size());
        }

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

        lastOperationStats.put("lastRunTime", LocalDateTime.now());
        lastOperationStats.put("durationSeconds", java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds());
        lastOperationStats.put("newSongsCount", allNewSongTitles.size());
        lastOperationStats.put("excludedSongsCount", allExcludedSongInfo.size());
        lastOperationStats.put("failedSongsCount", allFailedSongInfo.size());
        lastOperationStats.put("errorSummary", errorSummary);
        lastOperationStats.put("newSongs", new ArrayList<>(allNewSongTitles));
        lastOperationStats.put("excludedSongs", new ArrayList<>(allExcludedSongInfo));
        lastOperationStats.put("failedSongs", new ArrayList<>(allFailedSongInfo));
        lastOperationStats.put("slowestChannels", slowestChannels);

        try {
            SongUpdateLog log = new SongUpdateLog();
            log.setRunTime(LocalDateTime.now());
            log.setDurationSeconds((Long) lastOperationStats.get("durationSeconds"));
            log.setNewSongsCount(allNewSongTitles.size());
            log.setExcludedSongsCount(allExcludedSongInfo.size());
            log.setFailedSongsCount(allFailedSongInfo.size());
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

        newSongs.forEach(song -> {
            song.setStatus("existing");
            song.setUpdateDayTime(LocalDateTime.now());
            vtuberSongsRepository.save(song);
        });

        if (logger.isInfoEnabled()) {
            logger.info("Updated {} songs from 'new' to 'existing'.", newSongs.size());
        }
    }

    @SuppressWarnings("PMD.LooseCoupling")
    public void updateViewCounts() {
        List<VtuberSongsEntity> songs = vtuberSongsRepository.findAll();
        if (logger.isInfoEnabled()) {
            logger.info("조회수 업데이트를 위해 {}개의 노래를 찾았습니다.", songs.size());
        }
        if (songs.isEmpty()) {
            return;
        }

        Map<String, VtuberSongsEntity> songMap = songs.stream()
                .collect(Collectors.toMap(VtuberSongsEntity::getVideoId, song -> song, (existing, replacement) -> {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Duplicate videoId found: {}. Discarding one of the entries.", existing.getVideoId());
                    }
                    return existing;
                }));
        List<String> videoIds = new ArrayList<>(songMap.keySet());

        int updatedCount = 0;
        int deletedCount = 0;
        int failedCount = 0;
        int weeklyUpdatedCount = 0;
        int weeklyFailedCount = 0;

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
                            if (song != null && video.getStatistics() != null) {
                                if (video.getStatistics().getViewCount() == null) {
                                    if (logger.isWarnEnabled()) {
                                        logger.warn("조회수 정보를 가져올 수 없습니다 (videoId: {}). 건너뜁니다.", video.getId());
                                    }
                                    continue;
                                }
                                long newViewCount = video.getStatistics().getViewCount().longValue();
                                long viewIncreaseDay = newViewCount - song.getViewCount();
                                song.setViewCount(newViewCount);
                                song.setViewsIncreaseDay(viewIncreaseDay);
                                song.setUpdateDayTime(LocalDateTime.now());

                                song.setViewsIncreaseWeek(song.getViewsIncreaseWeek() + viewIncreaseDay);

                                if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                                    try {
                                        song.setViewsIncreaseWeek(0L);
                                        song.setLastWeekViewCount(newViewCount);
                                        song.setUpdateWeekTime(LocalDateTime.now());
                                        weeklyUpdatedCount++;
                                    } catch (Exception e) {
                                        weeklyFailedCount++;
                                        if (logger.isErrorEnabled()) {
                                            logger.error("주간 조회수 초기화 및 기준점 설정 실패 (videoId: {})", song.getVideoId(), e);
                                        }
                                    }
                                }
                                vtuberSongsRepository.save(song);
                                updatedCount++;
                            } else {
                                failedCount++;
                                if (song == null) {
                                    if (logger.isWarnEnabled()) {
                                        logger.warn("조회수 업데이트 실패 (videoId: {}): DB에서 해당 노래를 찾을 수 없습니다.", video.getId());
                                    }
                                } else {
                                    if (logger.isWarnEnabled()) {
                                        logger.warn("조회수 업데이트 실패 (videoId: {}): YouTube API에서 통계 정보를 반환하지 않았습니다.", video.getId());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            failedCount++;
                            if (logger.isErrorEnabled()) {
                                logger.error("조회수 업데이트 중 예외 발생 (videoId: {})", video.getId(), e);
                            }
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
                if (logger.isErrorEnabled()) {
                    logger.error("조회수 업데이트 배치 실패. 다음 동영상 ID들이 영향을 받았습니다: {}", String.join(", ", batch), e);
                }
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("일일 조회수 업데이트 완료. 업데이트: {}개, 삭제: {}개, 실패: {}개", updatedCount, deletedCount, failedCount);
        }
        if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
            if (logger.isInfoEnabled()) {
                logger.info("주간 조회수 업데이트 요약. 성공: {}개, 실패: {}개", weeklyUpdatedCount, weeklyFailedCount);
            }
        }
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
        if (videoIds == null || videoIds.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("채널 [{}]에서 처리할 비디오 ID가 없습니다.", channelName);
            }
            return newSongTitles;
        }

        try {
            YouTube.Videos.List videoRequest = youTube.videos().list(List.of("id", "snippet", "contentDetails", "statistics"));
            videoRequest.setId(videoIds);
            VideoListResponse videoResponse = youTubeApiService.executeRequest(videoRequest);

            if (videoResponse == null || videoResponse.getItems().isEmpty()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("채널 [{}]의 비디오 ID 목록에 대한 세부 정보를 가져오지 못했습니다.", channelName);
                }
                String errorMsg = String.format("[%s] 비디오 세부 정보 응답 없음 (API 응답 null 또는 빈 목록)", channelName);
                allFailedSongInfo.add(errorMsg);
                summarizeError(errorMsg, errorSummary);
                return newSongTitles;
            }

            for (Video video : videoResponse.getItems()) {
                String videoTitle = video.getSnippet().getTitle();
                String videoId = video.getId();

                if (validationService.isSongAlreadyExists(videoId)) {
                    excludedSongInfo.add(String.format("[%s] %s - 제외 사유: 이미 DB에 존재함", channelName, videoTitle));
                    continue;
                }

                String classification = validationService.classifyVideo(video);
                if ("ignore".equals(classification)) {
                    excludedSongInfo.add(String.format("[%s] %s - 제외 사유: 분류 제외 (길이 초과, 쇼츠 등)", channelName, videoTitle));
                    continue;
                }

                if (!validationService.isSongRelated(video)) {
                    excludedSongInfo.add(String.format("[%s] %s - 제외 사유: 노래와 관련 없음 (AI 판별 또는 키워드 부족)", channelName, videoTitle));
                    continue;
                }

                if (video.getStatistics() == null || video.getStatistics().getViewCount() == null) {
                    excludedSongInfo.add(String.format("[%s] %s - 제외 사유: 조회수 정보 없음 (회원 전용 등)", channelName, videoTitle));
                    if (logger.isWarnEnabled()) {
                        logger.warn("비디오 통계 정보(조회수)가 없어 DB에 추가하지 않습니다 (videoId: {}, title: {}). 회원용 동영상일 수 있습니다.", videoId, videoTitle);
                    }
                    continue;
                }

                try {
                    saveNewSong(video, video.getStatistics(), channelName, classification);
                    newSongTitles.add(String.format("%s (분류: %s)", videoTitle, classification));
                } catch (Exception e) {
                    String errorMsg = String.format("[%s] %s - 실패 사유: DB 저장 오류 (%s)", channelName, videoTitle, e.getMessage());
                    allFailedSongInfo.add(errorMsg);
                    summarizeError(errorMsg, errorSummary);
                    if (logger.isErrorEnabled()) {
                        logger.error("노래 저장 중 오류 발생 - 비디오: {}", videoTitle, e);
                    }
                }
            }

            if (!newSongTitles.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("채널 [{}]에 {}개의 새로운 노래가 추가되었습니다.", channelName, newSongTitles.size());
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("채널 [{}]에서 새로 추가할 노래가 없습니다.", channelName);
                }
            }

        } catch (IOException e) {
            String errorMsg = String.format("[%s] 시스템 오류: 비디오 정보 조회 중 IOException 발생 (%s)", channelName, e.getMessage());
            allFailedSongInfo.add(errorMsg);
            summarizeError(errorMsg, errorSummary);
            if (logger.isErrorEnabled()) {
                logger.error("비디오 정보를 가져오는 동안 IOException 발생 {}", channelName, e);
            }
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
        song.setAddedTime(LocalDateTime.now());
        song.setUpdateDayTime(LocalDateTime.now());
        song.setUpdateWeekTime(LocalDateTime.now());
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
        if (logger.isInfoEnabled()) {
            logger.info("채널 [{}]에서 모든 노래를 가져옵니다. 채널 ID: {}", channelName, channelId);
        }
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(channelId);
            if (uploadsPlaylistId == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("channel ID에 대한 업로드 재생목록 찾을 수 없음: {}", channelId);
                }
                String errorMsg = String.format("[%s] 업로드 재생목록 ID 조회 실패", channelName);
                allFailedSongInfo.add(errorMsg);
                summarizeError(errorMsg, errorSummary);
                return;
            }
            String pageToken = null;
            do {
                YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems().list(List.of("contentDetails", "snippet"));
                playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
                playlistItemsRequest.setMaxResults(50L);
                playlistItemsRequest.setPageToken(pageToken);

                PlaylistItemListResponse playlistItemResult = youTubeApiService.executeRequest(playlistItemsRequest);
                if (playlistItemResult == null) {
                    String errorMsg = String.format("[%s] 재생목록 항목 조회 실패 (API 응답 null)", channelName);
                    allFailedSongInfo.add(errorMsg);
                    summarizeError(errorMsg, errorSummary);
                    break;
                }
                List<PlaylistItem> playlistItems = playlistItemResult.getItems();


                List<String> videoIds = new ArrayList<>();
                for (PlaylistItem item : playlistItems) {
                    videoIds.add(item.getContentDetails().getVideoId());
                }
                allNewSongTitles.addAll(fetchAndProcessVideos(videoIds, channelName, excludedSongInfo, allFailedSongInfo, errorSummary));
                pageToken = playlistItemResult.getNextPageToken();
            } while (pageToken != null);
        } catch (Exception e) {
            String errorMsg = String.format("[%s] 시스템 오류: 재생목록 처리 중 예외 발생 (%s)", channelName, e.getMessage());
            allFailedSongInfo.add(errorMsg);
            summarizeError(errorMsg, errorSummary);
            if (logger.isErrorEnabled()) {
                logger.error("API 호출 중 오류 발생 - 채널명: {}, 채널 ID: {}", channelName, channelId, e);
            }
        }
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private String getUploadsPlaylistId(String channelId) throws IOException {
        YouTube.Channels.List channelRequest = youTube.channels().list(List.of("contentDetails"));
        channelRequest.setId(List.of(channelId));
        ChannelListResponse channelResult = youTubeApiService.executeRequest(channelRequest);
        List<Channel> channelsList = channelResult.getItems();
        if (channelsList != null && !channelsList.isEmpty()) {
            return channelsList.get(0).getContentDetails().getRelatedPlaylists().getUploads();
        }
        return null;
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private void fetchRecentSongsFromSearch(String channelId, String channelName, List<String> allNewSongTitles, List<String> excludedSongInfo, List<String> allFailedSongInfo, Map<String, Integer> errorSummary) {
        if (logger.isInfoEnabled()) {
            logger.info("기존 Vtuber 최근 노래 검색 시작 (API): {}", channelName);
        }
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(channelId);
            if (uploadsPlaylistId == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("채널 ID [{}]에 대한 업로드 재생목록을 찾을 수 없음: {}", channelName, channelId);
                }
                String errorMsg = String.format("[%s] 업로드 재생목록 ID 조회 실패", channelName);
                allFailedSongInfo.add(errorMsg);
                summarizeError(errorMsg, errorSummary);
                return;
            }

            YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems().list(List.of("contentDetails"));
            playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
            playlistItemsRequest.setMaxResults(10L);

            PlaylistItemListResponse playlistItemResult = youTubeApiService.executeRequest(playlistItemsRequest);
            if (playlistItemResult == null) {
                String errorMsg = String.format("[%s] 최신 비디오 목록 조회 실패 (API 응답 null)", channelName);
                allFailedSongInfo.add(errorMsg);
                summarizeError(errorMsg, errorSummary);
                return;
            }
            List<PlaylistItem> playlistItems = playlistItemResult.getItems();

            if (playlistItems == null || playlistItems.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("채널 [{}]의 업로드 재생목록에서 비디오 항목을 찾을 수 없음", channelName);
                }
                return;
            }

            List<String> videoIds = new ArrayList<>();
            for (PlaylistItem item : playlistItems) {
                videoIds.add(item.getContentDetails().getVideoId());
            }

            if (!videoIds.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("채널 [{}]에서 {}개의 최신 비디오를 찾았습니다. 처리 중...", channelName, videoIds.size());
                }
                allNewSongTitles.addAll(fetchAndProcessVideos(videoIds, channelName, excludedSongInfo, allFailedSongInfo, errorSummary));
            }
        } catch (IOException e) {
            String errorMsg = String.format("[%s] 시스템 오류: 최근 노래 조회 중 IOException (%s)", channelName, e.getMessage());
            allFailedSongInfo.add(errorMsg);
            summarizeError(errorMsg, errorSummary);
            if (logger.isErrorEnabled()) {
                logger.error("채널 [{}]의 최근 노래를 가져오는 중 API 오류 발생: {}", channelName, e.getMessage(), e);
            }
        } catch (Exception e) {
            String errorMsg = String.format("[%s] 알 수 없는 오류: 최근 노래 처리 중 예외 (%s)", channelName, e.getMessage());
            allFailedSongInfo.add(errorMsg);
            summarizeError(errorMsg, errorSummary);
            if (logger.isErrorEnabled()) {
                logger.error("채널 [{}]의 최근 노래 처리 중 알 수 없는 오류 발생: {}", channelName, e.getMessage(), e);
            }
        }
    }
}
