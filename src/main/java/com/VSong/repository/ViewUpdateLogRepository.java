package com.VSong.repository;

import com.VSong.entity.ViewUpdateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViewUpdateLogRepository extends JpaRepository<ViewUpdateLog, Long> {

    @Modifying
    @Query("DELETE FROM ViewUpdateLog v WHERE v.runTime < :thresholdDate")
    void deleteOldLogs(@Param("thresholdDate") LocalDateTime thresholdDate);

    @Query(value = "SELECT * FROM view_update_logs ORDER BY run_time DESC LIMIT 1", nativeQuery = true)
    ViewUpdateLog findLatestLog();

    @Query(value = "SELECT * FROM view_update_logs ORDER BY run_time DESC LIMIT 10", nativeQuery = true)
    List<ViewUpdateLog> findRecentLogs();
}
