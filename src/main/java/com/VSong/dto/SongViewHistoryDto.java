package com.VSong.dto;

import java.time.LocalDate;

public record SongViewHistoryDto(LocalDate recordDate, long viewCount) {
}
