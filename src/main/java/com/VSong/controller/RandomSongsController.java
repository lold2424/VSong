package com.VSong.controller;

import com.VSong.entity.VtuberSongsEntity;
import com.VSong.service.SongService;
import com.VSong.service.VtuberChannelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.VSong.repository.VtuberRepository;

@RestController
@RequestMapping("/api/v1/random-songs")
public class RandomSongsController {

    private final SongService songService;
    private final VtuberChannelService vtuberChannelService;

    public RandomSongsController(SongService songService, VtuberChannelService vtuberChannelService) {
        this.songService = songService;
        this.vtuberChannelService = vtuberChannelService;
    }

    @GetMapping("/random")
    public List<VtuberSongsEntity> getRandomVideoSongs(
            @RequestParam(value = "limit", defaultValue = "6") int limit,
            @RequestParam(value = "gender", required = false, defaultValue = "all") String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);
        return songService.getRandomVideoSongs(limit, channelIds);
    }

    @GetMapping("/random-shorts")
    public List<VtuberSongsEntity> getRandomShortsSongs(
            @RequestParam(value = "limit", defaultValue = "6") int limit,
            @RequestParam(value = "gender", required = false, defaultValue = "all") String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);
        return songService.getRandomShortsSongs(limit, channelIds);
    }
}
