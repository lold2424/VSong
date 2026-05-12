package com.VSong.repository;

import com.VSong.entity.SongViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface SongViewHistoryRepository extends JpaRepository<SongViewHistory, Long> {
    Optional<SongViewHistory> findByVideoIdAndRecordDate(String videoId, LocalDate recordDate);
    
    List<SongViewHistory> findByVideoIdAndRecordDateAfterOrderByRecordDateAsc(String videoId, LocalDate date);

    @Modifying
    @Transactional
    @Query("DELETE FROM SongViewHistory s WHERE s.recordDate < :date")
    void deleteByRecordDateBefore(@Param("date") LocalDate date);

    List<SongViewHistory> findByRecordDate(LocalDate recordDate);
}
