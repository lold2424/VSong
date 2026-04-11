package com.VSong.controller;

import com.VSong.service.MainPageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cache")
public class CacheManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CacheManagementController.class);
    private final MainPageService mainPageService;

    public CacheManagementController(MainPageService mainPageService) {
        this.mainPageService = mainPageService;
    }

    @PostMapping("/refresh-main-page")
    public ResponseEntity<String> refreshMainPageCache() {
        logger.info("Manual cache refresh request received for main page by an admin.");
        try {
            mainPageService.refreshMainPageCache();
            logger.info("Manual cache refresh job executed successfully.");
            return ResponseEntity.ok("메인 페이지 캐시 갱신 작업이 성공적으로 실행되었습니다.");
        } catch (Exception e) {
            logger.error("Error occurred during manual cache refresh", e);
            return ResponseEntity.internalServerError().body("캐시 갱신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
