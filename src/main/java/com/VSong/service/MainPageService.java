package com.VSong.service;

import com.VSong.dto.MainPageResponse;
import com.VSong.entity.VtuberSongsEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.cache.annotation.Cacheable;

@Service
public class MainPageService {

    private final SongService songService;
    private final SortSongsService sortSongsService;
    private final VtuberChannelService vtuberChannelService;
    private final Executor taskExecutor;

    public MainPageService(SongService songService, SortSongsService sortSongsService,
                           VtuberChannelService vtuberChannelService, @Qualifier("taskExecutor") Executor taskExecutor) {
        this.songService = songService;
        this.sortSongsService = sortSongsService;
        this.vtuberChannelService = vtuberChannelService;
        this.taskExecutor = taskExecutor;
    }

    @Cacheable(value = "mainPage", key = "#gender")
    public MainPageResponse getMainPageData(String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);

        CompletableFuture<List<VtuberSongsEntity>> randomVideoSongsFuture = CompletableFuture.supplyAsync(
                () -> songService.getRandomVideoSongs(9, channelIds), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10WeeklySongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10SongsByViewsIncreaseWeek(channelIds, "videos"), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10DailySongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10SongsByViewsIncreaseDay(channelIds), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10RecentSongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop9SongsByPublishedAt(channelIds, "videos"), taskExecutor);

        CompletableFuture<List<VtuberSongsEntity>> randomShortsFuture = CompletableFuture.supplyAsync(
                () -> songService.getRandomShortsSongs(9, channelIds), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10WeeklyShortsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10ShortsByViewsIncreaseWeek(channelIds, "shorts"), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top9RecentShortsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop9ShortsByPublishedAt(channelIds, "shorts"), taskExecutor);

        CompletableFuture.allOf(
                randomVideoSongsFuture, top10WeeklySongsFuture, top10DailySongsFuture, top10RecentSongsFuture,
                randomShortsFuture, top10WeeklyShortsFuture, top9RecentShortsFuture)
                .join();

        List<VtuberSongsEntity> randomVideoSongs = randomVideoSongsFuture.join();
        List<VtuberSongsEntity> top10WeeklySongs = top10WeeklySongsFuture.join();
        List<VtuberSongsEntity> top10DailySongs = top10DailySongsFuture.join();
        List<VtuberSongsEntity> top10RecentSongs = top10RecentSongsFuture.join();

        List<VtuberSongsEntity> randomShorts = randomShortsFuture.join();
        List<VtuberSongsEntity> top10WeeklyShorts = top10WeeklyShortsFuture.join();
        List<VtuberSongsEntity> top9RecentShorts = top9RecentShortsFuture.join();

        return new MainPageResponse(randomVideoSongs, top10WeeklySongs, top10DailySongs, top10RecentSongs,
                randomShorts, top10WeeklyShorts, top9RecentShorts);
    }
}

