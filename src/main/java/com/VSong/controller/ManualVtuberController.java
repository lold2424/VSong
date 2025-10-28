package com.VSong.controller;

import com.VSong.service.ManualVtuberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vtuber")
public class ManualVtuberController {

    private final ManualVtuberService manualVtuberService;

    public ManualVtuberController(ManualVtuberService manualVtuberService) {
        this.manualVtuberService = manualVtuberService;
    }

    @PostMapping("/manual-add")
    public ResponseEntity<String> addVtuberManually(@RequestParam String channelId) {
        String result = manualVtuberService.addVtuberChannel(channelId);
        if (result.startsWith("버튜버 채널이 성공적으로 추가되었습니다")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
