package com.VSong.repository;

import com.VSong.entity.SongViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface SongViewHistoryRepository extends JpaRepository<SongViewHistory, Long> {
    Optional<SongViewHistory> findByVideoIdAndRecordDate(String videoId, LocalDate recordDate);
    
    void deleteByRecordDateBefore(LocalDate date);

    List<SongViewHistory> findByRecordDate(LocalDate recordDate);
}
