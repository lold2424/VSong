package com.VSong.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.VSong.repository.ExceptVtuberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.VtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;

@Service
public class PlayboardVtuberService {

    private static final Logger logger = LoggerFactory.getLogger(PlayboardVtuberService.class);
    private final PlayboardSeleniumService seleniumService;
    private final ExceptVtuberRepository exceptVtuberRepository;

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;

    public PlayboardVtuberService(
            PlayboardSeleniumService seleniumService,
            YouTube youTube,
            VtuberRepository vtuberRepository,
            ExceptVtuberRepository exceptVtuberRepository,
            @Value("${youtube.api.keys}") List<String> apiKeys) {
        this.seleniumService = seleniumService;
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository; // 3. 의존성 주입
        this.apiKeys = apiKeys;
    }

    public void importAndEnrichVtubers() {
        logger.info("=== Playboard에서 VTuber 가져오기 시작 ===");

        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        logger.info("제외할 채널 ID 수: {}", exceptChannelIds.size());

        List<String> scrapedChannelIds = seleniumService.scrapeAllVtuberChannelIds();
        logger.info("스크래핑된 채널 ID 수: {}", scrapedChannelIds.size());

        List<String> channelIdsToProcess = scrapedChannelIds.stream()
                .filter(id -> !exceptChannelIds.contains(id))
                .collect(Collectors.toList());
        logger.info("제외 채널 필터링 후 처리할 채널 ID 수: {}", channelIdsToProcess.size());

        List<List<String>> partitions = partitionList(channelIdsToProcess, 50);

        for (List<String> partition : partitions) {
            try {
                YouTube.Channels.List request = youTube.channels().list("snippet,statistics");
                request.setId(String.join(",", partition));
                request.setKey(getCurrentApiKey());

                ChannelListResponse response = request.execute();

                for (Channel channel : response.getItems()) {
                    saveVtuberFromChannel(channel);
                }

                Thread.sleep(1000); // API 호출 제한

            } catch (Exception e) {
                logger.error("API 호출 오류: {}", e.getMessage());
            }
        }

        logger.info("=== Playboard 가져오기 완료 ===");
    }

    private void saveVtuberFromChannel(Channel channel) {
        String channelId = channel.getId();

        if (vtuberRepository.findByChannelId(channelId).isEmpty()) {
            VtuberEntity vtuber = new VtuberEntity();
            vtuber.setChannelId(channelId);
            vtuber.setName(channel.getSnippet().getTitle());
            vtuber.setDescription(truncateDescription(channel.getSnippet().getDescription()));
            vtuber.setSubscribers(channel.getStatistics().getSubscriberCount());
            vtuber.setChannelImg(channel.getSnippet().getThumbnails() != null
                    ? channel.getSnippet().getThumbnails().getDefault().getUrl()
                    : null);
            vtuber.setAddedTime(LocalDateTime.now());
            vtuber.setStatus("new");

            vtuberRepository.save(vtuber);
            logger.info("Playboard에서 가져온 VTuber 저장: {}", vtuber.getName());
        } else {
            logger.debug("이미 존재하는 채널: {}", channelId);
        }
    }

    private String getCurrentApiKey() {
        String key = apiKeys.get(currentKeyIndex);
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        return key;
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

    @Scheduled(cron = "0 50 23 * * ?", zone = "Asia/Seoul")
    public void scheduledImportFromPlayboard() {
        logger.info("=== 스케줄된 Playboard VTuber 가져오기 시작 ===");
        try {
            importAndEnrichVtubers();
            logger.info("스케줄된 Playboard 가져오기 성공");
        } catch (Exception e) {
            logger.error("스케줄된 Playboard 가져오기 실패: {}", e.getMessage());
        }
    }
}