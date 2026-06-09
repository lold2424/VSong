package com.VSong.repository;

import com.VSong.entity.VtuberProcessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VtuberProcessLogRepository extends JpaRepository<VtuberProcessLog, Long> {
    Page<VtuberProcessLog> findAllByOrderByProcessedAtDesc(Pageable pageable);
    
    Page<VtuberProcessLog> findByChannelIdOrderByProcessedAtDesc(String channelId, Pageable pageable);
    
    Page<VtuberProcessLog> findByChannelTitleContainingOrderByProcessedAtDesc(String channelTitle, Pageable pageable);
    
    Page<VtuberProcessLog> findByDecisionOrderByProcessedAtDesc(String decision, Pageable pageable);
    
    Page<VtuberProcessLog> findByDecisionAndChannelTitleContainingOrderByProcessedAtDesc(String decision, String channelTitle, Pageable pageable);
}
