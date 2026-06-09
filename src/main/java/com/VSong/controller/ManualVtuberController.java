package com.VSong.controller;

import com.VSong.dto.AddVtuberRequest;
import com.VSong.service.ManualVtuberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class ManualVtuberController {

    private final ManualVtuberService manualVtuberService;

    public ManualVtuberController(ManualVtuberService manualVtuberService) {
        this.manualVtuberService = manualVtuberService;
    }

    @PostMapping("/vtuber")
    public ResponseEntity<String> addVtuber(@RequestBody AddVtuberRequest request) {
        if (request == null || request.getChannelId() == null || request.getChannelId().isBlank()) {
            return ResponseEntity.badRequest().body("채널 ID, 유튜브 핸들, 또는 URL을 입력해주세요.");
        }
        String result = manualVtuberService.addVtuberChannel(request.getChannelId(), request.getGender());
        if (result.startsWith("버튜버 채널이 성공적으로 추가되었습니다")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}