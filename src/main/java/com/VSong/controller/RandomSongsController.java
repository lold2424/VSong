package com.VSong.controller;

import com.VSong.entity.VtuberSongsEntity;
import com.VSong.service.SongService;
import com.VSong.service.VtuberChannelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class RandomSongsController {

    private final SongService songService;
    private final VtuberChannelService vtuberChannelService;

    public RandomSongsController(SongService songService, VtuberChannelService vtuberChannelService) {
        this.songService = songService;
        this.vtuberChannelService = vtuberChannelService;
    }

    @GetMapping("/random-videos")
    public List<VtuberSongsEntity> getRandomVideoSongs(
            @RequestParam(defaultValue = "9") int limit,
            @RequestParam(defaultValue = "all") String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);
        return songService.getRandomVideoSongs(limit, channelIds);
    }
}
