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

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;

@Service
public class MainPageService {

    private final SongService songService;
    private final SortSongsService sortSongsService;
    private final VtuberChannelService vtuberChannelService;
    private final Executor taskExecutor;
    private final CacheManager cacheManager;

    public MainPageService(SongService songService, SortSongsService sortSongsService,
                           VtuberChannelService vtuberChannelService, @Qualifier("taskExecutor") Executor taskExecutor, CacheManager cacheManager) {
        this.songService = songService;
        this.sortSongsService = sortSongsService;
        this.vtuberChannelService = vtuberChannelService;
        this.taskExecutor = taskExecutor;
        this.cacheManager = cacheManager;
    }

    @CacheEvict(value = "mainPage", allEntries = true)
    public void refreshMainPageCache() {
    }

    @Cacheable(value = "mainPage", key = "#gender")
    public MainPageCacheableResponse getCacheableMainPageData(String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);

        CompletableFuture<List<VtuberSongsEntity>> top10WeeklySongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10SongsByViewsIncreaseWeek(channelIds), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10DailySongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10SongsByViewsIncreaseDay(channelIds), taskExecutor);
        CompletableFuture<List<VtuberSongsEntity>> top10RecentSongsFuture = CompletableFuture.supplyAsync(
                () -> sortSongsService.getTop10SongsByPublishedAt(channelIds), taskExecutor);

        CompletableFuture.allOf(
                top10WeeklySongsFuture, top10DailySongsFuture, top10RecentSongsFuture)
                .join();

        return new MainPageCacheableResponse(
                top10WeeklySongsFuture.join(),
                top10DailySongsFuture.join(),
                top10RecentSongsFuture.join()
        );
    }

    public MainPageRandomResponse getRandomMainPageData(String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);

        CompletableFuture<List<VtuberSongsEntity>> randomSongsFuture = CompletableFuture.supplyAsync(
                () -> songService.getRandomVideoSongs(10, channelIds), taskExecutor);

        randomSongsFuture.join();

        List<VtuberSongsEntity> randomSongs = randomSongsFuture.join();

        return new MainPageRandomResponse(
                randomSongs != null ? randomSongs : List.of()
        );
    }
}
