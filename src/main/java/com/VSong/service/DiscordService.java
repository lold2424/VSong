package com.VSong.service;

import com.VSong.dto.SongResponseDto;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberSongsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DiscordService {

    private final VtuberSongsRepository vtuberSongsRepository;

    public DiscordService(VtuberSongsRepository vtuberSongsRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
    }

    public List<SongResponseDto> getNewSongs() {
        return vtuberSongsRepository.findByStatus("new").stream()
                .map(SongResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SongResponseDto> getDailyPopularSongs(List<String> channelIds) {
        List<VtuberSongsEntity> entities = vtuberSongsRepository.findTop10ByViewsIncreaseDayDescAndChannelIds(channelIds);
        return entities.stream()
                .map(SongResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SongResponseDto> getWeeklyPopularSongs(List<String> channelIds, String classification) {
        return vtuberSongsRepository.findTop10ByViewsIncreaseWeekDescAndChannelIdsAndClassification(channelIds, classification).stream()
                .map(SongResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SongResponseDto> getLatestSongs(List<String> channelIds, String classification) {
        return vtuberSongsRepository.findTop10ByPublishedAtDescAndChannelIdsAndClassification(channelIds, classification).stream()
                .map(SongResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SongResponseDto> searchSongsByTitle(String title, String classification) {
        return vtuberSongsRepository.findAllByTitleContainingAndClassificationOrderByViewCountDesc(title, classification).stream()
                .map(SongResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SongResponseDto> getSongsByArtist(String vtuberName) {
        return vtuberSongsRepository.findAllByVtuberNameOrderByViewCountDesc(vtuberName).stream()
                .map(SongResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SongResponseDto> getRandomSongs(String classification, int count) {
        Pageable pageable = PageRequest.of(0, count);
        return vtuberSongsRepository.findRandomSongsByClassification(classification, pageable).stream()
                .map(SongResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<SongResponseDto> getSongDetails(String videoId) {
        return vtuberSongsRepository.findByVideoId(videoId)
                .map(SongResponseDto::fromEntity);
    }
}