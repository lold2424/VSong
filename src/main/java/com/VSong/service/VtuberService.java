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
    private final com.VSong.repository.ExceptVtuberRepository exceptVtuberRepository;
    private final com.VSong.repository.VtuberProcessLogRepository vtuberProcessLogRepository;
    private static final Logger logger = LoggerFactory.getLogger(VtuberService.class);

    public VtuberService(VtuberRepository vtuberRepository, 
                         VtuberSongsRepository vtuberSongsRepository,
                         com.VSong.repository.ExceptVtuberRepository exceptVtuberRepository,
                         com.VSong.repository.VtuberProcessLogRepository vtuberProcessLogRepository) {
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.vtuberProcessLogRepository = vtuberProcessLogRepository;
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

                List<VtuberSongsEntity> songs = vtuberSongsRepository.findAllByVtuberNameOrderByPublishedAtDesc(vtuber.get().getName());
                result.put("songs", songs);
            } else {
                result.put("vtubers", Collections.emptyList());
                result.put("songs", Collections.emptyList());
            }
        } else if (query != null && !query.isEmpty()) {
            String trimmedQuery = query.trim();
            String ftQuery = "+" + trimmedQuery + "*";

            List<VtuberSongsEntity> songsByTitle = vtuberSongsRepository.findAllByTitleContainingOrderByPublishedAtDesc(ftQuery);

            List<VtuberEntity> vtubers = vtuberRepository.findAllByNameContaining(trimmedQuery);
            vtubers.sort(Comparator.comparing(VtuberEntity::getSubscribers, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());

            Set<VtuberSongsEntity> combinedSongs = new LinkedHashSet<>(songsByTitle);
            for (VtuberEntity vtuber : vtubers) {
                List<VtuberSongsEntity> songsByVtuber = vtuberSongsRepository.findByChannelId(vtuber.getChannelId());
                combinedSongs.addAll(songsByVtuber);
            }

            List<VtuberSongsEntity> sortedSongs = new ArrayList<>(combinedSongs);
            sortedSongs.sort(Comparator.comparing(VtuberSongsEntity::getPublishedAt).reversed());

            result.put("songs", sortedSongs);
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
    public void updateVtuberGender(String channelId, String gender) {
        Optional<VtuberEntity> vtuberOpt = vtuberRepository.findByChannelId(channelId);
        if (vtuberOpt.isPresent()) {
            VtuberEntity vtuber = vtuberOpt.get();
            vtuber.setGender(gender);
            vtuberRepository.save(vtuber);
            logger.info("채널 ID {}의 성별을 {}로 업데이트 완료", channelId, gender);
        } else {
            logger.warn("성별 업데이트 실패: 채널 ID {}를 찾을 수 없습니다.", channelId);
        }
    }

    @Transactional
    public void deleteVtuberAndRelatedSongs(String channelId) {
        Optional<VtuberEntity> vtuberOpt = vtuberRepository.findByChannelId(channelId);
        String vtuberName = vtuberOpt.isPresent() ? vtuberOpt.get().getName() : "Unknown";

        vtuberSongsRepository.deleteByChannelId(channelId);
        logger.info("vtuber_songs 테이블에서 채널 ID {} 관련 데이터 삭제 완료", channelId);

        vtuberRepository.deleteByChannelId(channelId);
        logger.info("vtubers 테이블에서 채널 ID {} 삭제 완료", channelId);

        vtuberProcessLogRepository.save(new com.VSong.entity.VtuberProcessLog(channelId, vtuberName, "DELETED", "관리자에 의해 삭제 및 제외 처리됨"));

        if (!exceptVtuberRepository.existsByChannelId(channelId)) {
            com.VSong.entity.ExceptVtuberEntity exceptVtuber = new com.VSong.entity.ExceptVtuberEntity();
            exceptVtuber.setChannelId(channelId);
            exceptVtuberRepository.save(exceptVtuber);
            logger.info("채널 ID {}를 제외 목록(except_vtubers)에 추가 완료", channelId);
        }
    }
}

