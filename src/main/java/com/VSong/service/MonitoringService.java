package com.VSong.service;

import com.VSong.entity.ServerStatusLog;
import com.VSong.repository.DailyVisitorRepository;
import com.VSong.repository.ServerStatusLogRepository;
import com.VSong.repository.SongUpdateLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    
    private final HealthEndpoint healthEndpoint;
    private final ServerStatusLogRepository statusLogRepository;
    private final SongUpdateLogRepository songUpdateLogRepository;
    private final DailyVisitorRepository dailyVisitorRepository;
    private final com.VSong.repository.ApiQuotaLogRepository apiQuotaLogRepository;

    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:local}")
    private String activeProfile;

    public MonitoringService(HealthEndpoint healthEndpoint,
                             ServerStatusLogRepository statusLogRepository,
                             SongUpdateLogRepository songUpdateLogRepository,
                             DailyVisitorRepository dailyVisitorRepository,
                             com.VSong.repository.ApiQuotaLogRepository apiQuotaLogRepository) {
        this.healthEndpoint = healthEndpoint;
        this.statusLogRepository = statusLogRepository;
        this.songUpdateLogRepository = songUpdateLogRepository;
        this.dailyVisitorRepository = dailyVisitorRepository;
        this.apiQuotaLogRepository = apiQuotaLogRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void monitorServerStatus() {
        Status currentHealth = healthEndpoint.health().getStatus();
        String currentStatus = currentHealth.getCode();

        statusLogRepository.findTopByEnvironmentOrderByCheckTimeDesc(activeProfile).ifPresentOrElse(
            lastLog -> {
                if (!lastLog.getStatus().equals(currentStatus)) {
                    saveStatus(currentStatus);
                    logger.info("[MONITOR] [{}] Server status changed from {} to {}", activeProfile, lastLog.getStatus(), currentStatus);
                }
            },
            () -> saveStatus(currentStatus)
        );
    }

    private void saveStatus(String status) {
        ServerStatusLog log = new ServerStatusLog();
        log.setCheckTime(LocalDateTime.now());
        log.setStatus(status);
        log.setEnvironment(activeProfile);
        statusLogRepository.save(log);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldMonitoringData() {
        LocalDateTime thresholdDateTime = LocalDateTime.now().minusDays(30);
        LocalDate thresholdDate = LocalDate.now().minusDays(30);

        logger.info("[CLEANUP] Starting 30-day data cleanup...");

        try {
            songUpdateLogRepository.deleteOldLogs(thresholdDateTime);
            logger.info("[CLEANUP] Deleted song update logs older than 30 days.");

            statusLogRepository.deleteOldLogs(thresholdDateTime);
            logger.info("[CLEANUP] Deleted server status logs older than 30 days.");

            dailyVisitorRepository.deleteOldVisitors(thresholdDate);
            logger.info("[CLEANUP] Deleted daily visitor logs older than 30 days.");

            apiQuotaLogRepository.deleteByRequestTimeBefore(LocalDateTime.now().minusDays(7));
            logger.info("[CLEANUP] Deleted API quota logs older than 7 days.");
            
            logger.info("[CLEANUP] Data cleanup completed successfully.");
        } catch (Exception e) {
            logger.error("[CLEANUP] Error during 30-day data cleanup: {}", e.getMessage());
        }
    }
}
