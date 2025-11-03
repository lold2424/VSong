package com.VSong.controller;

import com.VSong.dto.SongResponseDto;
import com.VSong.service.DiscordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discord")
public class DiscordController {

    private final DiscordService discordService;

    public DiscordController(DiscordService discordService) {
        this.discordService = discordService;
    }

    @GetMapping("/new")
    public List<SongResponseDto> getNewSongs() {
        return discordService.getNewSongs();
    }

    @GetMapping("/popular/daily")
    public List<SongResponseDto> getDailyPopularSongs(
                                                       @RequestParam("channels") List<String> channelIds) {
        return discordService.getDailyPopularSongs(channelIds);
    }

    @GetMapping("/popular/weekly")
    public List<SongResponseDto> getWeeklyPopularSongs(
                                                        @RequestParam("channels") List<String> channelIds,
                                                        @RequestParam("classification") String classification) {
        return discordService.getWeeklyPopularSongs(channelIds, classification);
    }

    @GetMapping("/latest")
    public List<SongResponseDto> getLatestSongs(
                                                 @RequestParam("channels") List<String> channelIds,
                                                 @RequestParam("classification") String classification) {
        return discordService.getLatestSongs(channelIds, classification);
    }

    @GetMapping("/search")
    public List<SongResponseDto> searchSongsByTitle(
                                                     @RequestParam("title") String title,
                                                     @RequestParam("classification") String classification) {
        return discordService.searchSongsByTitle(title, classification);
    }

    @GetMapping("/artist")
    public List<SongResponseDto> getSongsByArtist(
                                                   @RequestParam("name") String vtuberName) {
        return discordService.getSongsByArtist(vtuberName);
    }

    @GetMapping("/random")
    public List<SongResponseDto> getRandomSongs(
                                                 @RequestParam("classification") String classification,
                                                 @RequestParam(value = "count", defaultValue = "1") int count) {
        return discordService.getRandomSongs(classification, count);
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<SongResponseDto> getSongDetails(
                                                           @PathVariable("videoId") String videoId) {

        return discordService.getSongDetails(videoId)
                .map(songDto -> ResponseEntity.ok(songDto))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}