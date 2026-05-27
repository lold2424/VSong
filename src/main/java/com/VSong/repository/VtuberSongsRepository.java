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

    @Query(value = "SELECT * FROM vtuber_songs WHERE (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY RAND()", nativeQuery = true)
    List<VtuberSongsEntity> findRandomSongs(Pageable pageable);

    @Query(value = "SELECT * FROM vtuber_songs WHERE channel_id IN (:channelIds) AND (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY RAND()", nativeQuery = true)
    List<VtuberSongsEntity> findRandomSongsByChannelIds(@Param("channelIds") List<String> channelIds, Pageable pageable);

    @Query(value = "SELECT * FROM vtuber_songs WHERE channel_id IN (:channelIds) AND (status = 'existing' OR (status = 'new' AND TIMESTAMPDIFF(HOUR, published_at, added_time) <= 24)) AND (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY views_increase_day DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByViewsIncreaseDayDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE channel_id IN (:channelIds) AND (status = 'existing' OR (status = 'new' AND TIMESTAMPDIFF(HOUR, published_at, added_time) <= 24)) AND (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY views_increase_week DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByViewsIncreaseWeekDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE channel_id IN (:channelIds) AND (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY published_at DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByPublishedAtDescAndChannelIds(@Param("channelIds") List<String> channelIds);

    @Query(value = "SELECT * FROM vtuber_songs WHERE MATCH(title) AGAINST(:title IN BOOLEAN MODE) AND (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY published_at DESC", nativeQuery = true)
    List<VtuberSongsEntity> findAllByTitleContainingOrderByPublishedAtDesc(@Param("title") String title);

    @Query(value = "SELECT * FROM vtuber_songs WHERE vtuber_name = :vtuberName AND (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY published_at DESC", nativeQuery = true)
    List<VtuberSongsEntity> findAllByVtuberNameOrderByPublishedAtDesc(@Param("vtuberName") String vtuberName);

    @Query(value = "SELECT * FROM vtuber_songs WHERE (title REGEXP :keywords) AND (classification IS NULL OR (classification != 'shorts' AND classification != 'ignore')) ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<VtuberSongsEntity> findByKeywords(@Param("keywords") String keywords, @Param("limit") int limit);

    @Query("SELECT COUNT(v) FROM VtuberSongsEntity v WHERE v.channelId = :channelId")
    int countByChannelId(@Param("channelId") String channelId);

    List<VtuberSongsEntity> findByChannelId(String channelId);

    @Modifying
    @Query("DELETE FROM VtuberSongsEntity v WHERE v.channelId = :channelId")
    void deleteByChannelId(@Param("channelId") String channelId);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM VtuberSongsEntity v WHERE v.videoId = :videoId")
    void deleteByVideoId(@Param("videoId") String videoId);
}
