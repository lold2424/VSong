package com.VSong.repository;

import com.VSong.entity.VtuberUpdateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VtuberUpdateLogRepository extends JpaRepository<VtuberUpdateLog, Long> {
    Optional<VtuberUpdateLog> findFirstByOrderByRunTimeDesc();
    java.util.List<VtuberUpdateLog> findTop10ByOrderByRunTimeDesc();
}
