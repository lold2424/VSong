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

    public List<VtuberSongsEntity> getRandomVideoSongs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<VtuberSongsEntity> randomSongs = vtuberSongsRepository.findRandomSongsByClassification("videos", pageable);
        System.out.println("Fetched random songs: " + randomSongs.size());
        return randomSongs;
    }

    public List<VtuberSongsEntity> getRandomShortsSongs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return vtuberSongsRepository.findRandomSongsByClassification("shorts", pageable);
    }

    // gender 필터링 적용된 메서드 추가
    public List<VtuberSongsEntity> getRandomVideoSongs(int limit, String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return vtuberSongsRepository.findRandomSongsByChannelIdsAndClassification(channelIds, "videos", pageable);
    }

    public List<VtuberSongsEntity> getRandomShortsSongs(int limit, String gender) {
        List<String> channelIds = getChannelIdsByGender(gender);
        if (channelIds.isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return vtuberSongsRepository.findRandomSongsByChannelIdsAndClassification(channelIds, "shorts", pageable);
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