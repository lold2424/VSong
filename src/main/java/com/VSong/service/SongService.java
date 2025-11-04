package com.VSong.service;

import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SongService {

    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberRepository vtuberRepository;

    public SongService(VtuberSongsRepository vtuberSongsRepository, VtuberRepository vtuberRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.vtuberRepository = vtuberRepository;
    }

    public List<VtuberSongsEntity> getRandomVideoSongs(int limit, List<String> channelIds) {
        if (channelIds.isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return vtuberSongsRepository.findRandomSongsByChannelIdsAndClassification(channelIds, "videos", pageable);
    }

    public List<VtuberSongsEntity> getRandomShortsSongs(int limit, List<String> channelIds) {
        if (channelIds.isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return vtuberSongsRepository.findRandomSongsByChannelIdsAndClassification(channelIds, "shorts", pageable);
    }
}