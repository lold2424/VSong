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
    private final MainPageService mainPageService; // Added MainPageService

    public GlobalScheduler(FirstUploadService firstUploadService,
                           UpdateVtuberSongsService updateVtuberSongsService,
                           UploadVtuberService uploadVtuberService,
                           UpdateVtuberService updateVtuberService,
                           ApiChannelIdService apiChannelIdService,
                           ThreadPoolExecutor vtuberSyncExecutor,
                           RelatedChannelService relatedChannelService,
                           MainPageService mainPageService) { // Added MainPageService to constructor
        this.firstUploadService = firstUploadService;
        this.updateVtuberSongsService = updateVtuberSongsService;
        this.uploadVtuberService = uploadVtuberService;
        this.updateVtuberService = updateVtuberService;
        this.apiChannelIdService = apiChannelIdService;
        this.vtuberSyncExecutor = vtuberSyncExecutor;
        this.relatedChannelService = relatedChannelService;
        this.mainPageService = mainPageService;
    }

    // FirstUploadService - 최초 1회만 실행 후 off
    /** @Scheduled(cron = "0 52 14 * * ?", zone = "Asia/Seoul")
    public void scheduleDailyFirstUpload() {
        logger.info("Executing scheduled task: dailyFirstUpload");
        firstUploadService.dailyFirstUpload();
    } **/

    // RelatedChannelService - 관련 채널 기반 버튜버 탐색
    @Scheduled(cron = "0 45 23 * * ?", zone = "Asia/Seoul")
    public void scheduleDiscoverAndSaveFromRelatedChannels() {
        logger.info("Executing scheduled task: discoverAndSaveFromRelatedChannels");
        relatedChannelService.discoverAndSaveFromRelatedChannels();
    }

    // UpdateVtuberSongsService - 최신 노래 수집
    @Scheduled(cron = "0 55 23 * * ?", zone = "Asia/Seoul")
    public void scheduleFetchVtuberSongs() {
        logger.info("Executing scheduled task: fetchVtuberSongs");
        updateVtuberSongsService.fetchVtuberSongs();
    }

    // UpdateVtuberSongsService - 최신 노래를 existing으로 상태 변경
    @Scheduled(cron = "0 4 0 * * MON", zone = "Asia/Seoul")
    public void scheduleUpdateSongStatusToExisting() {
        logger.info("Executing scheduled task: updateSongStatusToExisting");
        updateVtuberSongsService.updateSongStatusToExisting();
    }

    // UpdateVtuberSongsService - 노래 조회수 관리
    @CacheEvict(value = "mainPage", allEntries = true)
    @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Seoul")
    public void scheduleUpdateViewCounts() {
        logger.info("Executing scheduled task: updateViewCounts");
        updateVtuberSongsService.updateViewCounts();
        // Re-populate the cache for "all" gender after eviction
        logger.info("Re-populating main page cache for 'all' gender after eviction.");
        mainPageService.getCacheableMainPageData("all");
    }

    // UploadVtuberService - 버튜버 신규 채널 업로드
    @Scheduled(cron = "0 15 0 * * ?", zone = "Asia/Seoul")
    public void scheduleFetchAndSaveVtuberChannels() {
        logger.info("Executing scheduled task: fetchAndSaveVtuberChannels");
        uploadVtuberService.fetchAndSaveVtuberChannels();
    }

    // UpdateVtuberService - DB의 버튜버 프로필 업데이트
    @Scheduled(cron = "0 30 0 * * SUN", zone = "Asia/Seoul")
    public void scheduleSyncVtuberData() {
        logger.info("Executing scheduled task: syncVtuberData");
        updateVtuberService.syncVtuberData(vtuberSyncExecutor);
    }
}
