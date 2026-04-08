package com.VSong.repository;

import com.VSong.entity.AiRecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiRecommendationLogRepository extends JpaRepository<AiRecommendationLog, Long> {
    List<AiRecommendationLog> findTop100ByOrderByRequestedAtDesc();
    long countBySuccess(boolean success);
    long countByRequestedAtAfter(LocalDateTime dateTime);
}
