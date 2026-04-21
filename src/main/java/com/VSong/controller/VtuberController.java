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
            return ResponseEntity.badRequest().body(Map.of("error", "REQUIRED_PARAM", "message", "검색어나 채널 ID를 입력해주세요."));
        }

        if (query != null && !query.trim().isEmpty()) {
            String trimmedQuery = query.trim();
            if (trimmedQuery.length() < 2) {
                return ResponseEntity.badRequest().body(Map.of("error", "QUERY_TOO_SHORT", "message", "검색어는 최소 2자 이상 입력해주세요."));
            }
            if (trimmedQuery.length() > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "QUERY_TOO_LONG", "message", "검색어는 100자를 초과할 수 없습니다."));
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