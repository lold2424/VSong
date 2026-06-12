package com.VSong.service;

import com.VSong.entity.VtuberEntity;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.VtuberRepository;
import com.VSong.repository.VtuberSongsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.*;

@Service
public class VtuberService {
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private static final Logger logger = LoggerFactory.getLogger(VtuberService.class);

    public VtuberService(VtuberRepository vtuberRepository, VtuberSongsRepository vtuberSongsRepository) {
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
    }

    public VtuberEntity createVtuber(String description, String gender) {
        VtuberEntity vtuberEntity = new VtuberEntity();
        vtuberEntity.setDescription(org.apache.commons.text.StringEscapeUtils.escapeHtml4(description));
        vtuberEntity.setGender(org.apache.commons.text.StringEscapeUtils.escapeHtml4(gender));
        return vtuberRepository.save(vtuberEntity);
    }

    public Map<String, Object> searchVtubersAndSongs(String query, String channelId) {
        Map<String, Object> result = new HashMap<>();

        if (channelId != null && !channelId.isEmpty()) {
            Optional<VtuberEntity> vtuber = vtuberRepository.findByChannelId(channelId);
            if (vtuber.isPresent()) {
                List<VtuberEntity> vtubers = List.of(vtuber.get());
                result.put("vtubers", vtubers);

                List<VtuberSongsEntity> songs = vtuberSongsRepository.findAllByVtuberNameOrderByViewCountDesc(vtuber.get().getName());
                result.put("songs", songs);
            } else {
                result.put("vtubers", Collections.emptyList());
                result.put("songs", Collections.emptyList());
            }
        } else if (query != null && !query.isEmpty()) {
            List<VtuberSongsEntity> songs = vtuberSongsRepository.findAllByTitleContainingOrderByViewCountDesc(query);
            List<VtuberEntity> vtubers = vtuberRepository.findAllByNameContaining(query);
            result.put("songs", songs);
            result.put("vtubers", vtubers);
        } else {
            result.put("vtubers", Collections.emptyList());
            result.put("songs", Collections.emptyList());
        }

        return result;
    }

    public Map<String, Object> getVtuberDetails(String channelId) {
        Optional<VtuberEntity> vtuber = vtuberRepository.findByChannelId(channelId);
        if (vtuber.isEmpty()) {
            return Collections.emptyMap();
        }

        VtuberEntity vtuberEntity = vtuber.get();
        int songCount = vtuberSongsRepository.countByChannelId(channelId);

        Map<String, Object> details = new HashMap<>();
        details.put("name", vtuberEntity.getName());
        details.put("subscribers", vtuberEntity.getSubscribers());
        details.put("gender", vtuberEntity.getGender());
        details.put("songCount", songCount);
        details.put("channelImg", vtuberEntity.getChannelImg());

        return details;
    }

    public List<VtuberSongsEntity> getSongsByChannelId(String channelId) {
        return vtuberSongsRepository.findByChannelId(channelId);
    }

    public List<VtuberEntity> getAllVtubers() {
        return vtuberRepository.findAll();
    }

    @Transactional
    public void deleteVtuberAndRelatedSongs(String channelId) {
        vtuberSongsRepository.deleteByChannelId(channelId);
        logger.info("vtuber_songs 테이블에서 채널 ID {} 관련 데이터 삭제 완료", channelId);

        vtuberRepository.deleteByChannelId(channelId);
        logger.info("vtubers 테이블에서 채널 ID {} 삭제 완료", channelId);
    }
}
