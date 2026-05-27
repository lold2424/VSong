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
    Page<SongProcessLog> findByDecisionOrderByProcessedAtDesc(String decision, Pageable pageable);
    Page<SongProcessLog> findByDecisionAndTitleContainingOrderByProcessedAtDesc(String decision, String title, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE SongProcessLog s SET s.decision = 'DELETED' WHERE s.videoId = :videoId AND s.decision = 'ACCEPTED'")
    void markAsDeleted(@org.springframework.data.repository.query.Param("videoId") String videoId);
}
