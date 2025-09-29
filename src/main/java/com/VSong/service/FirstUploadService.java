package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.VSong.repository.ExceptVtuberRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

@Service
public class FirstUploadService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final List<String> apiKeys;
    private final List<AtomicInteger> apiKeyUsage;
    private final List<Boolean> keyAvailable;
    private int currentKeyIndex = 0;
    
    private static final Logger logger = LoggerFactory.getLogger(FirstUploadService.class);
    private static final List<String> SONG_KEYWORDS = List.of("music", "song", "cover", "original", "official", "mv", "뮤직", "노래", "커버");

    // 기존 상수들을 @Value로 변경
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
            ExceptVtuberRepository exceptVtuberRepository,
            @Value("${youtube.api.keys}") List<String> apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.apiKeys = new ArrayList<>(apiKeys);
        this.apiKeyUsage = new ArrayList<>();
        this.keyAvailable = new ArrayList<>();
        
        for (int i = 0; i < apiKeys.size(); i++) {
            this.apiKeyUsage.add(new AtomicInteger(0));
            this.keyAvailable.add(true);
        }
    }

    /**
     * 매일 자정에 실행되어 VTuber 데이터를 점진적으로 수집
     * 로컬 환경에서만 활성화됨
     */
    public void dailyFirstUpload() {
        if (!firstUploadEnabled) {
            logger.debug("첫 업로드 기능이 비활성화되어 있습니다.");
            return;
        }
        
        logger.info("=== 첫 업로드 작업 시작 ===");
        
        resetDailyUsage();
        
        // 아직 처리되지 않은 VTuber들 조회 (status가 'new'이고 songs가 없는 것들)
        List<VtuberEntity> unprocessedVtubers = getUnprocessedVtubers();
        
        if (unprocessedVtubers.isEmpty()) {
            logger.info("처리할 VTuber가 없습니다. 첫 업로드 작업 완료.");
            return;
        }
        
        logger.info("처리 대기 중인 VTuber 수: {}", unprocessedVtubers.size());
        
        // 하루에 처리할 수만큼 제한
        List<VtuberEntity> todayVtubers = unprocessedVtubers.stream()
                .limit(VTUBERS_PER_DAY)
                .collect(Collectors.toList());
        
        logger.info("오늘 처리할 VTuber 수: {}", todayVtubers.size());
        
        int processedCount = 0;
        for (VtuberEntity vtuber : todayVtubers) {
            if (dailyApiUsage.get() >= DAILY_QUOTA_LIMIT) {
                logger.warn("일일 API 할당량 한계에 도달했습니다. 작업을 중단합니다.");
                break;
            }
            
            try {
                logger.info("VTuber 처리 시작: {} (채널 ID: {})", vtuber.getName(), vtuber.getChannelId());
                
                boolean success = processVtuberSongs(vtuber);
                if (success) {
                    vtuber.setStatus("processed");
                    vtuberRepository.save(vtuber);
                    processedCount++;
                    logger.info("VTuber 처리 완료: {} ({}/{})", vtuber.getName(), processedCount, todayVtubers.size());
                } else {
                    logger.warn("VTuber 처리 실패: {}", vtuber.getName());
                }
                
                // API 호출 간격 조절
                Thread.sleep(1000);
                
            } catch (Exception e) {
                logger.error("VTuber 처리 중 오류 발생: {} - {}", vtuber.getName(), e.getMessage());
            }
        }
        
        logger.info("=== 첫 업로드 작업 완료 - 처리된 VTuber 수: {}, 사용된 API 할당량: {} ===", 
                   processedCount, dailyApiUsage.get());
    }

    /**
     * 수동으로 특정 개수의 VTuber 처리
     */
    public void manualFirstUpload(int count) {
        logger.info("=== 수동 첫 업로드 작업 시작 - 처리할 개수: {} ===", count);
        
        List<VtuberEntity> unprocessedVtubers = getUnprocessedVtubers();
        
        if (unprocessedVtubers.isEmpty()) {
            logger.info("처리할 VTuber가 없습니다.");
            return;
        }
        
        List<VtuberEntity> targetVtubers = unprocessedVtubers.stream()
                .limit(count)
                .collect(Collectors.toList());
        
        int processedCount = 0;
        for (VtuberEntity vtuber : targetVtubers) {
            if (dailyApiUsage.get() >= DAILY_QUOTA_LIMIT) {
                logger.warn("API 할당량 한계에 도달했습니다. 작업을 중단합니다.");
                break;
            }
            
            try {
                logger.info("VTuber 처리 시작: {}", vtuber.getName());
                
                boolean success = processVtuberSongs(vtuber);
                if (success) {
                    vtuber.setStatus("processed");
                    vtuberRepository.save(vtuber);
                    processedCount++;
                } else {
                    logger.warn("VTuber 처리 실패: {}", vtuber.getName());
                }
                
                Thread.sleep(1000);
                
            } catch (Exception e) {
                logger.error("VTuber 처리 중 오류 발생: {} - {}", vtuber.getName(), e.getMessage());
            }
        }
        
        logger.info("=== 수동 첫 업로드 작업 완료 - 처리된 VTuber 수: {} ===", processedCount);
    }

    private List<VtuberEntity> getUnprocessedVtubers() {
        // status가 'new', 'existing', 'processing', 'error'이면서 아직 완전히 처리되지 않은 VTuber들 조회
        return vtuberRepository.findAll().stream()
                .filter(vtuber -> {
                    String status = vtuber.getStatus();
                    // 완전히 처리된 것은 제외
                    if ("processed".equals(status)) {
                        return false;
                    }
                    
                    // 처리 중이거나 오류가 발생한 경우 (중간에 중단된 경우)
                    if ("processing".equals(status) || "error".equals(status)) {
                        return true;
                    }
                    
                    // 새로운 VTuber이거나 기존 VTuber인데 아직 노래가 수집되지 않은 경우
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
        int totalProcessed = 0;
        int totalSongs = 0;
        
        try {
            do {
                if (dailyApiUsage.get() >= DAILY_QUOTA_LIMIT) {
                    logger.warn("API 할당량 한계 도달. 플레이리스트 처리 중단: {}", vtuber.getName());
                    break;
                }
                
                rateLimiter.acquire();
                incrementApiUsage();
                
                YouTube.PlaylistItems.List playlistItemsRequest = youTube.playlistItems()
                        .list(List.of("contentDetails","snippet"));
                playlistItemsRequest.setPlaylistId(uploadsPlaylistId);
                playlistItemsRequest.setMaxResults(50L);
                playlistItemsRequest.setPageToken(pageToken);
                playlistItemsRequest.setKey(getCurrentApiKey());

                PlaylistItemListResponse playlistItemResult = playlistItemsRequest.execute();
                List<PlaylistItem> playlistItems = playlistItemResult.getItems();

                if (playlistItems == null || playlistItems.isEmpty()) {
                    break;
                }

                List<String> videoIds = playlistItems.stream()
                        .map(item -> item.getContentDetails().getVideoId())
                        .collect(Collectors.toList());

                int songsFound = fetchAndProcessVideos(videoIds, vtuber);
                totalSongs += songsFound;
                totalProcessed += playlistItems.size();
                
                logger.info("플레이리스트 페이지 처리 완료: {} - 처리된 비디오: {}, 발견된 노래: {}", 
                           vtuber.getName(), playlistItems.size(), songsFound);

                pageToken = playlistItemResult.getNextPageToken();
                
                // 너무 많은 비디오가 있는 경우 중간에 끊기
                if (totalProcessed >= 1000) {
                    logger.info("최대 처리 한계 도달. 플레이리스트 처리 중단: {} (처리된 비디오: {})", 
                               vtuber.getName(), totalProcessed);
                    break;
                }
                
            } while (pageToken != null);
            
            logger.info("VTuber 플레이리스트 처리 완료: {} - 총 처리된 비디오: {}, 총 발견된 노래: {}", 
                       vtuber.getName(), totalProcessed, totalSongs);
            
            return totalSongs > 0; // 최소 1개의 노래가 발견되면 성공
            
        } catch (GoogleJsonResponseException e) {
            handleApiException(e, vtuber.getName());
            return false;
        } catch (IOException e) {
            logger.error("IO 오류 발생: {} - {}", vtuber.getName(), e.getMessage());
            return false;
        }
    }

    private int fetchAndProcessVideos(List<String> videoIds, VtuberEntity vtuber) {
        int songsFound = 0;
        
        try {
            rateLimiter.acquire();
            incrementApiUsage();
            
            YouTube.Videos.List videosRequest = youTube.videos().list(List.of("id","snippet","contentDetails","statistics"));
            videosRequest.setId(videoIds);
            videosRequest.setKey(getCurrentApiKey());

            VideoListResponse videoResponse = videosRequest.execute();
            List<Video> videos = videoResponse.getItems();

            for (Video video : videos) {
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

    private boolean processVideo(Video video, VtuberEntity vtuber) {
        String title = video.getSnippet().getTitle().toLowerCase();
        
        if (!isSongRelated(title)) {
            return false;
        }

        String videoId = video.getId();
        
        // 이미 존재하는 노래인지 확인
        if (vtuberSongsRepository.findByVideoId(videoId).isPresent()) {
            return false;
        }

        String classification = determineClassification(video);
        if ("ignore".equals(classification)) {
            return false;
        }

        saveNewSong(video, vtuber, classification);
        return true;
    }

    private String determineClassification(Video video) {
        String title = video.getSnippet().getTitle();
        String durationStr = video.getContentDetails().getDuration();
        Duration videoDuration = Duration.parse(durationStr);

        if (videoDuration.compareTo(Duration.ofMinutes(8)) > 0) {
            return "ignore";
        }
        if (videoDuration.compareTo(Duration.ofMinutes(1)) <= 0 || title.toLowerCase().contains("short")) {
            return "shorts";
        }
        return "videos";
    }

    private void saveNewSong(Video video, VtuberEntity vtuber, String classification) {
        VtuberSongsEntity song = new VtuberSongsEntity();
        song.setChannelId(video.getSnippet().getChannelId());
        song.setVideoId(video.getId());
        song.setTitle(video.getSnippet().getTitle());
        song.setPublishedAt(Instant.ofEpochMilli(video.getSnippet().getPublishedAt().getValue())
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
        song.setAddedTime(LocalDateTime.now());
        song.setUpdateDayTime(LocalDateTime.now());
        song.setUpdateWeekTime(LocalDateTime.now());
        song.setVtuberName(vtuber.getName());
        song.setViewCount(video.getStatistics().getViewCount().longValue());
        song.setViewsIncreaseDay(0L);
        song.setViewsIncreaseWeek(0L);
        song.setLastWeekViewCount(0L);
        song.setStatus("existing"); // 첫 업로드에서는 바로 existing으로 설정
        song.setClassification(classification);

        vtuberSongsRepository.save(song);
        logger.debug("새 노래 저장: {} - {}", vtuber.getName(), song.getTitle());
    }

    private boolean isSongRelated(String title) {
        return SONG_KEYWORDS.stream().anyMatch(title::contains);
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private void incrementApiUsage() {
        apiKeyUsage.get(currentKeyIndex).incrementAndGet();
        dailyApiUsage.incrementAndGet();
        
        // API 키별 할당량 체크 (키당 10,000)
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

        logger.error("모든 API 키의 할당량이 소진되었습니다. 작업을 중단합니다.");
        throw new RuntimeException("모든 API 키의 할당량이 소진되었습니다.");
    }

    private void handleApiException(GoogleJsonResponseException e, String vtuberName) {
        int errorCode = e.getStatusCode();
        String errorMessage = e.getMessage();
        
        logger.error("API 호출 중 오류 발생 - VTuber: {} - 상태코드: {} - 오류: {}", 
                    vtuberName, errorCode, errorMessage);
        
        // 403 Forbidden (할당량 초과) 또는 기타 API 키 관련 오류 처리
        if (errorCode == 403 || errorMessage.contains("quotaExceeded") || errorMessage.contains("dailyLimitExceeded")) {
            logger.warn("API 키 할당량 초과 감지. 다음 키로 전환합니다.");
            keyAvailable.set(currentKeyIndex, false);
            try {
                switchApiKey();
            } catch (RuntimeException re) {
                logger.error("모든 API 키가 소진되어 작업을 중단합니다.");
                throw re;
            }
        } else {
            // 다른 종류의 오류는 키 전환 없이 처리
            logger.warn("일시적 API 오류로 판단됩니다. 잠시 후 재시도합니다.");
        }
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

    /**
     * 현재 진행 상황 조회
     */
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