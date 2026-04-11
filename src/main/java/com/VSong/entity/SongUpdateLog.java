package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "song_update_logs")
public class SongUpdateLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime runTime;
    private Long durationSeconds;
    private Integer newSongsCount;
    private Integer excludedSongsCount;
    private Integer failedSongsCount;

    @Column(columnDefinition = "TEXT")
    private String errorSummaryJson;

    @Column(columnDefinition = "TEXT")
    private String slowestChannelsJson;

    @Column(columnDefinition = "TEXT")
    private String newSongsJson;

    @Column(columnDefinition = "TEXT")
    private String excludedSongsJson;

    @Column(columnDefinition = "TEXT")
    private String failedSongsJson;

    public SongUpdateLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getRunTime() { return runTime; }
    public void setRunTime(LocalDateTime runTime) { this.runTime = runTime; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getNewSongsCount() { return newSongsCount; }
    public void setNewSongsCount(Integer newSongsCount) { this.newSongsCount = newSongsCount; }

    public Integer getExcludedSongsCount() { return excludedSongsCount; }
    public void setExcludedSongsCount(Integer excludedSongsCount) { this.excludedSongsCount = excludedSongsCount; }

    public Integer getFailedSongsCount() { return failedSongsCount; }
    public void setFailedSongsCount(Integer failedSongsCount) { this.failedSongsCount = failedSongsCount; }

    public String getErrorSummaryJson() { return errorSummaryJson; }
    public void setErrorSummaryJson(String errorSummaryJson) { this.errorSummaryJson = errorSummaryJson; }

    public String getSlowestChannelsJson() { return slowestChannelsJson; }
    public void setSlowestChannelsJson(String slowestChannelsJson) { this.slowestChannelsJson = slowestChannelsJson; }

    public String getNewSongsJson() { return newSongsJson; }
    public void setNewSongsJson(String newSongsJson) { this.newSongsJson = newSongsJson; }

    public String getExcludedSongsJson() { return excludedSongsJson; }
    public void setExcludedSongsJson(String excludedSongsJson) { this.excludedSongsJson = excludedSongsJson; }

    public String getFailedSongsJson() { return failedSongsJson; }
    public void setFailedSongsJson(String failedSongsJson) { this.failedSongsJson = failedSongsJson; }
}
