package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vtuber_update_logs")
public class VtuberUpdateLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime runTime;
    private Long durationSeconds;
    private Integer newVtubersCount;
    private Integer updatedVtubersCount;
    private Integer deletedVtubersCount;
    private Integer failedVtubersCount;

    @Column(columnDefinition = "TEXT")
    private String logSummary;

    @Column(columnDefinition = "TEXT")
    private String logDetailsJson;

    public VtuberUpdateLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getRunTime() { return runTime; }
    public void setRunTime(LocalDateTime runTime) { this.runTime = runTime; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getNewVtubersCount() { return newVtubersCount; }
    public void setNewVtubersCount(Integer newVtubersCount) { this.newVtubersCount = newVtubersCount; }

    public Integer getUpdatedVtubersCount() { return updatedVtubersCount; }
    public void setUpdatedVtubersCount(Integer updatedVtubersCount) { this.updatedVtubersCount = updatedVtubersCount; }

    public Integer getDeletedVtubersCount() { return deletedVtubersCount; }
    public void setDeletedVtubersCount(Integer deletedVtubersCount) { this.deletedVtubersCount = deletedVtubersCount; }

    public Integer getFailedVtubersCount() { return failedVtubersCount; }
    public void setFailedVtubersCount(Integer failedVtubersCount) { this.failedVtubersCount = failedVtubersCount; }

    public String getLogSummary() { return logSummary; }
    public void setLogSummary(String logSummary) { this.logSummary = logSummary; }

    public String getLogDetailsJson() { return logDetailsJson; }
    public void setLogDetailsJson(String logDetailsJson) { this.logDetailsJson = logDetailsJson; }
}
