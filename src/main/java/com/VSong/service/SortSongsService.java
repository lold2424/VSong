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

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseWeek(List<String> channelIds, String classification) {
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByViewsIncreaseWeekDescAndChannelIdsAndClassification(channelIds, classification);
    }

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseDay(List<String> channelIds) {
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByViewsIncreaseDayDescAndChannelIds(channelIds);
    }

    public List<VtuberSongsEntity> getTop10SongsByPublishedAt(List<String> channelIds, String classification) {
        if (channelIds.isEmpty()) {
            return List.of();
        }
        return vtuberSongsRepository.findTop10ByPublishedAtDescAndChannelIdsAndClassification(channelIds, classification);
    }
}
