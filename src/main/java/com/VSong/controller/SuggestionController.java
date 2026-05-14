package com.VSong.controller;

import com.VSong.entity.SuggestionEntity;
import com.VSong.service.SuggestionService;
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
    public ResponseEntity<?> createSuggestion(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        String userEmail = payload.get("userEmail");
        String userName = payload.get("userName");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("내용을 입력해주세요.");
        }

        suggestionService.saveSuggestion(content, userEmail, userName);
        return ResponseEntity.ok().body(Map.of("message", "건의사항이 성공적으로 접수되었습니다."));
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SuggestionEntity>> getAdminSuggestions() {
        return ResponseEntity.ok(suggestionService.getAllSuggestions());
    }
}
