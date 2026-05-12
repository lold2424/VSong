package com.VSong.scheduler;

import com.VSong.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.cache.annotation.CacheEvict;

@Component
@EnableScheduling
public class GlobalScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalScheduler.class);

    private final FirstUploadService firstUploadService;
    private final UpdateVtuberSongsService updateVtuberSongsService;
    private final UploadVtuberService uploadVtuberService;
    private final UpdateVtuberService updateVtuberService;
    private final ApiChannelIdService apiChannelIdService;
    private final ThreadPoolExecutor vtuberSyncExecutor;
    private final RelatedChannelService relatedChannelService;
    private final MainPageService mainPageService;

    public GlobalScheduler(FirstUploadService firstUploadService,
                           UpdateVtuberSongsService updateVtuberSongsService,
                           UploadVtuberService uploadVtuberService,
                           UpdateVtuberService updateVtuberService,
                           ApiChannelIdService apiChannelIdService,
                           ThreadPoolExecutor vtuberSyncExecutor,
                           RelatedChannelService relatedChannelService,
                           MainPageService mainPageService) {
        this.firstUploadService = firstUploadService;
        this.updateVtuberSongsService = updateVtuberSongsService;
        this.uploadVtuberService = uploadVtuberService;
        this.updateVtuberService = updateVtuberService;
        this.apiChannelIdService = apiChannelIdService;
        this.vtuberSyncExecutor = vtuberSyncExecutor;
        this.relatedChannelService = relatedChannelService;
        this.mainPageService = mainPageService;
    }

    @Scheduled(cron = "0 39 23 * * ?", zone = "Asia/Seoul")
    public void scheduleFetchVtuberSongs() {
        logger.info("Executing scheduled task: fetchVtuberSongs");
        updateVtuberSongsService.fetchVtuberSongs();
    }

    @Scheduled(cron = "0 4 0 * * MON", zone = "Asia/Seoul")
    public void scheduleUpdateSongStatusToExisting() {
        logger.info("Executing scheduled task: updateSongStatusToExisting");
        updateVtuberSongsService.updateSongStatusToExisting();
    }

    @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Seoul")
    public void scheduleUpdateViewCounts() {
        logger.info("Executing scheduled task: updateViewCounts");
        updateVtuberSongsService.updateViewCounts();
    }

    @Scheduled(cron = "0 15 0 * * ?", zone = "Asia/Seoul")
    public void scheduleFetchAndSaveVtuberChannels() {
        logger.info("Executing scheduled task: fetchAndSaveVtuberChannels");
        uploadVtuberService.fetchAndSaveVtuberChannels();
    }

    @Scheduled(cron = "0 30 0 * * SUN", zone = "Asia/Seoul")
    public void scheduleSyncVtuberData() {
        logger.info("Executing scheduled task: syncVtuberData");
        updateVtuberService.syncVtuberData(vtuberSyncExecutor);
    }
}
