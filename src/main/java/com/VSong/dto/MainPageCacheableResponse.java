package com.VSong.dto;

import com.VSong.entity.VtuberSongsEntity;
import java.util.List;

public record MainPageCacheableResponse(
    List<VtuberSongsEntity> top10WeeklySongs,
    List<VtuberSongsEntity> top10DailySongs,
    List<VtuberSongsEntity> top10RecentSongs
) {}
