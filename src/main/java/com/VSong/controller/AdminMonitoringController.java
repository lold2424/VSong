package com.VSong.controller;

import com.VSong.service.UpdateVtuberSongsService;
import com.VSong.service.YouTubeApiService;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitoring")
public class AdminMonitoringController {

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
}
