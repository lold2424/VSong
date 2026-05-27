package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vtuber_songs", indexes = {
    @Index(name = "idx_video_id", columnList = "videoId", unique = true),
    @Index(name = "idx_channel_id", columnList = "channelId"),
    @Index(name = "idx_published_at", columnList = "publishedAt"),
    @Index(name = "idx_views_increase_day", columnList = "viewsIncreaseDay"),
    @Index(name = "idx_views_increase_week", columnList = "viewsIncreaseWeek")
})
public class VtuberSongsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channelId;

    private String videoId;

    private String title;

    private String description;

    private LocalDateTime publishedAt;

    private LocalDateTime addedTime;

    private String vtuberName;

    @Column(length = 50)
    private String classification;

    private Long viewCount;

    private Long lastWeekViewCount;

    private Long viewsIncreaseDay;

    private Long viewsIncreaseWeek;

    private LocalDateTime updateDayTime;

    private LocalDateTime updateWeekTime;

    private String status;

    @Column(name = "parsed_title")
    private String parsedTitle;

    @Column(name = "song_type")
    private String songType; // "COVER", "ORIGINAL"

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getParsedTitle() {
        return parsedTitle;
    }

    public void setParsedTitle(String parsedTitle) {
        this.parsedTitle = parsedTitle;
    }

    public String getSongType() {
        return songType;
    }

    public void setSongType(String songType) {
        this.songType = songType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(LocalDateTime addedTime) {
        this.addedTime = addedTime;
    }

    public String getVtuberName() {
        return vtuberName;
    }

    public void setVtuberName(String vtuberName) {
        this.vtuberName = vtuberName;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getViewsIncreaseDay() {
        return viewsIncreaseDay;
    }

    public void setViewsIncreaseDay(Long viewsIncreaseDay) {
        this.viewsIncreaseDay = viewsIncreaseDay;
    }

    public Long getViewsIncreaseWeek() {
        return viewsIncreaseWeek;
    }

    public void setViewsIncreaseWeek(Long viewsIncreaseWeek) {
        this.viewsIncreaseWeek = viewsIncreaseWeek;
    }

    public LocalDateTime getUpdateDayTime() {
        return updateDayTime;
    }

    public void setUpdateDayTime(LocalDateTime updateDayTime) {
        this.updateDayTime = updateDayTime;
    }

    public LocalDateTime getUpdateWeekTime() {
        return updateWeekTime;
    }

    public void setUpdateWeekTime(LocalDateTime updateWeekTime) {
        this.updateWeekTime = updateWeekTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public Long getLastWeekViewCount() {
        return lastWeekViewCount;
    }

    public void setLastWeekViewCount(Long lastWeekViewCount) {
        this.lastWeekViewCount = lastWeekViewCount;
    }
}