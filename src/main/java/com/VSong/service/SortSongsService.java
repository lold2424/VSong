package com.VSong.service;

import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberSongsRepository;
import com.VSong.repository.VtuberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SortSongsService {

    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberRepository vtuberRepository;

    public SortSongsService(VtuberSongsRepository vtuberSongsRepository, VtuberRepository vtuberRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.vtuberRepository = vtuberRepository;
    }

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseWeek(String gender) {
        System.out.println("getTop10SongsByViewsIncreaseWeek called with gender: " + gender);
        List<String> channelIds = getChannelIdsByGender(gender);
        System.out.println("channelIds: " + channelIds);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByViewsIncreaseWeekDescAndChannelIdsAndClassification(channelIds, "videos");
    }

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseDay(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByViewsIncreaseDayDescAndChannelIds(channelIds);
    }

    public List<VtuberSongsEntity> getTop9SongsByPublishedAt(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop9ByPublishedAtDescAndChannelIdsAndClassification(channelIds, "videos");
    }

    public List<VtuberSongsEntity> getTop10ShortsByViewsIncreaseWeek(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByViewsIncreaseWeekDescAndChannelIdsAndClassification(channelIds, "shorts");
    }

    public List<VtuberSongsEntity> getTop9ShortsByPublishedAt(String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop9ByPublishedAtDescAndChannelIdsAndClassification(channelIds, "shorts");
    }

    private List<String> getChannelIdsByGender(String gender) {
        if (gender == null || gender.equalsIgnoreCase("all")) {
            return vtuberRepository.findAllChannelIds();
        } else if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("female")) {
            return vtuberRepository.findChannelIdsByGender(gender.toLowerCase());
        } else if (gender.equalsIgnoreCase("mixed")) { // null 값의 경우 'mixed'로 간주
            return vtuberRepository.findChannelIdsWithNullGender();
        } else {
            throw new IllegalArgumentException("Invalid gender parameter: " + gender);
        }
    }
}
