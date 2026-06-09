package com.VSong.controller;

import com.VSong.entity.SongProcessLog;
import com.VSong.service.UpdateVtuberSongsService;
import com.VSong.service.YouTubeApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitoring")
public class AdminMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(AdminMonitoringController.class);
    private final YouTubeApiService youtubeApiService;
    private final UpdateVtuberSongsService updateVtuberSongsService;
    private final com.VSong.service.UploadVtuberService uploadVtuberService;
    private final com.VSong.service.UpdateVtuberService updateVtuberService;
    private final com.VSong.repository.SongUpdateLogRepository songUpdateLogRepository;
    private final com.VSong.repository.VtuberUpdateLogRepository vtuberUpdateLogRepository;
    private final com.VSong.repository.AiRecommendationLogRepository aiRecommendationLogRepository;
    private final com.VSong.repository.ApiQuotaLogRepository apiQuotaLogRepository;
    private final HealthEndpoint healthEndpoint;
    private final java.util.concurrent.ThreadPoolExecutor vtuberSyncExecutor;
    private final com.VSong.repository.SongProcessLogRepository songProcessLogRepository;
    private final com.VSong.repository.VtuberProcessLogRepository vtuberProcessLogRepository;

    public AdminMonitoringController(YouTubeApiService youtubeApiService,
                                     UpdateVtuberSongsService updateVtuberSongsService,
                                     com.VSong.service.UploadVtuberService uploadVtuberService,
                                     com.VSong.service.UpdateVtuberService updateVtuberService,
                                     com.VSong.repository.SongUpdateLogRepository songUpdateLogRepository,
                                     com.VSong.repository.VtuberUpdateLogRepository vtuberUpdateLogRepository,
                                     com.VSong.repository.AiRecommendationLogRepository aiRecommendationLogRepository,
                                     com.VSong.repository.ApiQuotaLogRepository apiQuotaLogRepository,
                                     HealthEndpoint healthEndpoint,
                                     java.util.concurrent.ThreadPoolExecutor vtuberSyncExecutor,
                                     com.VSong.repository.SongProcessLogRepository songProcessLogRepository,
                                     com.VSong.repository.VtuberProcessLogRepository vtuberProcessLogRepository) {
        this.youtubeApiService = youtubeApiService;
        this.updateVtuberSongsService = updateVtuberSongsService;
        this.uploadVtuberService = uploadVtuberService;
        this.updateVtuberService = updateVtuberService;
        this.songUpdateLogRepository = songUpdateLogRepository;
        this.vtuberUpdateLogRepository = vtuberUpdateLogRepository;
        this.aiRecommendationLogRepository = aiRecommendationLogRepository;
        this.apiQuotaLogRepository = apiQuotaLogRepository;
        this.healthEndpoint = healthEndpoint;
        this.vtuberSyncExecutor = vtuberSyncExecutor;
        this.songProcessLogRepository = songProcessLogRepository;
        this.vtuberProcessLogRepository = vtuberProcessLogRepository;
    }

    @GetMapping("/ingestion-logs")
    public org.springframework.data.domain.Page<com.VSong.entity.SongProcessLog> getIngestionLogs(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String videoId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String title,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String decision,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        if (videoId != null && !videoId.isEmpty()) {
            List<com.VSong.entity.SongProcessLog> logs = songProcessLogRepository.findByVideoIdOrderByProcessedAtDesc(videoId);
            return new org.springframework.data.domain.PageImpl<>(logs, pageable, logs.size());
        }

        boolean hasDecision = decision != null && !decision.isEmpty();
        boolean hasTitle = title != null && !title.isEmpty();

        if (hasDecision && hasTitle) {
            return songProcessLogRepository.findByDecisionAndTitleContainingOrderByProcessedAtDesc(decision, title, pageable);
        } else if (hasDecision) {
            return songProcessLogRepository.findByDecisionOrderByProcessedAtDesc(decision, pageable);
        } else if (hasTitle) {
            return songProcessLogRepository.findByTitleContainingOrderByProcessedAtDesc(title, pageable);
        }

        return songProcessLogRepository.findAllByOrderByProcessedAtDesc(pageable);
    }

    @GetMapping("/vtuber-process-logs")
    public org.springframework.data.domain.Page<com.VSong.entity.VtuberProcessLog> getVtuberProcessLogs(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String channelId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String channelTitle,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String decision,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        if (channelId != null && !channelId.isEmpty()) {
            return vtuberProcessLogRepository.findByChannelIdOrderByProcessedAtDesc(channelId, pageable);
        }

        boolean hasDecision = decision != null && !decision.isEmpty();
        boolean hasTitle = channelTitle != null && !channelTitle.isEmpty();

        if (hasDecision && hasTitle) {
            return vtuberProcessLogRepository.findByDecisionAndChannelTitleContainingOrderByProcessedAtDesc(decision, channelTitle, pageable);
        } else if (hasDecision) {
            return vtuberProcessLogRepository.findByDecisionOrderByProcessedAtDesc(decision, pageable);
        } else if (hasTitle) {
            return vtuberProcessLogRepository.findByChannelTitleContainingOrderByProcessedAtDesc(channelTitle, pageable);
        }

        return vtuberProcessLogRepository.findAllByOrderByProcessedAtDesc(pageable);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        data.put("youtubeApi", youtubeApiService.getApiKeysStatus());

        Map<String, Object> quotaStats = new HashMap<>();
        quotaStats.put("usageByMethod", apiQuotaLogRepository.getUsageStatsByMethod(java.time.LocalDateTime.now().minusDays(7)));
        quotaStats.put("recentLogs", apiQuotaLogRepository.findTop100ByOrderByRequestTimeDesc());
        data.put("youtubeQuotaStats", quotaStats);

        data.put("songUpdateStats", updateVtuberSongsService.getLastOperationStats());
        data.put("songUpdateHistory", updateVtuberSongsService.getRecentLogs());

        data.put("viewUpdateStats", updateVtuberSongsService.getLastViewUpdateStats());
        data.put("viewUpdateHistory", updateVtuberSongsService.getRecentViewUpdateLogs());

        data.put("vtuberUpdateStats", uploadVtuberService.getLastOperationStats());
        data.put("vtuberUpdateHistory", uploadVtuberService.getRecentLogs());

        Map<String, Object> aiStats = new HashMap<>();
        aiStats.put("totalRequests", aiRecommendationLogRepository.count());
        aiStats.put("successCount", aiRecommendationLogRepository.countBySuccess(true));
        aiStats.put("failCount", aiRecommendationLogRepository.countBySuccess(false));
        
        data.put("aiRecommendationStats", aiStats);
        data.put("aiRecommendationHistory", aiRecommendationLogRepository.findTop100ByOrderByRequestedAtDesc());

        Status healthStatus = healthEndpoint.health().getStatus();
        data.put("systemHealth", healthStatus.getCode());

        return data;
    }

    @GetMapping("/song-log/{id}")
    public com.VSong.entity.SongUpdateLog getSongLog(@PathVariable Long id) {
        return songUpdateLogRepository.findById(id).orElseThrow(() -> new RuntimeException("Log not found"));
    }

    @GetMapping("/vtuber-log/{id}")
    public com.VSong.entity.VtuberUpdateLog getVtuberLog(@PathVariable Long id) {
        return vtuberUpdateLogRepository.findById(id).orElseThrow(() -> new RuntimeException("Log not found"));
    }

    @PostMapping("/run-song-update")
    public Map<String, String> runSongUpdate() {
        logger.info("Admin manually triggered song update (Asynchronous).");

        new Thread(() -> {
            try {
                updateVtuberSongsService.fetchVtuberSongs();
                logger.info("Manual song update completed successfully.");
            } catch (Exception e) {
                logger.error("Error during manual song update: ", e);
            }
        }).start();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "노래 수집 작업이 백그라운드에서 시작되었습니다. 잠시 후 대시보드를 새로고침하여 결과를 확인하세요.");
        return response;
    }

    @PostMapping("/run-vtuber-update")
    public Map<String, String> runVtuberUpdate() {
        logger.info("Admin manually triggered vtuber update (Asynchronous).");

        new Thread(() -> {
            try {
                uploadVtuberService.fetchAndSaveVtuberChannels();
                updateVtuberService.syncVtuberData(vtuberSyncExecutor);
                logger.info("Manual vtuber update completed successfully.");
            } catch (Exception e) {
                logger.error("Error during manual vtuber update: ", e);
            }
        }).start();

        Map<String, String> response = new HashMap<>();
        response.put("message", "버튜버 수집 및 동기화 작업이 백그라운드에서 시작되었습니다. 잠시 후 대시보드를 새로고침하여 결과를 확인하세요.");
        return response;
    }
}
