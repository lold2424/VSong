package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "view_update_logs")
public class ViewUpdateLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime runTime;
    private Long durationSeconds;
    private Integer totalSongsCount;
    private Integer updatedCount;
    private Integer deletedCount;
    private Integer failedCount;
    private Integer usedQuota;

    public ViewUpdateLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getRunTime() { return runTime; }
    public void setRunTime(LocalDateTime runTime) { this.runTime = runTime; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getTotalSongsCount() { return totalSongsCount; }
    public void setTotalSongsCount(Integer totalSongsCount) { this.totalSongsCount = totalSongsCount; }

    public Integer getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(Integer updatedCount) { this.updatedCount = updatedCount; }

    public Integer getDeletedCount() { return deletedCount; }
    public void setDeletedCount(Integer deletedCount) { this.deletedCount = deletedCount; }

    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }

    public Integer getUsedQuota() { return usedQuota; }
    public void setUsedQuota(Integer usedQuota) { this.usedQuota = usedQuota; }
}
