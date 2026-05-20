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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ManualVtuberService {

    private static final Logger logger = LoggerFactory.getLogger(ManualVtuberService.class);

    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final VtuberValidationService validationService;
    private final YouTube youTube;
    private final YouTubeApiService youTubeApiService;

    public ManualVtuberService(VtuberRepository vtuberRepository,
                               ExceptVtuberRepository exceptVtuberRepository,
                               VtuberValidationService validationService,
                               YouTube youTube,
                               YouTubeApiService youTubeApiService) {
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.validationService = validationService;
        this.youTube = youTube;
        this.youTubeApiService = youTubeApiService;
    }

    @SuppressWarnings("PMD.LooseCoupling")
    public String addVtuberChannel(String input) {
        if (logger.isInfoEnabled()) {
            logger.info("수동 버튜버 채널 추가 요청: {}", input);
        }

        Channel youtubeChannel;
        try {
            youtubeChannel = fetchChannelDetails(input);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("YouTube API 호출 중 오류 발생 (입력값: {}): {}", input, e.getMessage());
            }
            return "YouTube API 호출 중 오류가 발생했습니다: " + e.getMessage();
        }

        if (youtubeChannel == null) {
            return "채널 정보를 찾을 수 없습니다: " + input;
        }

        String actualChannelId = youtubeChannel.getId();

        if (vtuberRepository.existsByChannelId(actualChannelId)) {
            return "이미 존재하는 버튜버 채널입니다: " + actualChannelId;
        }
        if (exceptVtuberRepository.existsById(actualChannelId)) {
            return "제외 목록에 있는 채널입니다: " + actualChannelId;
        }

        String channelTitle = youtubeChannel.getSnippet().getTitle();

        try {
            saveNewVtuber(youtubeChannel);
            if (logger.isInfoEnabled()) {
                logger.info("수동으로 버튜버 채널 저장 완료: {} ({})", channelTitle, actualChannelId);
            }
            return "버튜버 채널이 성공적으로 추가되었습니다: " + channelTitle;
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("버튜버 채널 저장 중 오류 발생 (채널 ID: {}): {}", actualChannelId, e.getMessage());
            }
            return "버튜버 채널 저장 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private Channel fetchChannelDetails(String input) throws IOException {
        YouTube.Channels.List channelRequest = youTube.channels().list(List.of("snippet", "statistics"));
        
        String target = input.trim();
        if (target.contains("@")) {
            String handle = target.substring(target.indexOf("@"));
            handle = handle.split("[/?#]")[0];
            channelRequest.setForHandle(handle);
        } else if (target.startsWith("UC")) {
            channelRequest.setId(List.of(target));
        } else {
            channelRequest.setForHandle("@" + target);
        }

        channelRequest.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");

        ChannelListResponse channelResponse = youTubeApiService.executeRequest(channelRequest);
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
}
