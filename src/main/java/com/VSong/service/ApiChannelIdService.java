package com.VSong.service;

import com.VSong.repository.VtuberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiChannelIdService {

    private final VtuberRepository vtuberRepository;

    public ApiChannelIdService(VtuberRepository vtuberRepository) {
        this.vtuberRepository = vtuberRepository;
    }

    /**
     * 데이터베이스에 저장된 모든 Vtuber의 채널 ID 목록을 반환합니다.
     * @return 채널 ID 목록
     */
    public List<String> getApiChannelIds() {
        return vtuberRepository.findAllChannelIds();
    }
}
