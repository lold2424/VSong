package com.VSong.repository;

import com.VSong.entity.SongProcessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SongProcessLogRepository extends JpaRepository<SongProcessLog, Long> {
    List<SongProcessLog> findByVideoIdOrderByProcessedAtDesc(String videoId);
    Page<SongProcessLog> findAllByOrderByProcessedAtDesc(Pageable pageable);
    Page<SongProcessLog> findByTitleContainingOrderByProcessedAtDesc(String title, Pageable pageable);
}
