package com.VSong.service;

import com.VSong.dto.MainPageCacheableResponse;
import com.VSong.dto.MainPageRandomResponse;
import com.VSong.entity.VtuberSongsEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
    public MainPageCacheableResponse getCacheableMainPageData(String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);

        CompletableFuture<List<VtuberSongsEntity>> top10WeeklySongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10SongsByViewsIncreaseWeek(channelIds, "videos"), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10DailySongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10SongsByViewsIncreaseDay(channelIds), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10RecentSongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop9SongsByPublishedAt(channelIds, "videos"), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10WeeklyShortsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10ShortsByViewsIncreaseWeek(channelIds, "shorts"), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top9RecentShortsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop9ShortsByPublishedAt(channelIds, "shorts"), taskExecutor);

        CompletableFuture.allOf(
                top10WeeklySongsFuture, top10DailySongsFuture, top10RecentSongsFuture,
                top10WeeklyShortsFuture, top9RecentShortsFuture)
                .join();

        return new MainPageCacheableResponse(
                top10WeeklySongsFuture.join(),
                top10DailySongsFuture.join(),
                top10RecentSongsFuture.join(),
                top10WeeklyShortsFuture.join(),
                top9RecentShortsFuture.join()
        );
    }

    public MainPageRandomResponse getRandomMainPageData(String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);

        CompletableFuture<List<VtuberSongsEntity>> randomVideoSongsFuture = CompletableFuture.supplyAsync(
                () -> songService.getRandomVideoSongs(9, channelIds), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> randomShortsFuture = CompletableFuture.supplyAsync(
                () -> songService.getRandomShortsSongs(9, channelIds), taskExecutor);

        CompletableFuture.allOf(randomVideoSongsFuture, randomShortsFuture).join();

        return new MainPageRandomResponse(
                randomVideoSongsFuture.join(),
                randomShortsFuture.join()
        );
    }
}

