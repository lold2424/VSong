package com.VSong.controller;

import com.VSong.dto.VtuberRequest;
import com.VSong.entity.VtuberEntity;
import com.VSong.service.VtuberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/vtubers")
public class AdminVtuberController {

    private final VtuberService vtuberService;
    private final com.VSong.service.UpdateVtuberSongsService updateVtuberSongsService;
    private final com.VSong.repository.VtuberSongsRepository vtuberSongsRepository;
    private final com.VSong.repository.SongViewHistoryRepository songViewHistoryRepository;
    private final com.VSong.repository.SongProcessLogRepository songProcessLogRepository;

    public AdminVtuberController(VtuberService vtuberService, 
                                com.VSong.service.UpdateVtuberSongsService updateVtuberSongsService,
                                com.VSong.repository.VtuberSongsRepository vtuberSongsRepository,
                                com.VSong.repository.SongViewHistoryRepository songViewHistoryRepository,
                                com.VSong.repository.SongProcessLogRepository songProcessLogRepository) {
        this.vtuberService = vtuberService;
        this.updateVtuberSongsService = updateVtuberSongsService;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.songViewHistoryRepository = songViewHistoryRepository;
        this.songProcessLogRepository = songProcessLogRepository;
    }

    @PostMapping
    public ResponseEntity<VtuberEntity> createVtuber(@RequestBody VtuberRequest vtuberRequest) {
        VtuberEntity vtuberEntity = vtuberService.createVtuber(vtuberRequest.getDescription(), vtuberRequest.getGender());
        return new ResponseEntity<>(vtuberEntity, HttpStatus.CREATED);
    }

    @PostMapping("/clean-titles")
    public ResponseEntity<String> cleanAllTitles() {
        updateVtuberSongsService.cleanAllExistingTitles();
        return ResponseEntity.ok("제목 정제 작업이 백그라운드에서 시작되었습니다. 로그를 통해 진행 상황을 확인해 주세요.");
    }

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.web.bind.annotation.DeleteMapping("/songs/{videoId}")
    public ResponseEntity<String> deleteSong(@org.springframework.web.bind.annotation.PathVariable String videoId) {
        songViewHistoryRepository.deleteByVideoId(videoId);

        vtuberSongsRepository.deleteByVideoId(videoId);

        songProcessLogRepository.markAsDeleted(videoId);
        
        return ResponseEntity.ok("노래와 관련된 모든 데이터가 성공적으로 삭제되었습니다.");
    }
}
