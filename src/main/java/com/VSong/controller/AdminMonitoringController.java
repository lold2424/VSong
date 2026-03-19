package com.VSong.controller;

import com.VSong.service.UpdateVtuberSongsService;
import com.VSong.service.YouTubeApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final HealthEndpoint healthEndpoint;

    public AdminMonitoringController(YouTubeApiService youtubeApiService,
                                     UpdateVtuberSongsService updateVtuberSongsService,
                                     HealthEndpoint healthEndpoint) {
        this.youtubeApiService = youtubeApiService;
        this.updateVtuberSongsService = updateVtuberSongsService;
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        data.put("youtubeApi", youtubeApiService.getApiKeysStatus());

        data.put("songUpdateStats", updateVtuberSongsService.getLastOperationStats());

        Status healthStatus = healthEndpoint.health().getStatus();
        data.put("systemHealth", healthStatus.getCode());

        return data;
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
}
