package com.VSong.controller;

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
    private final HealthEndpoint healthEndpoint;
    private final java.util.concurrent.ThreadPoolExecutor vtuberSyncExecutor;

    public AdminMonitoringController(YouTubeApiService youtubeApiService,
                                     UpdateVtuberSongsService updateVtuberSongsService,
                                     com.VSong.service.UploadVtuberService uploadVtuberService,
                                     com.VSong.service.UpdateVtuberService updateVtuberService,
                                     com.VSong.repository.SongUpdateLogRepository songUpdateLogRepository,
                                     com.VSong.repository.VtuberUpdateLogRepository vtuberUpdateLogRepository,
                                     HealthEndpoint healthEndpoint,
                                     java.util.concurrent.ThreadPoolExecutor vtuberSyncExecutor) {
        this.youtubeApiService = youtubeApiService;
        this.updateVtuberSongsService = updateVtuberSongsService;
        this.uploadVtuberService = uploadVtuberService;
        this.updateVtuberService = updateVtuberService;
        this.songUpdateLogRepository = songUpdateLogRepository;
        this.vtuberUpdateLogRepository = vtuberUpdateLogRepository;
        this.healthEndpoint = healthEndpoint;
        this.vtuberSyncExecutor = vtuberSyncExecutor;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        data.put("youtubeApi", youtubeApiService.getApiKeysStatus());

        data.put("songUpdateStats", updateVtuberSongsService.getLastOperationStats());
        data.put("songUpdateHistory", updateVtuberSongsService.getRecentLogs());

        data.put("vtuberUpdateStats", uploadVtuberService.getLastOperationStats());
        data.put("vtuberUpdateHistory", uploadVtuberService.getRecentLogs());

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
