package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.ExceptVtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class UpdateVtuberService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberService vtuberService;
    private final RateLimiter rateLimiter = RateLimiter.create(5.0);
    private static final Logger logger = LoggerFactory.getLogger(UpdateVtuberService.class);

    public UpdateVtuberService(YouTube youTube, VtuberRepository vtuberRepository, ExceptVtuberRepository exceptVtuberRepository, VtuberService vtuberService) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.vtuberService = vtuberService;
    }

    public void syncVtuberData(ThreadPoolExecutor executor, List<String> apiChannelIds) {
        Set<String> dbChannelIds = new HashSet<>(vtuberRepository.findAllChannelIds());
        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        logger.info("DB 채널 ID 개수: {}, API 채널 ID 개수: {}, 제외 채널 ID 개수: {}", dbChannelIds.size(), apiChannelIds.size(), exceptChannelIds.size());

        apiChannelIds.removeAll(exceptChannelIds);

        List<List<String>> partitions = partitionList(apiChannelIds, 50);
        for (List<String> partition : partitions) {
            executor.submit(() -> processPartition(partition, dbChannelIds));
        }

        for (String channelId : dbChannelIds) {
            vtuberService.deleteVtuberAndRelatedSongs(channelId);
        }
    }

    private void processPartition(List<String> channelIds, Set<String> dbChannelIds) {
        try {
            rateLimiter.acquire();

            YouTube.Channels.List request = youTube.channels().list("snippet,statistics");
            request.setId(String.join(",", channelIds));
            request.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");
            request.setKey("YOUR_API_KEY");

            ChannelListResponse response = request.execute();
            List<Channel> channels = response.getItems();

            if (channels == null || channels.isEmpty()) {
                logger.warn("API 응답이 비어 있습니다.");
                return;
            }

            for (Channel channel : channels) {
                String channelId = channel.getId();
                dbChannelIds.remove(channelId);

                VtuberEntity vtuber = vtuberRepository.findByChannelId(channelId).orElse(new VtuberEntity());
                boolean isNew = vtuber.getId() == null;

                vtuber.setChannelId(channelId);
                vtuber.setName(channel.getSnippet().getTitle());
                vtuber.setDescription(truncateDescription(channel.getSnippet().getDescription()));
                vtuber.setSubscribers(channel.getStatistics().getSubscriberCount());
                vtuber.setChannelImg(channel.getSnippet().getThumbnails() != null
                        ? channel.getSnippet().getThumbnails().getDefault().getUrl()
                        : null);
                vtuber.setAddedTime(LocalDateTime.now());
                vtuber.setStatus(isNew ? "new" : "updated");

                vtuberRepository.save(vtuber);
                logger.info("VTuber {} 처리 완료 - 상태: {}", vtuber.getName(), isNew ? "새로 추가됨" : "업데이트됨");
            }
        } catch (Exception e) {
            logger.error("채널 데이터 처리 중 오류 발생: {}", e.getMessage());
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
}
