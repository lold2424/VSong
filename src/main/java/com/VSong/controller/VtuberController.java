package com.VSong.controller;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.service.VtuberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vtubers")
public class VtuberController {

    private final VtuberService vtuberService;

    public VtuberController(VtuberService vtuberService) {
        this.vtuberService = vtuberService;
    }

    @GetMapping
    public ResponseEntity<List<VtuberEntity>> getAllVtubers() {
        List<VtuberEntity> vtubers = vtuberService.getAllVtubers();
        return ResponseEntity.ok(vtubers);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVtubersAndSongs(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "channelId", required = false) String channelId) {

        if ((query == null || query.trim().isEmpty()) && (channelId == null || channelId.trim().isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        if (query != null && !query.trim().isEmpty()) {
            if (query.length() > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query parameter exceeds maximum length of 100 characters."));
            }
        }

        Map<String, Object> searchResults = vtuberService.searchVtubersAndSongs(query, channelId);
        return ResponseEntity.ok(searchResults);
    }

    @GetMapping("/{channelId}/details")
    public ResponseEntity<Map<String, Object>> getVtuberDetails(@PathVariable String channelId) {
        Map<String, Object> details = vtuberService.getVtuberDetails(channelId);
        if (details.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

    @GetMapping("/{channelId}/songs")
    public ResponseEntity<List<VtuberSongsEntity>> getSongsByChannelId(@PathVariable String channelId) {
        List<VtuberSongsEntity> songs = vtuberService.getSongsByChannelId(channelId);
        return ResponseEntity.ok(songs);
    }
}