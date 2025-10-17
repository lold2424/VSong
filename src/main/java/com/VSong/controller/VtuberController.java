package com.VSong.controller;

import com.VSong.dto.VtuberRequest;
import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.service.VtuberService;
import org.springframework.http.HttpStatus;
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


    @PostMapping
    public ResponseEntity<VtuberEntity> createVtuber(@RequestBody VtuberRequest vtuberRequest) {
        VtuberEntity vtuberEntity = vtuberService.createVtuber(vtuberRequest.getDescription(), vtuberRequest.getGender());
        return new ResponseEntity<>(vtuberEntity, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVtubersAndSongs(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "channelId", required = false) String channelId) {

        if ((query == null || query.trim().isEmpty()) && (channelId == null || channelId.trim().isEmpty())) {
            return ResponseEntity.badRequest().build();
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