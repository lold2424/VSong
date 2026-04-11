package com.VSong.repository;

import com.VSong.entity.ServerStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ServerStatusLogRepository extends JpaRepository<ServerStatusLog, Long> {
    
    @Modifying
    @Query("DELETE FROM ServerStatusLog s WHERE s.checkTime < :thresholdDate")
    void deleteOldLogs(@Param("thresholdDate") LocalDateTime thresholdDate);

    Optional<ServerStatusLog> findTopByEnvironmentOrderByCheckTimeDesc(String environment);
}
