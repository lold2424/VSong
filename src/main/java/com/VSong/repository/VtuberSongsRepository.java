package com.VSong.repository;

import com.VSong.entity.VtuberSongsEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VtuberSongsRepository extends JpaRepository<VtuberSongsEntity, Long> {
    Optional<VtuberSongsEntity> findByVideoId(String videoId);
    boolean existsByVideoId(String videoId);

    List<VtuberSongsEntity> findByStatus(String status);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = :classification ORDER BY RAND()", nativeQuery = true)
    List<VtuberSongsEntity> findRandomSongsByClassification(@Param("classification") String classification, Pageable pageable);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = :classification AND channel_id IN (:channelIds) ORDER BY RAND()", nativeQuery = true)
    List<VtuberSongsEntity> findRandomSongsByChannelIdsAndClassification(@Param("channelIds") List<String> channelIds, @Param("classification") String classification, Pageable pageable);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'videos' AND channel_id IN (:channelIds) ORDER BY views_increase_day DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByViewsIncreaseDayDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = :classification AND channel_id IN (:channelIds) ORDER BY views_increase_week DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByViewsIncreaseWeekDescAndChannelIdsAndClassification(@Param("channelIds") List<String> channelIds, @Param("classification") String classification);

    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = :classification AND channel_id IN (:channelIds) ORDER BY published_at DESC LIMIT 9", nativeQuery = true)
    List<VtuberSongsEntity> findTop9ByPublishedAtDescAndChannelIdsAndClassification(@Param("channelIds") List<String> channelIds, @Param("classification") String classification);

    // LIKE 절 수정: CONCAT 함수 사용
    @Query(value = "SELECT * FROM vtuber_songs WHERE title LIKE CONCAT('%', :title, '%') AND classification = :classification ORDER BY view_count DESC", nativeQuery = true)
    List<VtuberSongsEntity> findAllByTitleContainingAndClassificationOrderByViewCountDesc(@Param("title") String title, @Param("classification") String classification);

    @Query(value = "SELECT * FROM vtuber_songs WHERE vtuber_name = :vtuberName ORDER BY view_count DESC", nativeQuery = true)
    List<VtuberSongsEntity> findAllByVtuberNameOrderByViewCountDesc(@Param("vtuberName") String vtuberName);

    @Query("SELECT COUNT(v) FROM VtuberSongsEntity v WHERE v.channelId = :channelId")
    int countByChannelId(@Param("channelId") String channelId);

    List<VtuberSongsEntity> findByChannelId(String channelId);

    @Modifying
    @Query("DELETE FROM VtuberSongsEntity v WHERE v.channelId = :channelId")
    void deleteByChannelId(@Param("channelId") String channelId);
}
