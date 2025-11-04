package com.VSong.controller;

import com.VSong.dto.MainPageResponse;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.service.MainPageService;
import com.VSong.service.SongService;
import com.VSong.service.SortSongsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        return mainPageService.getMainPageData(gender);
    }
}
