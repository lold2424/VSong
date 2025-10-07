package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.ExceptVtuberRepository;
import com.VSong.repository.VtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class UpdateVtuberService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberService vtuberService;
    private final RateLimiter rateLimiter = RateLimiter.create(5.0);
    private static final Logger logger = LoggerFactory.getLogger(UpdateVtuberService.class);

    private final List<String> apiKeys;
    private int currentKeyIndex = 0;

    public UpdateVtuberService(YouTube youTube,
                               VtuberRepository vtuberRepository,
                               ExceptVtuberRepository exceptVtuberRepository,
                               VtuberService vtuberService,
                               @Value("${youtube.api.keys}") String apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.vtuberService = vtuberService;
        this.apiKeys = Arrays.stream(apiKeys.split(",")).map(String::trim).collect(Collectors.toList());
    }

    public void syncVtuberData(ThreadPoolExecutor executor) {
        List<String> dbChannelIds = vtuberRepository.findAllChannelIds();
        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        dbChannelIds.removeAll(exceptChannelIds);

        logger.info("DB의 채널 {}개를 동기화합니다.", dbChannelIds.size());

        Set<String> existingApiChannelIds = Collections.synchronizedSet(new HashSet<>());
        List<Future<?>> futures = new ArrayList<>();

        List<List<String>> partitions = partitionList(dbChannelIds, 50);
        for (List<String> partition : partitions) {
            futures.add(executor.submit(() -> processSyncPartition(partition, existingApiChannelIds)));
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
            logger.info("API에서 확인되지 않아 삭제될 채널 수: {}", originalDbIds.size());
            for (String channelIdToDelete : originalDbIds) {
                vtuberService.deleteVtuberAndRelatedSongs(channelIdToDelete);
                logger.info("삭제된 채널 ID: {}", channelIdToDelete);
            }
        } else {
            logger.info("삭제할 채널이 없습니다.");
        }
        logger.info("=== syncVtuberData 종료 ===");
    }

    private void processSyncPartition(List<String> channelIds, Set<String> existingApiChannelIds) {
        if (channelIds.isEmpty()) {
            return;
        }
        try {
            rateLimiter.acquire();

            YouTube.Channels.List request = youTube.channels().list(List.of("snippet", "statistics"));
            request.setId(channelIds);
            request.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");
            request.setKey(getCurrentApiKey());

            ChannelListResponse response = request.execute();
            List<Channel> channels = response.getItems();

            if (channels == null) {
                logger.warn("API 응답이 null입니다. 파티션의 채널 {}개를 삭제 방지를 위해 유지합니다.", channelIds.size());
                existingApiChannelIds.addAll(channelIds);
                return;
            }

            for (Channel channel : channels) {
                String channelId = channel.getId();
                existingApiChannelIds.add(channelId);

                VtuberEntity vtuber = vtuberRepository.findByChannelId(channelId)
                        .orElseGet(() -> {
                            logger.warn("DB에 없는 채널 ID '{}'가 동기화 목록에 포함되어 있습니다. 새로 추가합니다.", channelId);
                            return new VtuberEntity();
                        });

                vtuber.setChannelId(channelId);
                vtuber.setName(channel.getSnippet().getTitle());
                vtuber.setDescription(truncateDescription(channel.getSnippet().getDescription()));
                vtuber.setSubscribers(channel.getStatistics().getSubscriberCount());
                if (channel.getSnippet().getThumbnails() != null && channel.getSnippet().getThumbnails().getDefault() != null) {
                    vtuber.setChannelImg(channel.getSnippet().getThumbnails().getDefault().getUrl());
                }
                vtuber.setStatus("updated");

                vtuberRepository.save(vtuber);
            }
        } catch (Exception e) {
            logger.error("채널 데이터 동기화 처리 중 오류 발생하여 해당 파티션의 채널 {}개를 삭제 방지를 위해 유지합니다. 오류: {}", channelIds.size(), e.getMessage());
            existingApiChannelIds.addAll(channelIds);
            rotateApiKey();
        }
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

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private synchronized void rotateApiKey() {
        if (apiKeys.size() <= 1) {
            logger.error("API 키가 하나뿐이라 교체할 수 없습니다. 할당량 문제를 확인하세요.");
            return;
        }
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.warn("API 키를 다음 키로 전환했습니다. (인덱스: {})", currentKeyIndex);
    }
}