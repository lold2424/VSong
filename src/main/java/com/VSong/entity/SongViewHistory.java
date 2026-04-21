package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "song_view_history", indexes = {
    @Index(name = "idx_video_id_date", columnList = "videoId, recordDate")
})
public class SongViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String videoId;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private Long viewCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
}
