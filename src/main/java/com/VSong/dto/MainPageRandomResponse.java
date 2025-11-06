package com.VSong.dto;

import com.VSong.entity.VtuberSongsEntity;
import java.util.List;

public record MainPageRandomResponse(
    List<VtuberSongsEntity> randomVideoSongs,
    List<VtuberSongsEntity> randomShorts
) {}