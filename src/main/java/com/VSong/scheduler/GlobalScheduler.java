package com.VSong.scheduler;

import com.VSong.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

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

    public GlobalScheduler(FirstUploadService firstUploadService,
                           UpdateVtuberSongsService updateVtuberSongsService,
                           UploadVtuberService uploadVtuberService,
                           UpdateVtuberService updateVtuberService,
                           ApiChannelIdService apiChannelIdService,
                           ThreadPoolExecutor vtuberSyncExecutor) {
        this.firstUploadService = firstUploadService;
        this.updateVtuberSongsService = updateVtuberSongsService;
        this.uploadVtuberService = uploadVtuberService;
        this.updateVtuberService = updateVtuberService;
        this.apiChannelIdService = apiChannelIdService;
        this.vtuberSyncExecutor = vtuberSyncExecutor;
    }

    // FirstUploadService - 최초 1회만 실행 후 off
    /** @Scheduled(cron = "0 24 16 * * ?", zone = "Asia/Seoul")
    public void scheduleDailyFirstUpload() {
        logger.info("Executing scheduled task: dailyFirstUpload");
        firstUploadService.dailyFirstUpload();
    }
    **/

    // UpdateVtuberSongsService - 최신 노래 수집
    @Scheduled(cron = "0 5 0 * * ?", zone = "Asia/Seoul")
    public void scheduleFetchVtuberSongs() {
        logger.info("Executing scheduled task: fetchVtuberSongs");
        updateVtuberSongsService.fetchVtuberSongs();
    }

    // UpdateVtuberSongsService - 최신 노래를 existing으로 상태 변경
    @Scheduled(cron = "30 2 0 * * MON", zone = "Asia/Seoul")
    public void scheduleUpdateSongStatusToExisting() {
        logger.info("Executing scheduled task: updateSongStatusToExisting");
        updateVtuberSongsService.updateSongStatusToExisting();
    }

    // UpdateVtuberSongsService - 노래 조회수 관리
    @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Seoul")
    public void scheduleUpdateViewCounts() {
        logger.info("Executing scheduled task: updateViewCounts");
        updateVtuberSongsService.updateViewCounts();
    }

    // UploadVtuberService - 버튜버 신규 채널 업로드
    @Scheduled(cron = "0 10 0 * * ?", zone = "Asia/Seoul")
    public void scheduleFetchAndSaveVtuberChannels() {
        logger.info("Executing scheduled task: fetchAndSaveVtuberChannels");
        uploadVtuberService.fetchAndSaveVtuberChannels();
    }

    // UpdateVtuberService - DB의 버튜버 프로필 업데이트
    @Scheduled(cron = "0 30 0 * * ?", zone = "Asia/Seoul")
    public void scheduleSyncVtuberData() {
        logger.info("Executing scheduled task: syncVtuberData");
        List<String> apiChannelIds = apiChannelIdService.getApiChannelIds();
        updateVtuberService.syncVtuberData(vtuberSyncExecutor, apiChannelIds);
    }
}
