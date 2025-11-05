package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class UpdateVtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberValidationService validationService;
    private final YouTubeApiService youTubeApiService;
    private static final Logger logger = Logger.getLogger(UpdateVtuberSongsService.class.getName());

    public UpdateVtuberSongsService(
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
        List<VtuberSongsEntity> songs = vtuberSongsRepository.findAll();
        List<String> videoIds = songs.stream().map(VtuberSongsEntity::getVideoId).toList();

        int batchSize = 50;
        for (int i = 0; i < videoIds.size(); i += batchSize) {
            List<String> batch = videoIds.subList(i, Math.min(i + batchSize, videoIds.size()));
            try {
                YouTube.Videos.List request = youTube.videos().list(List.of("statistics"));
                request.setId(batch);
                VideoListResponse response = youTubeApiService.executeRequest(request);

                if (response != null) {
                    for (Video video : response.getItems()) {
                        VtuberSongsEntity song = songs.stream().filter(s -> s.getVideoId().equals(video.getId())).findFirst().orElse(null);
                        if (song != null) {
                            long newViewCount = video.getStatistics().getViewCount().longValue();
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
                }
            } catch (IOException e) {
                logger.severe("Failed to fetch view counts for batch: " + e.getMessage());
            }
        }
        logger.info("조회수 업데이트 및 삭제된 동영상 제거 완료");
    }

    private void fetchAndProcessVideos(List<String> videoIds, String channelName) {
        if (videoIds == null || videoIds.isEmpty()) return;

        int newSongsAddedCount = 0; // Counter for newly added songs

        try {
            // 1. Fetch snippet and contentDetails for initial validation
            YouTube.Videos.List initialRequest = youTube.videos().list(List.of("id", "snippet", "contentDetails"));
            initialRequest.setId(videoIds);
            VideoListResponse initialResponse = youTubeApiService.executeRequest(initialRequest);

            List<Video> videosToSave = new ArrayList<>();
            for (Video video : initialResponse.getItems()) {
                if (validationService.isSongRelated(video)) {
                    String classification = validationService.classifyVideo(video);
                    if (!"ignore".equals(classification) && !validationService.isSongAlreadyExists(video.getId())) {
                        // This video is a song and not in the DB, so we'll need its stats.
                        videosToSave.add(video);
                    }
                }
            }

            if (videosToSave.isEmpty()) {
                logger.info("채널 [" + channelName + "]에서 새로 추가할 노래가 없습니다.");
                return;
            }

            // 2. Fetch statistics only for the validated songs
            List<String> videoIdsToFetchStats = videosToSave.stream().map(Video::getId).toList();
            YouTube.Videos.List statsRequest = youTube.videos().list(List.of("statistics"));
            statsRequest.setId(videoIdsToFetchStats);
            VideoListResponse statsResponse = youTubeApiService.executeRequest(statsRequest);

            for (Video statsVideo : statsResponse.getItems()) {
                Video originalVideo = videosToSave.stream().filter(v -> v.getId().equals(statsVideo.getId())).findFirst().orElse(null);
                if (originalVideo != null) {
                    String classification = validationService.classifyVideo(originalVideo);
                    saveNewSong(originalVideo, statsVideo.getStatistics(), channelName, classification);
                    newSongsAddedCount++; // Increment counter
                }
            }
            logger.info("채널 [" + channelName + "]에 " + newSongsAddedCount + "개의 새로운 노래가 추가되었습니다.");

        } catch (IOException e) {
            logger.severe("IOException while fetching video details for " + channelName + ": " + e.getMessage());
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
        song.setLastWeekViewCount(0L);
        song.setStatus("new");
        song.setClassification(classification);

        vtuberSongsRepository.save(song);
        logger.info("노래 저장 완료: " + song.getTitle());
    }

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

                PlaylistItemListResponse playlistItemResult = youTubeApiService.executeRequest(playlistItemsRequest);
                List<PlaylistItem> playlistItems = playlistItemResult.getItems();


                List<String> videoIds = new ArrayList<>();
                for (PlaylistItem item : playlistItems) {
                    videoIds.add(item.getContentDetails().getVideoId());
                }
                fetchAndProcessVideos(videoIds, channelName);
                pageToken = playlistItemResult.getNextPageToken();
            } while (pageToken != null);
        } catch (Exception e) {
            logger.severe("API 호출 중 오류 발생 - 채널명: " + channelName + ", 채널 ID: " + channelId + " - 오류 메시지: " + e.getMessage());
        }
    }

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

    private void fetchRecentSongsFromSearch(String channelId, String channelName, Instant publishedAfterInstant) {
        String rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;

        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(rssUrl).timeout(10000).get();
            org.jsoup.select.Elements entries = doc.select("entry");

            if (entries.isEmpty()) {
                logger.info("No video entries found in RSS feed for " + channelName);
                return;
            }

            List<String> videoIds = new ArrayList<>();
            for (org.jsoup.nodes.Element entry : entries) {
                String videoId = entry.select("yt|videoId").first().text();
                videoIds.add(videoId);
            }

            if (!videoIds.isEmpty()) {
                logger.info("Found " + videoIds.size() + " recent videos from RSS for " + channelName + ". Processing...");
                fetchAndProcessVideos(videoIds, channelName);
            }
        } catch (Exception e) {
            logger.severe("Failed to fetch or parse RSS feed for " + channelName + ": " + e.getMessage());
        }
    }

    private long fetchViewCount(String videoId) {
        try {
            YouTube.Videos.List request = youTube.videos().list(List.of("statistics"));
            request.setId(List.of(videoId));
            var response = youTubeApiService.executeRequest(request);
            if (response != null && !response.getItems().isEmpty()) {
                return response.getItems().get(0).getStatistics().getViewCount().longValue();
            }
        } catch (IOException e) {
            logger.severe("Failed to fetch view counts: " + e.getMessage());
        }
        return 0;
    }
}
