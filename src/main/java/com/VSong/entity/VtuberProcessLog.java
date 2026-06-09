package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vtuber_process_logs", indexes = {
    @Index(name = "idx_vtuber_process_channel_id", columnList = "channelId"),
    @Index(name = "idx_vtuber_process_decision", columnList = "decision")
})
public class VtuberProcessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channelId;

    private String channelTitle;

    @Column(nullable = false)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public VtuberProcessLog() {
        this.processedAt = LocalDateTime.now();
    }

    public VtuberProcessLog(String channelId, String channelTitle, String decision, String reason) {
        this.channelId = channelId;
        this.channelTitle = channelTitle;
        this.decision = decision;
        this.reason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
