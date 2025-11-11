package com.VSong.controller;

import com.VSong.dto.MainPageCacheableResponse;
import com.VSong.dto.MainPageRandomResponse;
import com.VSong.dto.MainPageResponse;
import com.VSong.service.MainPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/main")
public class MainController {

    private final MainPageService mainPageService;

    public MainController(MainPageService mainPageService) {
        this.mainPageService = mainPageService;
    }

    @GetMapping
    public MainPageResponse getMainPage(
            @RequestParam(value = "gender", required = false, defaultValue = "all") String gender) {
        
        MainPageCacheableResponse cacheableData = mainPageService.getCacheableMainPageData(gender);
        MainPageRandomResponse randomData = mainPageService.getRandomMainPageData(gender);

        return new MainPageResponse(
                randomData.randomVideoSongs(),
                cacheableData.top10WeeklySongs(),
                cacheableData.top10DailySongs(),
                cacheableData.top10RecentSongs(),
                randomData.randomShorts(),
                cacheableData.top10WeeklyShorts(),
                cacheableData.top9RecentShorts()
        );
    }
}
