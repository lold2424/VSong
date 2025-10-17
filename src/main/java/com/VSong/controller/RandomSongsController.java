package com.VSong.controller;

import com.VSong.entity.VtuberSongsEntity;
import com.VSong.service.SongService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/random-songs")
public class RandomSongsController {

    private final SongService songService;

    public RandomSongsController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping("/random")
    public List<VtuberSongsEntity> getRandomVideoSongs(
            @RequestParam(value = "limit", defaultValue = "6") int limit,
            @RequestParam(value = "gender", required = false, defaultValue = "all") String gender) {
        return songService.getRandomVideoSongs(limit, gender);
    }

    @GetMapping("/random-shorts")
    public List<VtuberSongsEntity> getRandomShortsSongs(
            @RequestParam(value = "limit", defaultValue = "6") int limit,
            @RequestParam(value = "gender", required = false, defaultValue = "all") String gender) {
        return songService.getRandomShortsSongs(limit, gender);
    }
}
