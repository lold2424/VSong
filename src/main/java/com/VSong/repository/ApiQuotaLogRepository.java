package com.VSong.repository;

import com.VSong.entity.ApiQuotaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ApiQuotaLogRepository extends JpaRepository<ApiQuotaLog, Long> {

    List<ApiQuotaLog> findTop100ByOrderByRequestTimeDesc();

    @Query("SELECT l.apiMethod as method, SUM(l.cost) as totalCost FROM ApiQuotaLog l " +
           "WHERE l.requestTime >= :since GROUP BY l.apiMethod")
    List<Map<String, Object>> getUsageStatsByMethod(@Param("since") LocalDateTime since);

    @Query("SELECT SUM(l.cost) FROM ApiQuotaLog l WHERE l.requestTime BETWEEN :start AND :end")
    Integer sumCostByRequestTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(l.cost) FROM ApiQuotaLog l WHERE l.apiKeyPrefix = :prefix AND l.requestTime >= :since")
    Integer sumCostByKeyPrefixAndRequestTimeAfter(@Param("prefix") String prefix, @Param("since") LocalDateTime since);

    void deleteByRequestTimeBefore(LocalDateTime expiryDate);
}
