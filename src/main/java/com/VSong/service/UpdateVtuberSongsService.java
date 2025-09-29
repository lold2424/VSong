package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Service
public class UpdateVtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberValidationService validationService; // 검증 서비스 주입
    private final List<String> apiKeys;
    private final List<Boolean> keyUsage;
    private int currentKeyIndex = 0;
    private static final Logger logger = Logger.getLogger(UpdateVtuberSongsService.class.getName());

    public UpdateVtuberSongsService(
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
        this.keyUsage = new ArrayList<>(apiKeys.size());
        for (int i = 0; i < apiKeys.size(); i++) {
            this.keyUsage.add(true);
        }
    }

    public void fetchVtuberSongs() {
        logger.info("=== fetchVtuberSongs 실행 시작 ===");

        List<VtuberEntity> newVtubers = vtuberRepository.findByStatus("new");
        List<VtuberEntity> existingVtubers = vtuberRepository.findByStatus("existing");

        logger.info("조회된 new Vtubers 수: " + newVtubers.size());
        logger.info("조회된 existing Vtubers 수: " + existingVtubers.size());

        for (VtuberEntity vtuber : newVtubers) {
            logger.info("새로운 Vtuber 처리 시작: " + vtuber.getName());
            fetchAllSongsFromPlaylist(vtuber.getChannelId(), vtuber.getName());
            vtuber.setStatus("existing");
            vtuberRepository.save(vtuber);
            logger.info("새로운 Vtuber 처리 완료: " + vtuber.getName());
        }

        Instant threeDaysAgoInstant = LocalDateTime.now().minusDays(3).toInstant(ZoneOffset.UTC);
        for (VtuberEntity vtuber : existingVtubers) {
            logger.info("기존 Vtuber 최근 노래 검색 시작: " + vtuber.getName());
            fetchRecentSongsFromSearch(vtuber.getChannelId(), vtuber.getName(), threeDaysAgoInstant);
            logger.info("기존 Vtuber 최근 노래 검색 완료: " + vtuber.getName());
        }

        logger.info("=== fetchVtuberSongs 실행 종료 ===");
    }

    public void updateSongStatusToExisting() {
        List<VtuberSongsEntity> newSongs = vtuberSongsRepository.findByStatus("new");

        newSongs.forEach(song -> {
            song.setStatus("existing");
            song.setUpdateDayTime(LocalDateTime.now()); // 상태 변경 시간을 기록
            vtuberSongsRepository.save(song);
        });

        logger.info("Updated " + newSongs.size() + " songs from 'new' to 'existing'.");
    }

    public void updateViewCounts() {
        resetKeyUsage();
        List<VtuberSongsEntity> songs = vtuberSongsRepository.findAll();
        for (VtuberSongsEntity song : songs) {
            long newViewCount = fetchViewCount(song.getVideoId());

            if (newViewCount == 0) {
                logger.info("삭제된 동영상 감지: " + song.getTitle() + " (" + song.getVideoId() + ")");
                vtuberSongsRepository.delete(song);
                continue;
            }

            if (newViewCount > 0) {
                long viewIncreaseDay = newViewCount - song.getViewCount();
                song.setViewCount(newViewCount);
                song.setViewsIncreaseDay(viewIncreaseDay);
                song.setUpdateDayTime(LocalDateTime.now());

                if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                    long viewIncreaseWeek = newViewCount - song.getLastWeekViewCount();
                    song.setViewsIncreaseWeek(viewIncreaseWeek);
                    song.setLastWeekViewCount(newViewCount);
                    song.setUpdateWeekTime(LocalDateTime.now());
                }
                vtuberSongsRepository.save(song);
            }
        }
        logger.info("조회수 업데이트 및 삭제된 동영상 제거 완료");
    }

    private void fetchAndProcessVideos(List<String> videoIds, String channelName) {
        if (videoIds == null || videoIds.isEmpty()) return;

        try {
            YouTube.Videos.List videosRequest = youTube.videos().list(List.of("id", "snippet", "contentDetails", "statistics"));
            videosRequest.setId(videoIds);
            videosRequest.setKey(apiKeys.get(currentKeyIndex));

            VideoListResponse videoResponse = videosRequest.execute();
            List<Video> videos = videoResponse.getItems();

            for (Video video : videos) {
                // 통합된 검증 로직 사용
                if (validationService.isSongRelated(video)) {
                    String classification = validationService.classifyVideo(video);
                    if ("ignore".equals(classification)) continue;

                    if (!validationService.isSongAlreadyExists(video.getId())) {
                        logger.info("노래로 판단된 동영상: " + video.getSnippet().getTitle() + " (" + video.getId() + ")");
                        saveNewSong(video, channelName, classification);
                    } else {
                        logger.info("이미 존재하는 노래: " + video.getSnippet().getTitle());
                    }
                } else {
                    logger.info("노래와 관련 없는 동영상 필터링: " + video.getSnippet().getTitle());
                }
            }
        } catch (IOException e) {
            logger.severe("IOException while fetching video details for " + channelName + ": " + e.getMessage());
            switchApiKey();
        }
    }

    private void handleSearchResults(List<SearchResult> searchResults, String channelName) {
        if (searchResults == null || searchResults.isEmpty()) return;

        List<String> videoIds = new ArrayList<>();
        for (SearchResult result : searchResults) {
            if (result.getId() != null && result.getId().getVideoId() != null) {
                videoIds.add(result.getId().getVideoId());
            }
        }
        if (videoIds.isEmpty()) return;

        fetchAndProcessVideos(videoIds, channelName); // 로직 재사용
    }

    private void saveNewSong(Video video, String channelName, String classification) {
        VtuberSongsEntity song = new VtuberSongsEntity();
        song.setChannelId(video.getSnippet().getChannelId());
        song.setVideoId(video.getId());
        song.setTitle(video.getSnippet().getTitle());
        song.setPublishedAt(Instant.ofEpochMilli(video.getSnippet().getPublishedAt().getValue()).atZone(ZoneId.systemDefault()).toLocalDateTime());
        song.setAddedTime(LocalDateTime.now());
        song.setUpdateDayTime(LocalDateTime.now());
        song.setUpdateWeekTime(LocalDateTime.now());
        song.setVtuberName(channelName);
        song.setViewCount(video.getStatistics().getViewCount().longValue());
        song.setViewsIncreaseDay(0L);
        song.setViewsIncreaseWeek(0L);
        song.setLastWeekViewCount(0L);
        song.setStatus("new");
        song.setClassification(classification);

        vtuberSongsRepository.save(song);
        logger.info("노래 저장 완료: " + song.getTitle());
    }
    
    // --- Helper and private methods for API calls, key rotation etc. ---
    // These methods remain largely the same.

    private void fetchAllSongsFromPlaylist(String channelId, String channelName) {
        logger.info("채널 [" + channelName + "]에서 모든 노래를 가져옵니다. 채널 ID: " + channelId);
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(channelId);
            if (uploadsPlaylistId == null) {
                logger.severe("Uploads playlist not found for channel ID: " + channelId);
                return;
            }
            String pageToken = null;
            do {
                YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems().list(List.of("contentDetails", "snippet"));
                playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
                playlistItemsRequest.setMaxResults(50L);
                playlistItemsRequest.setPageToken(pageToken);
                playlistItemsRequest.setKey(apiKeys.get(currentKeyIndex));

                PlaylistItemListResponse playlistItemResult = executeRequestWithRetry(() -> playlistItemsRequest.execute());
                List<PlaylistItem> playlistItems = playlistItemResult.getItems();

                List<String> videoIds = new ArrayList<>();
                for (PlaylistItem item : playlistItems) {
                    videoIds.add(item.getContentDetails().getVideoId());
                }
                fetchAndProcessVideos(videoIds, channelName);
                pageToken = playlistItemResult.getNextPageToken();
            } while (pageToken != null);
        } catch (Exception e) {
            handleApiException(e, channelName, channelId);
        }
    }

    private String getUploadsPlaylistId(String channelId) throws IOException {
        YouTube.Channels.List channelRequest = youTube.channels().list(List.of("contentDetails"));
        channelRequest.setId(List.of(channelId));
        channelRequest.setKey(apiKeys.get(currentKeyIndex));
        ChannelListResponse channelResult = channelRequest.execute();
        List<Channel> channelsList = channelResult.getItems();
        if (channelsList != null && !channelsList.isEmpty()) {
            return channelsList.get(0).getContentDetails().getRelatedPlaylists().getUploads();
        }
        return null;
    }

    private void fetchRecentSongsFromSearch(String channelId, String channelName, Instant publishedAfterInstant) {
        String combinedQuery = "music|cover|original|official";
        String pageToken = null;
        do {
            try {
                YouTube.Search.List search = youTube.search().list(List.of("id", "snippet"));
                search.setChannelId(channelId);
                search.setQ(combinedQuery);
                search.setType(List.of("video"));
                search.setOrder("date");
                search.setFields("nextPageToken,items(id/videoId)");
                search.setMaxResults(50L);
                search.setKey(apiKeys.get(currentKeyIndex));
                search.setPageToken(pageToken);
                search.setPublishedAfter(publishedAfterInstant.toString());

                SearchListResponse searchResponse = search.execute();
                handleSearchResults(searchResponse.getItems(), channelName);
                pageToken = searchResponse.getNextPageToken();
            } catch (IOException e) {
                handleApiException(e, channelName, channelId);
            }
        } while (pageToken != null);
    }

    private long fetchViewCount(String videoId) {
        try {
            YouTube.Videos.List request = youTube.videos().list(List.of("statistics"));
            request.setId(List.of(videoId));
            request.setKey(apiKeys.get(currentKeyIndex));
            var response = executeRequestWithRetry(() -> request.execute());
            if (!response.getItems().isEmpty()) {
                return response.getItems().get(0).getStatistics().getViewCount().longValue();
            }
        } catch (IOException e) {
            logger.severe("Failed to fetch view counts: " + e.getMessage());
            switchApiKey();
        }
        return 0;
    }

    private void switchApiKey() {
        int initialKeyIndex = currentKeyIndex;
        do {
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
            if (keyUsage.get(currentKeyIndex)) {
                logger.info("API 키 전환: " + apiKeys.get(currentKeyIndex));
                return;
            }
        } while (currentKeyIndex != initialKeyIndex);
        throw new RuntimeException("모든 API 키의 할당량이 소진되었습니다.");
    }

    private void resetKeyUsage() {
        for (int i = 0; i < keyUsage.size(); i++) {
            keyUsage.set(i, true);
        }
    }

    private void handleApiException(Exception e, String channelName, String channelId) {
        logger.severe("API 호출 중 오류 발생 - 채널명: " + channelName + ", 채널 ID: " + channelId + " - 오류 메시지: " + e.getMessage());
        keyUsage.set(currentKeyIndex, false);
        switchApiKey();
    }

    private <T> T executeRequestWithRetry(Callable<T> apiCall) throws IOException {
        int attempts = 0;
        while (attempts < apiKeys.size()) {
            try {
                return apiCall.call();
            } catch (GoogleJsonResponseException e) {
                if (e.getDetails() != null && "quotaExceeded".equals(e.getDetails().getErrors().get(0).getReason())) {
                    logger.warning("API 쿼터 소진. 다음 키로 전환하여 재시도합니다.");
                    keyUsage.set(currentKeyIndex, false);
                    switchApiKey();
                    attempts++;
                } else {
                    throw e;
                }
            } catch (Exception e) {
                throw new IOException("API 호출 중 예상치 못한 오류 발생", e);
            }
        }
        throw new IOException("모든 API 키의 쿼터를 소진하여 더 이상 작업을 진행할 수 없습니다.");
    }
}
