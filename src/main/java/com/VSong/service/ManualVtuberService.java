package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.repository.ExceptVtuberRepository;
import com.VSong.repository.VtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ManualVtuberService {

    private static final Logger logger = LoggerFactory.getLogger(ManualVtuberService.class);

    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberValidationService validationService;
    private final YouTube youTube;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;

    public ManualVtuberService(VtuberRepository vtuberRepository,
                               ExceptVtuberRepository exceptVtuberRepository,
                               VtuberValidationService validationService,
                               YouTube youTube,
                               @Value("${youtube.api.keys}") String apiKeys) {
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.validationService = validationService;
        this.youTube = youTube;
        this.apiKeys = Arrays.asList(apiKeys.split(","));
    }

    public String addVtuberChannel(String channelId) {
        logger.info("수동 버튜버 채널 추가 요청: {}", channelId);

        // 1. 이미 DB에 존재하는지 확인
        if (vtuberRepository.existsByChannelId(channelId)) {
            return "이미 존재하는 버튜버 채널입니다: " + channelId;
        }
        // 2. 제외 목록에 있는지 확인
        if (exceptVtuberRepository.existsById(channelId)) {
            return "제외 목록에 있는 채널입니다: " + channelId;
        }

        Channel youtubeChannel = null;
        try {
            youtubeChannel = fetchChannelDetails(channelId);
        } catch (IOException e) {
            logger.error("YouTube API 호출 중 오류 발생 (채널 ID: {}): {}", channelId, e.getMessage());
            return "YouTube API 호출 중 오류가 발생했습니다: " + e.getMessage();
        }

        if (youtubeChannel == null) {
            return "채널 정보를 찾을 수 없습니다: " + channelId;
        }

        String channelTitle = youtubeChannel.getSnippet().getTitle();

        // 3. VtuberValidationService를 통한 유효성 검사




        // 4. DB에 저장
        try {
            saveNewVtuber(youtubeChannel);
            logger.info("수동으로 버튜버 채널 저장 완료: {} ({})", channelTitle, channelId);
            return "버튜버 채널이 성공적으로 추가되었습니다: " + channelTitle;
        } catch (Exception e) {
            logger.error("버튜버 채널 저장 중 오류 발생 (채널 ID: {}): {}", channelId, e.getMessage());
            return "버튜버 채널 저장 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private Channel fetchChannelDetails(String channelId) throws IOException {
        YouTube.Channels.List channelRequest = youTube.channels().list(List.of("snippet", "statistics"));
        channelRequest.setId(List.of(channelId));
        channelRequest.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");
        channelRequest.setKey(getCurrentApiKey());

        ChannelListResponse channelResponse = channelRequest.execute();
        List<Channel> channels = channelResponse.getItems();

        if (channels == null || channels.isEmpty()) {
            return null;
        }
        return channels.get(0);
    }

    private void saveNewVtuber(Channel channel) {
        VtuberEntity vtuber = new VtuberEntity();
        vtuber.setChannelId(channel.getId());
        vtuber.setName(channel.getSnippet().getTitle());
        String description = channel.getSnippet().getDescription();
        if (description != null && description.length() > 255) {
            description = description.substring(0, 255);
        }
        vtuber.setDescription(description);
        vtuber.setSubscribers(channel.getStatistics().getSubscriberCount());
        vtuber.setAddedTime(LocalDateTime.now());
        if (channel.getSnippet().getThumbnails() != null && channel.getSnippet().getThumbnails().getDefault() != null) {
            vtuber.setChannelImg(channel.getSnippet().getThumbnails().getDefault().getUrl());
        }
        vtuber.setStatus("new");
        vtuberRepository.save(vtuber);
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    private synchronized void rotateApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.warn("API 키를 다음 키로 전환했습니다.");
    }
}
