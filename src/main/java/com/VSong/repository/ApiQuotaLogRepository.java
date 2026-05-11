package com.VSong.repository;

import com.VSong.entity.ApiQuotaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ApiQuotaLogRepository extends JpaRepository<ApiQuotaLog, Long> {

    List<ApiQuotaLog> findTop100ByOrderByRequestTimeDesc();

    @Query("SELECT l.apiMethod as method, SUM(l.cost) as totalCost FROM ApiQuotaLog l " +
           "WHERE l.requestTime >= :since GROUP BY l.apiMethod")
    List<Map<String, Object>> getUsageStatsByMethod(LocalDateTime since);

    void deleteByRequestTimeBefore(LocalDateTime expiryDate);
}
