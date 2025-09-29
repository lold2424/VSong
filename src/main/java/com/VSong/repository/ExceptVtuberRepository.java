package com.VSong.repository;

import com.VSong.entity.ExceptVtuberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptVtuberRepository extends JpaRepository<ExceptVtuberEntity, String> {

    @Query("SELECT e.channelId FROM ExceptVtuberEntity e")
    List<String> findAllChannelIds();
    boolean existsByChannelId(String channelId);
}
