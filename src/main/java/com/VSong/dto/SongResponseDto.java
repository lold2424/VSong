package com.VSong.dto;

import com.VSong.entity.VtuberSongsEntity;

public record SongResponseDto(
        String videoId,
        String title,
        String vtuberName,
        long viewCount,
        long viewsIncreaseDay,
        long viewsIncreaseWeek
) {
    public static SongResponseDto fromEntity(VtuberSongsEntity entity) {
        return new SongResponseDto(
                entity.getVideoId(),
                entity.getTitle(),
                entity.getVtuberName(),
                entity.getViewCount(),
                entity.getViewsIncreaseDay(),
                entity.getViewsIncreaseWeek()
        );
    }
}