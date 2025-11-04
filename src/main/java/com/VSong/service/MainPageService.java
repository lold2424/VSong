package com.VSong.service;

import com.VSong.dto.MainPageResponse;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;

@Service
public class MainPageService {

    private final SongService songService;
    private final SortSongsService sortSongsService;
    private final VtuberChannelService vtuberChannelService;

    public MainPageService(SongService songService, SortSongsService sortSongsService, VtuberChannelService vtuberChannelService) {
        this.songService = songService;
        this.sortSongsService = sortSongsService;
        this.vtuberChannelService = vtuberChannelService;
    }

    @Cacheable(value = "mainPage", key = "#gender")
    public MainPageResponse getMainPageData(String gender) {
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender(gender);

        List<VtuberSongsEntity> randomVideoSongs = songService.getRandomVideoSongs(9, channelIds);
        List<VtuberSongsEntity> top10WeeklySongs = sortSongsService.getTop10SongsByViewsIncreaseWeek(channelIds, "videos");
        List<VtuberSongsEntity> top10DailySongs = sortSongsService.getTop10SongsByViewsIncreaseDay(channelIds);
        List<VtuberSongsEntity> top10RecentSongs = sortSongsService.getTop9SongsByPublishedAt(channelIds, "videos");

        List<VtuberSongsEntity> randomShorts = songService.getRandomShortsSongs(9, channelIds);
        List<VtuberSongsEntity> top10WeeklyShorts = sortSongsService.getTop10ShortsByViewsIncreaseWeek(channelIds, "shorts");
        List<VtuberSongsEntity> top9RecentShorts = sortSongsService.getTop9ShortsByPublishedAt(channelIds, "shorts");

        return new MainPageResponse(randomVideoSongs, top10WeeklySongs, top10DailySongs, top10RecentSongs,
                randomShorts, top10WeeklyShorts, top9RecentShorts);
    }
}

