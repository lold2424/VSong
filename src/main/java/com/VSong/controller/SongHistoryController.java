package com.VSong.controller;

import com.VSong.dto.SongViewHistoryDto;
import com.VSong.entity.SongViewHistory;
import com.VSong.repository.SongViewHistoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/songs")
public class SongHistoryController {

    private final SongViewHistoryRepository songViewHistoryRepository;

    public SongHistoryController(SongViewHistoryRepository songViewHistoryRepository) {
        this.songViewHistoryRepository = songViewHistoryRepository;
    }

    @GetMapping("/{videoId}/history")
    public List<SongViewHistoryDto> getSongHistory(@PathVariable String videoId) {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(8);
        
        return songViewHistoryRepository.findByVideoIdAndRecordDateAfterOrderByRecordDateAsc(videoId, sevenDaysAgo)
                .stream()
                .map(h -> new SongViewHistoryDto(h.getRecordDate(), h.getViewCount()))
                .collect(Collectors.toList());
    }
}
