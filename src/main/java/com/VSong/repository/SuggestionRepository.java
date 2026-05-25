package com.VSong.repository;

import com.VSong.entity.SuggestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SuggestionRepository extends JpaRepository<SuggestionEntity, Long> {
    List<SuggestionEntity> findAllByOrderByCreatedAtDesc();
    long countByUserEmailAndCreatedAtAfter(String userEmail, LocalDateTime dateTime);
    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime dateTime);
}
