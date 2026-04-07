package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.ExceptVtuberRepository;
import com.VSong.repository.VtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class UpdateVtuberService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberService vtuberService;
    private final YouTubeApiService youTubeApiService;
    private final com.VSong.repository.VtuberUpdateLogRepository vtuberUpdateLogRepository;
    private static final Logger logger = LoggerFactory.getLogger(UpdateVtuberService.class);

    public UpdateVtuberService(YouTube youTube,
                               VtuberRepository vtuberRepository,
                               ExceptVtuberRepository exceptVtuberRepository,
                               VtuberService vtuberService,
                               YouTubeApiService youTubeApiService,
                               com.VSong.repository.VtuberUpdateLogRepository vtuberUpdateLogRepository) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.vtuberService = vtuberService;
        this.youTubeApiService = youTubeApiService;
        this.vtuberUpdateLogRepository = vtuberUpdateLogRepository;
    }

    public void syncVtuberData(ThreadPoolExecutor executor) {
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
        java.util.concurrent.atomic.AtomicInteger updatedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        int deletedCount = 0;

        List<String> dbChannelIds = vtuberRepository.findAllChannelIds();
        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        dbChannelIds.removeAll(exceptChannelIds);

        if (logger.isInfoEnabled()) {
            logger.info("DB의 채널 {}개를 동기화합니다.", dbChannelIds.size());
        }

        Set<String> existingApiChannelIds = Collections.synchronizedSet(new HashSet<>());
        List<Future<?>> futures = new ArrayList<>();

        List<List<String>> partitions = partitionList(dbChannelIds, 50);
        for (List<String> partition : partitions) {
            futures.add(executor.submit(() -> {
                int updated = processSyncPartition(partition, existingApiChannelIds);
                updatedCount.addAndGet(updated);
                if (updated == 0 && !partition.isEmpty() && existingApiChannelIds.size() < partition.size()) {
                    failedCount.addAndGet(partition.size());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error waiting for partition processing to complete", e);
            }
        }

        Set<String> originalDbIds = new HashSet<>(dbChannelIds);
        originalDbIds.removeAll(existingApiChannelIds);

        if (!originalDbIds.isEmpty()) {
            deletedCount = originalDbIds.size();
            if (logger.isInfoEnabled()) {
                logger.info("API에서 확인되지 않아 삭제될 채널 수: {}", originalDbIds.size());
            }
            for (String channelIdToDelete : originalDbIds) {
                vtuberService.deleteVtuberAndRelatedSongs(channelIdToDelete);
                if (logger.isInfoEnabled()) {
                    logger.info("삭제된 채널 ID: {}", channelIdToDelete);
                }
            }
        } else {
            logger.info("삭제할 채널이 없습니다.");
        }

        long duration = java.time.Duration.between(startTime, java.time.LocalDateTime.now()).getSeconds();
        com.VSong.entity.VtuberUpdateLog log = new com.VSong.entity.VtuberUpdateLog();
        log.setRunTime(java.time.LocalDateTime.now());
        log.setDurationSeconds(duration);
        log.setNewVtubersCount(0);
        log.setUpdatedVtubersCount(updatedCount.get());
        log.setDeletedVtubersCount(deletedCount);
        log.setFailedVtubersCount(failedCount.get());
        log.setLogSummary("VTuber data synchronization run completed.");
        vtuberUpdateLogRepository.save(log);

        logger.info("=== syncVtuberData 종료 ===");
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private int processSyncPartition(List<String> channelIds, Set<String> existingApiChannelIds) {
        int updatedInPartition = 0;
        if (channelIds.isEmpty()) {
            return 0;
        }
        try {
            YouTube.Channels.List request = youTube.channels().list(List.of("snippet", "statistics"));
            request.setId(channelIds);
            request.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");

            ChannelListResponse response = youTubeApiService.executeRequest(request);
            List<Channel> channels = response.getItems();

            if (channels == null) {
                if (logger.isWarnEnabled()) {
                    logger.warn("API 응답이 null입니다. 파티션의 채널 {}개를 삭제 방지를 위해 유지합니다.", channelIds.size());
                }
                existingApiChannelIds.addAll(channelIds);
                return 0;
            }

            for (Channel channel : channels) {
                String channelId = channel.getId();
                existingApiChannelIds.add(channelId);

                VtuberEntity vtuber = vtuberRepository.findByChannelId(channelId)
                        .orElseGet(() -> {
                            if (logger.isWarnEnabled()) {
                                logger.warn("DB에 없는 채널 ID '{}'가 동기화 목록에 포함되어 있습니다. 새로 추가합니다.", channelId);
                            }
                            return new VtuberEntity();
                        });

                vtuber.setChannelId(channelId);
                vtuber.setName(channel.getSnippet().getTitle());
                vtuber.setDescription(truncateDescription(channel.getSnippet().getDescription()));
                vtuber.setSubscribers(channel.getStatistics().getSubscriberCount());
                if (channel.getSnippet().getThumbnails() != null && channel.getSnippet().getThumbnails().getDefault() != null) {
                    vtuber.setChannelImg(channel.getSnippet().getThumbnails().getDefault().getUrl());
                }
                vtuber.setStatus("existing");

                vtuberRepository.save(vtuber);
                updatedInPartition++;
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("채널 데이터 동기화 처리 중 오류 발생하여 해당 파티션의 채널 {}개를 삭제 방지를 위해 유지합니다. 오류: {}", channelIds.size(), e.getMessage());
            }
            existingApiChannelIds.addAll(channelIds);
        }
        return updatedInPartition;
    }

    private String truncateDescription(String description) {
        if (description != null && description.length() > 255) {
            return description.substring(0, 255);
        }
        return description;
    }

    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}