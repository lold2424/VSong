package com.VSong.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.VSong.service.PlayboardVtuberService;

@RestController
@RequestMapping("/admin")
public class PlayboardController {
    
    @Autowired
    private PlayboardVtuberService playboardService;
    
    @PostMapping("/import-playboard")
    public ResponseEntity<String> importFromPlayboard() {
        try {
            playboardService.importAndEnrichVtubers();
            return ResponseEntity.ok("Playboard에서 VTuber 목록 가져오기 완료");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("오류: " + e.getMessage());
        }
    }
}