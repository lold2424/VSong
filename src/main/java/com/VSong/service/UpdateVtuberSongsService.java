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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UpdateVtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberValidationService validationService;
    private final YouTubeApiService youTubeApiService;
    private final MainPageService mainPageService;
    private final CacheManager cacheManager;
    private static final Logger logger = LoggerFactory.getLogger(UpdateVtuberSongsService.class);

    public UpdateVtuberSongsService(
            YouTube youTube,
            VtuberRepository vtuberRepository,
            VtuberSongsRepository vtuberSongsRepository,
            VtuberValidationService validationService,
            YouTubeApiService youTubeApiService,
            MainPageService mainPageService,
            CacheManager cacheManager) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.validationService = validationService;
        this.youTubeApiService = youTubeApiService;
        this.mainPageService = mainPageService;
        this.cacheManager = cacheManager;
    }

    public void fetchVtuberSongs() {
        logger.info("=== fetchVtuberSongs 실행 시작 ===");

        List<VtuberEntity> newVtubers = vtuberRepository.findByStatus("new");
        List<VtuberEntity> existingVtubers = vtuberRepository.findByStatus("existing");

        if (logger.isInfoEnabled()) {
            logger.info("조회된 new Vtubers 수: {}", newVtubers.size());
            logger.info("조회된 existing Vtubers 수: {}", existingVtubers.size());
        }

        for (VtuberEntity vtuber : newVtubers) {
            if (logger.isInfoEnabled()) {
                logger.info("새로운 Vtuber 처리 시작: {}", vtuber.getName());
            }
            fetchAllSongsFromPlaylist(vtuber.getChannelId(), vtuber.getName());
            vtuber.setStatus("existing");
            vtuberRepository.save(vtuber);
            if (logger.isInfoEnabled()) {
                logger.info("새로운 Vtuber 처리 완료: {}", vtuber.getName());
            }
        }


        for (VtuberEntity vtuber : existingVtubers) {
            if (logger.isInfoEnabled()) {
                logger.info("기존 Vtuber 최근 노래 검색 시작: {}", vtuber.getName());
            }
            fetchRecentSongsFromSearch(vtuber.getChannelId(), vtuber.getName());
        }

        logger.info("메인 페이지 캐시를 초기화하고 다시 채웁니다.");
        Objects.requireNonNull(cacheManager.getCache("mainPage")).clear();
        mainPageService.getCacheableMainPageData("all");

        logger.info("=== fetchVtuberSongs 실행 종료 ===");
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
                                long newViewCount = video.getStatistics().getViewCount().longValue();
                                long viewIncreaseDay = newViewCount - song.getViewCount();
                                song.setViewCount(newViewCount);
                                song.setViewsIncreaseDay(viewIncreaseDay);
                                song.setUpdateDayTime(LocalDateTime.now());

                                // Always accumulate weekly increase
                                song.setViewsIncreaseWeek(song.getViewsIncreaseWeek() + viewIncreaseDay);

                                // Reset lastWeekViewCount and viewsIncreaseWeek on Monday
                                if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                                    try {
                                        song.setViewsIncreaseWeek(0L); // Reset for the new week
                                        song.setLastWeekViewCount(newViewCount); // Set new baseline for the week
                                        song.setUpdateWeekTime(LocalDateTime.now());
                                        weeklyUpdatedCount++; // This count is for the reset, not accumulation
                                    } catch (Exception e) {
                                        weeklyFailedCount++;
                                        logger.error("주간 조회수 초기화 및 기준점 설정 실패 (videoId: {})", song.getVideoId(), e);
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
                            logger.error("조회수 업데이트 중 예외 발생 (videoId: {})", video.getId(), e);
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
                logger.error("조회수 업데이트 배치 실패. 다음 동영상 ID들이 영향을 받았습니다: {}", String.join(", ", batch), e);
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

    private void fetchAndProcessVideos(List<String> videoIds, String channelName) {
        if (videoIds == null || videoIds.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("채널 [{}]에서 처리할 비디오 ID가 없습니다.", channelName);
            }
            return;
        }

        int newSongsAddedCount = 0;

        try {
            // Fetch video details including snippet, contentDetails, and statistics in one go
            YouTube.Videos.List videoRequest = youTube.videos().list(List.of("id", "snippet", "contentDetails", "statistics"));
            videoRequest.setId(videoIds);
            VideoListResponse videoResponse = youTubeApiService.executeRequest(videoRequest);

            if (videoResponse == null || videoResponse.getItems().isEmpty()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("채널 [{}]의 비디오 ID 목록에 대한 세부 정보를 가져오지 못했습니다.", channelName);
                }
                return;
            }

            for (Video video : videoResponse.getItems()) {
                String videoTitle = video.getSnippet().getTitle();
                String videoId = video.getId();
                if (logger.isInfoEnabled()) {
                    logger.info("채널 [{}]의 비디오 처리 중: \"{}\"", channelName, videoTitle);
                }

                if (validationService.isSongAlreadyExists(videoId)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("비디오가 이미 DB에 존재하여 건너뜁니다: {}", videoTitle);
                    }
                    continue;
                }

                if (!validationService.isSongRelated(video)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("비디오가 노래 관련이 아니므로 건너뜁니다: {}", videoTitle);
                    }
                    continue;
                }

                String classification = validationService.classifyVideo(video);
                if ("ignore".equals(classification)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("비디오 분류가 'ignore'이므로 건너뜁니다: {}", videoTitle);
                    }
                    continue;
                }

                // If all checks pass, save the new song
                saveNewSong(video, video.getStatistics(), channelName, classification);
                newSongsAddedCount++;
            }

            if (newSongsAddedCount > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("채널 [{}]에 {}개의 새로운 노래가 추가되었습니다.", channelName, newSongsAddedCount);
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("채널 [{}]에서 새로 추가할 노래가 없습니다.", channelName);
                }
            }

        } catch (IOException e) {
            logger.error("비디오 정보를 가져오는 동안 IOException 발생 {}", channelName, e);
        }
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
        song.setLastWeekViewCount(statistics.getViewCount().longValue());
        song.setStatus("new");
        song.setClassification(classification);

        vtuberSongsRepository.save(song);
        if (logger.isInfoEnabled()) {
            logger.info("성공적으로 노래 저장: {}", song.getTitle());
        }
    }

    private void fetchAllSongsFromPlaylist(String channelId, String channelName) {
        if (logger.isInfoEnabled()) {
            logger.info("채널 [{}]에서 모든 노래를 가져옵니다. 채널 ID: {}", channelName, channelId);
        }
        try {
            String uploadsPlaylistId = getUploadsPlaylistId(channelId);
            if (uploadsPlaylistId == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("channel ID에 대한 업로드 재생목록 찾을 수 없음: {}", channelId);
                }
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
            logger.error("API 호출 중 오류 발생 - 채널명: {}, 채널 ID: {}", channelName, channelId, e);
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

    private void fetchRecentSongsFromSearch(String channelId, String channelName) {
        String rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;

        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(rssUrl).timeout(10000).get();
            org.jsoup.select.Elements entries = doc.select("entry");

            if (entries.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("RSS 피드에 비디오 항목 탐색 불가 {}", channelName);
                }
                return;
            }

            List<String> videoIds = new ArrayList<>();
            for (org.jsoup.nodes.Element entry : entries) {
                String videoId = entry.select("yt|videoId").first().text();
                videoIds.add(videoId);
            }

            if (!videoIds.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Found {} RSS 최신 비디오 {}. Processing...", videoIds.size(), channelName);
                }
                fetchAndProcessVideos(videoIds, channelName); // <-- BUG FIX: This call was missing
            }
        } catch (Exception e) {
            logger.error("RSS 피드를 가져오거나 구문 분석하는 데 실패 {}", channelName, e);
        }
    }


}
