package com.VSong.repository;

import com.VSong.entity.SongUpdateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface SongUpdateLogRepository extends JpaRepository<SongUpdateLog, Long> {
    
    @Modifying
    @Query("DELETE FROM SongUpdateLog s WHERE s.runTime < :thresholdDate")
    void deleteOldLogs(@Param("thresholdDate") LocalDateTime thresholdDate);

    @Query(value = "SELECT * FROM song_update_logs ORDER BY run_time DESC LIMIT 1", nativeQuery = true)
    SongUpdateLog findLatestLog();
}
