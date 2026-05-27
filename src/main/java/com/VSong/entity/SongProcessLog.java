package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "song_process_logs", indexes = {
    @Index(name = "idx_video_id", columnList = "videoId"),
    @Index(name = "idx_processed_at", columnList = "processedAt")
})
public class SongProcessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String videoId;
    private String title;
    private String vtuberName;
    private String decision;
    
    @Column(length = 1000)
    private String reason;
    
    private LocalDateTime processedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVtuberName() { return vtuberName; }
    public void setVtuberName(String vtuberName) { this.vtuberName = vtuberName; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
