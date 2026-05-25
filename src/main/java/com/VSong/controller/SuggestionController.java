package com.VSong.controller;

import com.VSong.entity.SuggestionEntity;
import com.VSong.service.SuggestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping
    public ResponseEntity<?> createSuggestion(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String content = payload.get("content");
        String userEmail = payload.get("userEmail");
        String userName = payload.get("userName");

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        try {
            suggestionService.saveSuggestion(content, userEmail, userName, ipAddress);
            return ResponseEntity.ok().body(Map.of("message", "건의사항이 성공적으로 접수되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SuggestionEntity>> getAdminSuggestions() {
        return ResponseEntity.ok(suggestionService.getAllSuggestions());
    }
}
